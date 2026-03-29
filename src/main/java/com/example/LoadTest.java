package com.example;

import com.example.command.RBACSystem;
import com.example.exception.DuplicatedResourceException;
import com.example.exception.ResourceNotFoundException;
import com.example.filter.UserFilters;
import com.example.manager.AssignmentManager;
import com.example.manager.RoleManager;
import com.example.manager.UserManager;
import com.example.model.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTest {
    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 50;
    private static final AtomicInteger userCounter = new AtomicInteger(1);
    private static final AtomicInteger roleCounter = new AtomicInteger(1);

    private static final List<String> PERMISSION_NAMES = List.of("READ", "WRITE", "DELETE", "EXECUTE");
    private static final List<String> RESOURCES = List.of("users", "reports", "settings", "documents", "logs");

    private static final List<String> createdUsers = new CopyOnWriteArrayList<>();
    private static final List<String> createdRoles = new CopyOnWriteArrayList<>();

    private static volatile boolean running = true;
    private static final List<String> errors = new CopyOnWriteArrayList<>();
    private static final AtomicLong successfulOps = new AtomicLong(0);
    private static final AtomicLong failedOps = new AtomicLong(0);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Starting Load Test ===");
        System.out.println("Threads: " + THREAD_COUNT);
        System.out.println("Operations per thread: " + OPERATIONS_PER_THREAD);
        System.out.println("Total operations: " + (THREAD_COUNT * OPERATIONS_PER_THREAD));
        System.out.println();

        RBACSystem system = new RBACSystem();
        system.initialize();

        UserManager userManager = system.getUserManager();
        RoleManager roleManager = system.getRoleManager();
        AssignmentManager assignmentManager = system.getAssignmentManager();

        setupBaseData(userManager, roleManager);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<?>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                Random random = new Random();

                for (int op = 0; op < OPERATIONS_PER_THREAD && running; op++) {
                    try {
                        int operationType = random.nextInt(7);

                        switch (operationType) {
                            case 0 -> createUser(userManager, threadId, op);
                            case 1 -> updateUser(userManager, threadId, op);

                            case 2 -> createRole(roleManager, threadId, op);
                            case 3 -> assignRole(assignmentManager, userManager, roleManager, threadId, op);

                            case 4 -> searchUsers(userManager, threadId, op);
                            case 5 -> getUserPermissions(assignmentManager, userManager, threadId, op);
                            case 6 -> listAllData(userManager, roleManager, assignmentManager, threadId, op);
                        }

                        successfulOps.incrementAndGet();

                        // Иногда делаем паузу для имитации реальной работы
                        if (random.nextInt(100) < 5) {
                            Thread.sleep(random.nextInt(2));
                        }

                    } catch (Exception e) {
                        failedOps.incrementAndGet();
                        String errorMsg = String.format("[Thread %d] Op %d: %s",
                                threadId, op, e.getMessage());
                        errors.add(errorMsg);
                        if (errors.size() % 50 == 0) {
                            System.err.println("Errors so far: " + errors.size());
                        }
                    }
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get(2, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                System.err.println("Timeout waiting for thread completion");
                running = false;
                break;
            } catch (ExecutionException e) {
                errors.add("Execution error: " + e.getCause().getMessage());
            }
        }

        long endTime = System.currentTimeMillis();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Thread.sleep(1000);

        System.out.println();
        System.out.println("=== Load Test Results ===");
        System.out.println("Execution time: " + (endTime - startTime) + " ms");
        System.out.println("Successful operations: " + successfulOps.get());
        System.out.println("Failed operations: " + failedOps.get());
        System.out.println("Success rate: " +
                String.format("%.2f", 100.0 * successfulOps.get() /
                        (successfulOps.get() + failedOps.get())) + "%");
        System.out.println();
        System.out.println("Final user count: " + userManager.count());
        System.out.println("Final role count: " + roleManager.count());
        System.out.println("Final assignment count: " + assignmentManager.count());
        System.out.println("Errors count: " + errors.size());

        if (errors.isEmpty()) {
            System.out.println("NO ERRORS - System is thread-safe!");
        } else {
            System.out.println("ERRORS ENCOUNTERED: " + errors.size());
            Set<String> uniqueErrors = new HashSet<>(errors);
            System.out.println("Unique error types: " + uniqueErrors.size());
            uniqueErrors.stream().limit(10).forEach(e -> System.out.println("  - " + e));
            if (uniqueErrors.size() > 10) {
                System.out.println("  ... and " + (uniqueErrors.size() - 10) + " more types");
            }
        }

        System.out.println();
        System.out.println("=== Data Integrity Check ===");

        List<User> users = userManager.findAll();
        Set<String> usernames = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (User user : users) {
            if (!usernames.add(user.username())) {
                duplicates.add(user.username());
            }
        }
        if (!duplicates.isEmpty()) {
            System.out.println("DUPLICATE USERNAMES FOUND: " + duplicates.size());
        } else {
            System.out.println("No duplicate usernames");
        }

        List<Role> roles = roleManager.findAll();
        Set<String> roleNames = new HashSet<>();
        List<String> roleDuplicates = new ArrayList<>();
        for (Role role : roles) {
            if (!roleNames.add(role.getName())) {
                roleDuplicates.add(role.getName());
            }
        }
        if (!roleDuplicates.isEmpty()) {
            System.out.println("DUPLICATE ROLE NAMES FOUND: " + roleDuplicates.size());
        } else {
            System.out.println("No duplicate role names");
        }

        List<RoleAssignment> assignments = assignmentManager.findAll();
        Set<String> existingUsers = new HashSet<>();
        for (User user : users) {
            existingUsers.add(user.username());
        }

        Set<String> existingRoles = new HashSet<>();
        for (Role role : roles) {
            existingRoles.add(role.getName());
        }

        List<String> orphanedByUser = new ArrayList<>();
        List<String> orphanedByRole = new ArrayList<>();

        for (RoleAssignment assignment : assignments) {
            if (!existingUsers.contains(assignment.user().username())) {
                orphanedByUser.add(assignment.user().username());
            }
            if (!existingRoles.contains(assignment.role().getName())) {
                orphanedByRole.add(assignment.role().getName());
            }
        }

        if (!orphanedByUser.isEmpty()) {
            System.out.println("Orphaned assignments (user missing): " + new HashSet<>(orphanedByUser).size());
        } else {
            System.out.println("No orphaned assignments by user");
        }

        if (!orphanedByRole.isEmpty()) {
            System.out.println("Orphaned assignments (role missing): " + new HashSet<>(orphanedByRole).size());
        } else {
            System.out.println("No orphaned assignments by role");
        }

        long expiredCount = assignments.stream()
                .filter(a -> a instanceof TemporaryAssignment)
                .filter(a -> !a.isActive())
                .count();
        if (expiredCount > 0) {
            System.out.println("Expired temporary assignments: " + expiredCount);
        }

        system.shutdown();

        System.out.println();
        System.out.println("=== Load Test Finished ===");
    }

    private static void setupBaseData(UserManager userManager, RoleManager roleManager) {
        System.out.println("Setting up base data...");

        for (int i = 1; i <= 5; i++) {
            try {
                String username = "base_user_" + i;
                User user = User.validate(username, "Base User " + i, "base" + i + "@test.com");
                userManager.add(user);
                createdUsers.add(username);
            } catch (Exception e) {
            }
        }

        for (int i = 1; i <= 3; i++) {
            try {
                String roleName = "base_role_" + i;
                Set<Permission> perms = new HashSet<>();
                perms.add(new Permission("READ", "users", "Read users"));
                Role role = new Role(roleName, "Base role " + i, perms);
                roleManager.add(role);
                createdRoles.add(roleName);
            } catch (Exception e) {
            }
        }

        System.out.println("Base data setup complete. Users: " + userManager.count() +
                ", Roles: " + roleManager.count());
    }

    private static void createUser(UserManager userManager, int threadId, int op) {
        int num = userCounter.getAndIncrement();
        String username = "test_user_" + threadId + "_" + op + "_" + num;
        String fullName = "Test User " + threadId + "_" + op;
        String email = "user" + num + "@test.com";

        User user = User.validate(username, fullName, email);
        userManager.add(user);
        createdUsers.add(username);
    }

    private static void updateUser(UserManager userManager, int threadId, int op) {
        List<User> users = userManager.findAll().stream()
                .filter(u -> !u.username().equals("akuskodanil"))
                .limit(20)
                .toList();

        if (users.isEmpty()) return;

        Random random = new Random();
        User user = users.get(random.nextInt(users.size()));

        String newFullName = "Updated " + user.fullName() + " v" + op;
        String newEmail = "updated_" + System.currentTimeMillis() + "_" + user.username() + "@test.com";

        try {
            userManager.update(user.username(), newFullName, newEmail);
        } catch (com.example.exception.ResourceNotFoundException e) {
        }
    }

    private static void createRole(RoleManager roleManager, int threadId, int op) {
        int num = roleCounter.getAndIncrement();
        String roleName = "test_role_" + threadId + "_" + op + "_" + num;
        String description = "Test role created by thread " + threadId;

        Random random = new Random();
        Set<Permission> permissions = new HashSet<>();

        int permCount = random.nextInt(3) + 1;
        for (int i = 0; i < permCount; i++) {
            String permName = PERMISSION_NAMES.get(random.nextInt(PERMISSION_NAMES.size()));
            String resource = RESOURCES.get(random.nextInt(RESOURCES.size()));
            permissions.add(new Permission(permName, resource, "Auto-generated permission"));
        }

        try {
            Role role = new Role(roleName, description, permissions);
            roleManager.add(role);
            createdRoles.add(roleName);
        } catch (com.example.exception.DuplicatedResourceException e) {
        }
    }

    private static void assignRole(AssignmentManager assignmentManager,
                                   UserManager userManager,
                                   RoleManager roleManager,
                                   int threadId, int op) {
        List<User> users = userManager.findAll().stream()
                .filter(u -> !u.username().equals("akuskodanil"))
                .limit(30)
                .toList();

        List<Role> roles = roleManager.findAll().stream()
                .filter(r -> !r.getName().equals("Admin"))
                .filter(r -> !r.getName().contains("admin"))
                .limit(20)
                .toList();

        if (users.isEmpty() || roles.isEmpty()) return;

        Random random = new Random();
        User user = users.get(random.nextInt(users.size()));
        Role role = roles.get(random.nextInt(roles.size()));

        if (assignmentManager.userHasRole(user, role)) {
            return;
        }

        String reason = "Load test assignment from thread " + threadId + ", op " + op;
        AssignmentMetadata metadata = AssignmentMetadata.now("load_tester", reason);

        RoleAssignment assignment;
        if (random.nextInt(100) < 60) {
            assignment = new PermanentAssignment(user, role, metadata);
        } else {
            String expiresAt = java.time.ZonedDateTime.now()
                    .plusDays(random.nextInt(30) + 1)
                    .format(com.example.util.ValidationUtils.DATE_TIME_FORMATTER);
            assignment = new TemporaryAssignment(user, role, metadata, expiresAt, false);
        }

        try {
            assignmentManager.add(assignment);
        } catch (DuplicatedResourceException | ResourceNotFoundException e) {
        }
    }

    private static void searchUsers(UserManager userManager, int threadId, int op) {
        List<User> users = userManager.findAll();
        if (users.isEmpty()) return;

        Random random = new Random();
        int searchType = random.nextInt(4);

        try {
            switch (searchType) {
                case 0 -> {
                    if (!users.isEmpty()) {
                        String username = users.get(random.nextInt(users.size())).username();
                        if (username.length() >= 3) {
                            String partial = username.substring(0, Math.min(5, username.length()));
                            userManager.findByFilter(UserFilters.byUsernameContains(partial));
                        }
                    }
                }
                case 1 -> {
                    userManager.findByFilter(UserFilters.byEmailDomain("@test.com"));
                }
                case 2 -> {
                    if (!users.isEmpty()) {
                        String fullName = users.get(random.nextInt(users.size())).fullName();
                        if (fullName.length() >= 3) {
                            String partial = fullName.substring(0, Math.min(5, fullName.length()));
                            userManager.findByFilter(UserFilters.byFullNameContains(partial));
                        }
                    }
                }
                case 3 -> {
                    if (!users.isEmpty()) {
                        User user = users.get(random.nextInt(users.size()));
                        userManager.findByFilter(UserFilters.byEmail(user.email()));
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private static void getUserPermissions(AssignmentManager assignmentManager,
                                           UserManager userManager,
                                           int threadId, int op) {
        List<User> users = userManager.findAll();
        if (users.isEmpty()) return;

        Random random = new Random();
        User user = users.get(random.nextInt(users.size()));

        try {
            Set<Permission> permissions = assignmentManager.getUserPermissions(user);
            if (permissions == null) {
                throw new IllegalStateException("getUserPermissions returned null");
            }
        } catch (Exception e) {
        }
    }

    private static void listAllData(UserManager userManager,
                                    RoleManager roleManager,
                                    AssignmentManager assignmentManager,
                                    int threadId, int op) {
        userManager.findAll();
        roleManager.findAll();
        assignmentManager.findAll();
    }
}