package com.example.command;

import com.example.auditlog.AuditLog;
import com.example.manager.AssignmentManager;
import com.example.manager.RoleManager;
import com.example.manager.UserManager;
import com.example.model.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RBACSystem {
    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private String currentUser;

    private AuditLog logger;

    private ExecutorService executorService;

    private ScheduledExecutorService scheduledExecutorService;
    private volatile boolean scheduledTaskRunning = true;

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public AuditLog getLogger() {
        return logger;
    }

    public void setLogger(AuditLog logger) {
        this.logger = logger;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String username) {
        currentUser = username;
    }

    public void initialize() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager();

        logger = new AuditLog();

        executorService = Executors.newFixedThreadPool(4);

        assignmentManager.setUserManager(userManager);
        assignmentManager.setRoleManager(roleManager);
        roleManager.setAssignmentManager(assignmentManager);


        Permission readUsersPermission= new Permission("READ", "users", "Can read users");
        Permission writeUsersPermission = new Permission("WRITE", "users", "Can edit users");
        Permission deleteUsersPermission = new Permission("DELETE", "users", "Can delete users");

        Permission readReportsPermission= new Permission("READ", "reports", "Can read reports");
        Permission writeReportsPermission = new Permission("WRITE", "reports", "Can edit reports");
        Permission deleteReportsPermission = new Permission("DELETE", "reports", "Can delete reports");

        Permission readSettingsPermission= new Permission("READ", "settings", "Can read settings");
        Permission writeSettingsPermission = new Permission("WRITE", "settings", "Can edit settings");
        Permission deleteSettingsPermission = new Permission("DELETE", "settings", "Can delete settings");

        Role adminRole = new Role("Admin", "All permissions", new HashSet<>());
        Role managerRole = new Role("Manager", "Read- and write-only permissions", new HashSet<>());
        Role viewerRole = new Role("Viewer", "Read-only permissions", new HashSet<>());

        roleManager.add(adminRole);
        roleManager.add(managerRole);
        roleManager.add(viewerRole);

        Set<Permission> adminPermissions = Set.of(readUsersPermission, readReportsPermission, readSettingsPermission,
                writeUsersPermission, writeReportsPermission, writeSettingsPermission,
                deleteUsersPermission, deleteReportsPermission, deleteSettingsPermission);

        adminPermissions.forEach(permission -> roleManager.addPermissionToRole("Admin", permission));

        User admin = User.validate("akuskodanil", "Akusko Danil Dmitrievich", "akusko.dan@yandex.ru");
        userManager.add(admin);

        RoleAssignment adminRoleAssignment = new PermanentAssignment(admin,
                adminRole, AssignmentMetadata.now("System", "Administrator initialization"));

        assignmentManager.add(adminRoleAssignment);

        setCurrentUser(admin.username());

        startScheduledTasks();
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledTaskRunning = false;
            scheduledExecutorService.shutdown();
            try {
                if (!scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduledExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (logger != null) {
            logger.shutdown();
        }
    }

    public String generateStatistics() {
        int usersCount = userManager.count();
        int rolesCount = roleManager.count();
        int totalAssignmentsCount = assignmentManager.count();
        int activeAssignmentsCount = assignmentManager.getActiveAssignments().size();
        int expiredAssignmentsCount = assignmentManager.getExpiredAssignments().size();

        int averageRolesCountPerUser = 0;
        if (usersCount != 0) {
            averageRolesCountPerUser = rolesCount / usersCount;
        }

        Map<Role, Long> roleCount =  assignmentManager.findAll().stream()
                .collect(Collectors.groupingBy(
                        RoleAssignment::role,
                        Collectors.counting()
                ));

        String popularRoles = roleCount.entrySet().stream()
                .sorted(Map.Entry.<Role, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> String.format("%s: %d assignment(s)",
                        entry.getKey().getName(), entry.getValue()))
                .collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Count of users: %d\n", usersCount));
        sb.append(String.format("Count of roles: %d\n", rolesCount));
        sb.append(String.format("Total count of assignments: %d\n", totalAssignmentsCount));
        sb.append(String.format("Count of active assignments: %d\n", activeAssignmentsCount));
        sb.append(String.format("Count of expired assignments: %d\n", expiredAssignmentsCount));
        sb.append(String.format("Average count of roles per user: %d\n", averageRolesCountPerUser));
        sb.append(String.format("Top 3 most popular roles:\n%s", popularRoles.isEmpty() ? "missing" : popularRoles));

        return sb.toString();
    }

    private void startScheduledTasks() {
        scheduledExecutorService = Executors.newScheduledThreadPool(2);

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                expireTemporaryAssignments();
            } catch (Exception e) {
                System.err.println("[ScheduledTask] Error expiring assignments: " + e.getMessage());
            }
        }, 10, 30, TimeUnit.SECONDS);

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                logStatistics();
            } catch (Exception e) {
                System.err.println("[ScheduledTask] Error logging statistics: " + e.getMessage());
            }
        }, 15, 60, TimeUnit.SECONDS);
    }

    private void expireTemporaryAssignments() {
        List<RoleAssignment> allAssignments = assignmentManager.findAll();
        List<RoleAssignment> expiredTemporary = allAssignments.stream()
                .filter(assignment -> assignment instanceof TemporaryAssignment)
                .filter(assignment -> !assignment.isActive())
                .toList();

        if (!expiredTemporary.isEmpty()) {
            for (RoleAssignment assignment : expiredTemporary) {
                if (assignment instanceof TemporaryAssignment temp) {
                    if (logger != null) {
                        logger.log("expire-check", "scheduler",
                                assignment.user().username(),
                                "Temporary assignment expired for role: " + assignment.role().getName());
                    }
                }
            }
        }
    }

    private void logStatistics() {
        String stats = generateStatistics();
        if (logger != null) {
            String[] lines = stats.split("\n");
            for (String line : lines) {
                logger.log("statistics", "scheduler", "system", line);
            }
        }
    }
}