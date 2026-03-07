package com.example.command;

import com.example.filter.*;
import com.example.model.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CommandRegistry {
    public static void registerAllCommands(CommandParser parser) {
        registerUserCommands(parser);
        registerRoleCommands(parser);
        registerAssignmentCommands(parser);
        registerPermissionCommands(parser);
        registerServiceCommands(parser);
    }

    private static void registerUserCommands(CommandParser parser) {
        parser.registerCommand("user-list", "List of all users",
                (scanner, system) -> {
                    List<User> users = system.getUserManager().findAll();
                    if (users.isEmpty()) {
                        System.out.println("Missing users");
                        return;
                    }
                    System.out.println("-".repeat(170));
                    System.out.printf("| %-20s | %-70s | %-70s |\n", "Username", "Full name", "Email");
                    System.out.println("-".repeat(170));
                    users.forEach(user -> {
                        System.out.printf("| %-20s | %-70s | %-70s |\n",
                                user.username(), user.fullName(), user.email());
                        System.out.println("-".repeat(170));
                    });
                });

        parser.registerCommand("user-create", "Create new user",
                (scanner, system) -> {
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine().trim();

                    System.out.print("Enter fullName: ");
                    String fullName = scanner.nextLine().trim();

                    System.out.print("Enter email: ");
                    String email = scanner.nextLine().trim();

                    User newUser = null;
                    try {
                        newUser = User.validate(username, fullName, email);
                    } catch (Exception e) {
                        system.getLogger().log(
                                "user-create", system.getCurrentUser(), "user", "exception"
                        );
                        System.out.printf("Error creating user: %s\n", e.getMessage());
                    }

                    if (newUser != null) {
                        system.getUserManager().add(newUser);
                        system.getLogger().log(
                                "user-create", system.getCurrentUser(), "user", "success"
                        );
                        System.out.println("New user has been successfully created");
                    }
                });

        parser.registerCommand("user-view", "Get user information",
                (scanner, system) -> {
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine().trim();

                    Optional<User> user = system.getUserManager().findByUsername(username);
                    if (user.isEmpty()) {
                        System.out.printf("User with username '%s' not found\n", username);
                        return;
                    }
                    System.out.printf("User: %s\n", user.get().format());

                    System.out.println("Assigned roles and their permissions:");
                    List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user.get());
                    if (assignments.isEmpty()) {
                        System.out.println("missing");
                        return;
                    }
                    assignments.forEach(assignment -> System.out.println(assignment.role()));
                });

        parser.registerCommand("user-update", "Update user data",
                (scanner, system) -> {
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine().trim();

                    System.out.print("Enter new fullName: ");
                    String newFullName = scanner.nextLine().trim();

                    System.out.print("Enter new email: ");
                    String newEmail = scanner.nextLine().trim();

                    try {
                        system.getUserManager().update(username, newFullName, newEmail);
                    } catch (Exception e) {
                        System.out.printf("Error updating user data: %s\n", e.getMessage());
                        return;
                    }

                    System.out.println("User data has been successfully updated");
                });

        parser.registerCommand("user-delete", "Delete user",
                (scanner, system) -> {
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine().trim();

                    System.out.print("Confirm user deletion? (y/n): ");
                    String answer = scanner.nextLine().trim().toLowerCase();
                    if (!answer.equals("y")) {
                        system.getLogger().log(
                                "user-delete", system.getCurrentUser(), "user", "cancelling"
                        );
                        System.out.println("Canceling user deletion");
                        return;
                    }

                    Optional<User> user = system.getUserManager().findByUsername(username);
                    if (user.isEmpty()) {
                        system.getLogger().log(
                                "user-delete", system.getCurrentUser(), "user", "error"
                        );
                        System.out.printf("User with username '%s' not found\n", username);
                        return;
                    }

                    system.getAssignmentManager().findByUser(user.get()).forEach(
                            roleAssignment -> system.getAssignmentManager().remove(roleAssignment)
                    );

                    system.getUserManager().remove(user.get());
                    system.getLogger().log(
                            "user-delete", system.getCurrentUser(), "user", "success"
                    );
                    System.out.println("User and his assignments have been successfully deleted");
                });

        parser.registerCommand("user-search", "Search users by filters",
                (scanner, system) -> {
                    String[] options = {"by username (contains)",
                            "by email (contains)",
                            "by email domain",
                            "by full name (contains)"};

                    int i = 1;
                    for (String option : options) {
                        System.out.println("\t" + i + ". " + option);
                        ++i;
                    }

                    System.out.print("Select filter number: ");
                    int filterNumber = scanner.nextInt();
                    scanner.nextLine();

                    System.out.print("Enter filter key: ");
                    String filterKey = scanner.nextLine().trim();

                    UserFilter filter = null;
                    switch (filterNumber) {
                        case 1 -> filter = UserFilters.byUsernameContains(filterKey);
                        case 2 -> filter = UserFilters.byEmail(filterKey);
                        case 3 -> filter = UserFilters.byEmailDomain(filterKey);
                        case 4 -> filter = UserFilters.byFullNameContains(filterKey);
                        default -> {
                            System.out.println("Cancelling");
                            return;
                        }
                    }

                    List<User> filteredUsers = system.getUserManager().findByFilter(filter);
                    if (filteredUsers.isEmpty()) {
                        System.out.println("missing");
                        return;
                    }
                    System.out.println("Filtered users:");
                    filteredUsers.forEach(user -> System.out.println(user.format()));
                });
    }

    private static void registerRoleCommands(CommandParser parser) {
        parser.registerCommand("role-list", "List of all roles",
                (scanner, system) -> {
                    List<Role> roles = system.getRoleManager().findAll();
                    if (roles.isEmpty()) {
                        System.out.println("Missing roles");
                        return;
                    }
                    System.out.println("-".repeat(88));
                    System.out.printf("| %-41s | %-21s | %-16s |\n", "ID", "Name", "Permission count");
                    System.out.println("-".repeat(88));
                    roles.forEach(role -> {
                        System.out.printf("| %-41s | %-21s | %-16s |\n",
                                role.getId(), role.getName(), role.getPermissions().size());
                        System.out.println("-".repeat(88));
                    });
                });

        parser.registerCommand("role-create", "Create new role",
                (scanner, system) -> {
                    System.out.print("Enter role name: ");
                    String roleName = scanner.nextLine().trim();

                    if (roleName.toLowerCase().contains("admin")) {
                        system.getLogger().log(
                                "role-create", system.getCurrentUser(), "role", "error"
                        );
                        System.out.println("Error creating role: DON`T TOUCH ADMIN ROLE!");
                        return;
                    }

                    System.out.print("Enter role description: ");
                    String roleDescription = scanner.nextLine().trim();


                    Role newRole = null;
                    try {
                        newRole = new Role(roleName, roleDescription, new HashSet<>());
                    } catch (Exception e) {
                        system.getLogger().log(
                                "role-create", system.getCurrentUser(), "role", "exception"
                        );
                        System.out.printf("Error creating role: %s\n", e.getMessage());
                        return;
                    }

                    system.getRoleManager().add(newRole);
                    system.getLogger().log(
                            "role-create", system.getCurrentUser(), "role", "success"
                    );
                    System.out.println("New role has been successfully created");

                    while (true) {
                        System.out.print("Add new permission to role? (y/n): ");
                        String answer = scanner.nextLine().trim().toLowerCase();
                        if (!answer.equals("y")) {
                            system.getLogger().log(
                                    "permission add", system.getCurrentUser(), "permission", "cancelling"
                            );
                            System.out.println("Canceling permission addition");
                            return;
                        }

                        System.out.print("Enter permission name: ");
                        String name = scanner.nextLine().trim();

                        System.out.print("Enter permission resource: ");
                        String resource = scanner.nextLine().trim();

                        System.out.print("Enter permission description: ");
                        String description = scanner.nextLine().trim();

                        Permission permission = new Permission(name, resource, description);
                        system.getRoleManager().addPermissionToRole(roleName, permission);

                        system.getLogger().log(
                                "permission add", system.getCurrentUser(), "permission", "success"
                        );
                        System.out.println("Permission has been successfully added to role");
                    }

                });

        parser.registerCommand("role-view", "Get role information",
                (scanner, system) -> {
                    System.out.print("Enter role name: ");
                    String roleName = scanner.nextLine().trim();

                    Optional<Role> role = system.getRoleManager().findByName(roleName);
                    if (role.isEmpty()) {
                        System.out.printf("Role with name '%s' not found\n", roleName);
                        return;
                    }
                    System.out.println(role.get());
                });

        parser.registerCommand("role-update", "Update role data",
                (scanner, system) -> {
                    System.out.print("Enter role name: ");
                    String roleName = scanner.nextLine().trim();

                    System.out.print("Enter new role name: ");
                    String newRoleName = scanner.nextLine().trim();

                    if (roleName.toLowerCase().contains("admin") || newRoleName.toLowerCase().contains("admin")) {
                        System.out.println("Error updating role data: DON`T TOUCH ADMIN ROLE!");
                        return;
                    }

                    System.out.print("Enter new description: ");
                    String newDescription = scanner.nextLine().trim();

                    try {
                        system.getRoleManager().update(roleName, newRoleName, newDescription);
                    } catch (Exception e) {
                        System.out.printf("Error updating role data: %s\n", e.getMessage());
                        return;
                    }

                    System.out.println("Role data has been successfully updated");
                });

        parser.registerCommand("role-delete", "Delete role",
                (scanner, system) -> {
                    System.out.print("Enter role name: ");
                    String roleName = scanner.nextLine().trim();

                    Optional<Role> role = system.getRoleManager().findByName(roleName);
                    if (role.isEmpty()) {
                        system.getLogger().log(
                                "role-delete", system.getCurrentUser(), "role", "error"
                        );
                        System.out.printf("Role with name '%s' not found\n", roleName);
                        return;
                    }

                    System.out.print("Confirm role deletion? (y/n): ");
                    String answer = scanner.nextLine().trim().toLowerCase();
                    if (!answer.equals("y")) {
                        system.getLogger().log(
                                "role-delete", system.getCurrentUser(), "role", "cancelling"
                        );
                        System.out.println("Canceling role deletion");
                        return;
                    }

                    if (!system.getRoleManager().remove(role.get())) {
                        List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role.get());
                        List<User> users = new ArrayList<>();
                        assignments.forEach(assignment -> users.add(assignment.user()));
                        system.getLogger().log(
                                "role-delete", system.getCurrentUser(), "role", "error"
                        );
                        System.out.printf("Role with name '%s' is assigned to users:\n", roleName);
                        users.forEach(user -> System.out.println(user.format()));
                    } else {
                        system.getLogger().log(
                                "role-delete", system.getCurrentUser(), "role", "success"
                        );
                        System.out.println("Role have been successfully deleted");
                    }
                });

        parser.registerCommand("role-add-permission", "Add permission to role",
                (scanner, system) -> {
                    System.out.print("Enter role name: ");
                    String roleName = scanner.nextLine().trim();

                    System.out.print("Enter permission name: ");
                    String name = scanner.nextLine().trim();

                    System.out.print("Enter permission resource: ");
                    String resource = scanner.nextLine().trim();

                    System.out.print("Enter permission description: ");
                    String description = scanner.nextLine().trim();

                    try {
                        Permission permission = new Permission(name, resource, description);
                        system.getRoleManager().addPermissionToRole(roleName, permission);
                    } catch (Exception e) {
                        System.out.printf("Error adding permission to role: %s\n", e.getMessage());
                        return;
                    }

                    System.out.println("Permission has been successfully added to role");
                });

        parser.registerCommand("role-remove-permission", "Remove permission from role",
                (scanner, system) -> {
                    System.out.print("Enter role name: ");
                    String roleName = scanner.nextLine().trim();

                    if (roleName.toLowerCase().contains("admin")) {
                        System.out.println("DON`T TOUCH ADMIN ROLE!");
                        return;
                    }

                    Optional<Role> role = system.getRoleManager().findByName(roleName);
                    if (role.isEmpty()) {
                        System.out.printf("Role with name '%s' not found\n", roleName);
                        return;
                    }

                    Set<Permission> permissions = role.get().getPermissions();
                    if (permissions.isEmpty()) {
                        System.out.printf("Role with name '%s' does not have any permissions\n", roleName);
                        return;
                    }

                    AtomicInteger i = new AtomicInteger();
                    permissions.forEach(permission -> {
                        System.out.println(i.incrementAndGet() + ". " + permission.format());
                    });

                    while (true) {
                        System.out.print("\nEnter permission number for deletion: ");
                        int num = scanner.nextInt();
                        scanner.nextLine();

                        if (num >= 1 && num <= permissions.size()) {
                            List<Permission> permissionsList = permissions.stream().toList();
                            Permission permission = permissionsList.get(num - 1);
                            system.getRoleManager().removePermissionFromRole(roleName, permission);
                            System.out.println("Permission has been successfully removed from role");
                            return;
                        }

                        System.out.printf("Permission number must be between 1 and %d\n", permissions.size());
                    }
                });

        parser.registerCommand("role-search", "Search roles by filters",
                (scanner, system) -> {
                    String[] options = {"by name (contains)",
                            "by permission",
                            "by minimal permissions count"};

                    int i = 1;
                    for (String option : options) {
                        System.out.println("\t" + i + ". " + option);
                        ++i;
                    }

                    System.out.print("Select filter number: ");
                    int filterNumber = scanner.nextInt();
                    scanner.nextLine();

                    RoleFilter filter = null;
                    switch (filterNumber) {
                        case 1 -> {
                            System.out.print("Enter role name: ");
                            String roleName = scanner.nextLine().trim();
                            filter = RoleFilters.byNameContains(roleName);
                        }

                        case 2 -> {
                            System.out.print("Enter permission name and permission resource by space: ");
                            String filterKey = scanner.nextLine().trim();
                            String[] filterKeys = filterKey.split("\\s+");
                            filter = RoleFilters.hasPermission(filterKeys[0], filterKeys[1]);
                        }

                        case 3 -> {
                            System.out.print("Enter permissions count : ");
                            int n = scanner.nextInt();
                            scanner.nextLine();
                            filter = RoleFilters.hasAtLeastNPermissions(n);
                        }

                        default -> {
                            System.out.println("Cancelling");
                            return;
                        }
                    }

                    List<Role> filteredRoles = system.getRoleManager().findByFilter(filter);
                    if (filteredRoles.isEmpty()) {
                        System.out.println("missing");
                        return;
                    }
                    System.out.println("Filtered roles:");
                    filteredRoles.forEach(role -> System.out.println(role.format()));
                });
    }

    private static void registerAssignmentCommands(CommandParser parser) {
        parser.registerCommand("assign-role", "Assign role for user",
                (scanner, system) -> {
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine().trim();

                    Optional<User> user = system.getUserManager().findByUsername(username);
                    if (user.isEmpty()) {
                        system.getLogger().log(
                                "assign-role", system.getCurrentUser(), "assignment", "error"
                        );
                        System.out.printf("User with username '%s' not found\n", username);
                        return;
                    }

                    List<Role> roles = system.getRoleManager().findAll().stream()
                            .filter(role -> !role.getName().toLowerCase().contains("admin"))
                            .toList();

                    if (roles.isEmpty()) {
                        system.getLogger().log(
                                "assign-role", system.getCurrentUser(), "assignment", "error"
                        );
                        System.out.println("No available roles");
                        return;
                    }

                    List<String> roleNames = new ArrayList<>();
                    roles.forEach(role -> roleNames.add(role.getName()));

                    System.out.println("Available roles:");
                    roles.forEach(System.out::println);

                    System.out.print("Enter role name: ");
                    String roleName = scanner.nextLine().trim();

                    if (!roleNames.contains(roleName)) {
                        system.getLogger().log(
                                "assign-role", system.getCurrentUser(), "assignment", "cancelling"
                        );
                        System.out.println("Cancelling");
                        return;
                    }

                    Optional<Role> role = system.getRoleManager().findByName(roleName);

                    System.out.print("Enter assignment type (permanent or temporary): ");
                    String assignmentType = scanner.nextLine().trim();

                    if (!assignmentType.equalsIgnoreCase("permanent") &&
                            !assignmentType.equalsIgnoreCase("temporary")) {
                        system.getLogger().log(
                                "assign-role", system.getCurrentUser(), "assignment", "cancelling"
                        );
                        System.out.println("Cancelling");
                        return;
                    }

                    String expiresAt = null;
                    if (assignmentType.equalsIgnoreCase("temporary")) {
                        System.out.print("Enter expiration date (ex. 2026-03-04 22:00:00 +07:00): ");
                        expiresAt = scanner.nextLine().trim();
                    }

                    System.out.print("Enter assignment reason: ");
                    String reason = scanner.nextLine().trim();

                    AssignmentMetadata metadata = AssignmentMetadata.now(system.getCurrentUser(), reason);

                    RoleAssignment assignment = null;
                    if (expiresAt == null) {
                        assignment = new PermanentAssignment(user.get(), role.get(), metadata);
                    } else {
                        assignment = new TemporaryAssignment(user.get(), role.get(), metadata, expiresAt, false);
                    }

                    try {
                        system.getAssignmentManager().add(assignment);
                    } catch (Exception e) {
                        system.getLogger().log(
                                "assign-role", system.getCurrentUser(), "assignment", "error"
                        );
                        System.out.printf("Error assigning role for user: %s\n", e.getMessage());
                        return;
                    }

                    system.getLogger().log(
                            "assign-role", system.getCurrentUser(), "assignment", "success"
                    );
                    System.out.println("Role has been successfully assigned for user");
                });

        parser.registerCommand("revoke-role", "Revoke role from user",
                (scanner, system) -> {
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine().trim();

                    Optional<User> user = system.getUserManager().findByUsername(username);
                    if (user.isEmpty()) {
                        system.getLogger().log(
                                "revoke-role", system.getCurrentUser(), "assignment", "error"
                        );
                        System.out.printf("User with username '%s' not found\n", username);
                        return;
                    }

                    List<RoleAssignment> activeAssignments = system.getAssignmentManager().findByUser(user.get())
                            .stream().filter(RoleAssignment::isActive).toList();

                    if (activeAssignments.isEmpty()) {
                        system.getLogger().log(
                                "revoke-role", system.getCurrentUser(), "assignment", "error"
                        );
                        System.out.println("No active assignments for this user");
                        return;
                    }

                    System.out.println("Active assignments for user:");
                    activeAssignments.forEach(roleAssignment -> {
                        AbstractRoleAssignment assignment = (AbstractRoleAssignment) roleAssignment;
                        System.out.println(assignment.summary());
                    });

                    System.out.print("\nEnter assignment id: ");
                    String assignmentId = scanner.nextLine().trim();

                    Optional<RoleAssignment> assignment = system.getAssignmentManager().findById(assignmentId);
                    if (assignment.isPresent() && assignment.get().role().getName().toLowerCase().contains("admin")) {
                        system.getLogger().log(
                                "revoke-role", system.getCurrentUser(), "assignment", "error"
                        );
                        System.out.println("Error revoking role from user: DON`T TOUCH ADMIN ROLE!");
                        return;
                    }

                    try {
                        system.getAssignmentManager().revokeAssignment(assignmentId);
                    } catch (Exception e) {
                        system.getLogger().log(
                                "revoke-role", system.getCurrentUser(), "assignment", "exception"
                        );
                        System.out.printf("Error revoking role from user: %s\n", e.getMessage());
                        return;
                    }

                    system.getLogger().log(
                            "revoke-role", system.getCurrentUser(), "assignment", "success"
                    );
                    System.out.println("Role has been successfully revoked from user");
                });

        parser.registerCommand("assignment-list", "List of all assignments",
                (scanner, system) -> {
                    List<RoleAssignment> assignments = system.getAssignmentManager().findAll();
                    if (assignments.isEmpty()) {
                        System.out.println("missing");
                        return;
                    }

                    System.out.println("-".repeat(97));
                    System.out.printf("| %-20s | %-10s | %-15s | %-10s | %-26s |\n",
                            "Username", "Role name", "Type", "Status", "Assigned at");
                    System.out.println("-".repeat(97));
                    assignments.forEach(assignment -> {
                        System.out.printf("| %-20s | %-10s | %-15s | %-10s | %-26s |\n",
                                assignment.user().username(),
                                assignment.role().getName(),
                                assignment.assignmentType(),
                                assignment.isActive() ? "ACTIVE" : "NOT ACTIVE",
                                assignment.metadata().assignedAt());
                        System.out.println("-".repeat(97));
                    });
                });

        parser.registerCommand("assignment-list-user", "List of all assignments for user",
                (scanner, system) -> {
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine().trim();

                    Optional<User> user = system.getUserManager().findByUsername(username);
                    if (user.isEmpty()) {
                        System.out.printf("User with username '%s' not found\n", username);
                        return;
                    }

                    List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user.get());
                    if (assignments.isEmpty()) {
                        System.out.println("missing");
                        return;
                    }

                    assignments.forEach(roleAssignment -> {
                        AbstractRoleAssignment assignment = (AbstractRoleAssignment) roleAssignment;
                        System.out.println(assignment.summary());
                    });
                });

        parser.registerCommand("assignment-list-role", "List of all assignments for role",
                (scanner, system) -> {
                    System.out.print("Enter role name: ");
                    String roleName = scanner.nextLine().trim();

                    Optional<Role> role = system.getRoleManager().findByName(roleName);
                    if (role.isEmpty()) {
                        System.out.printf("Role with name '%s' not found\n", roleName);
                        return;
                    }

                    List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role.get());
                    if (assignments.isEmpty()) {
                        System.out.println("missing");
                        return;
                    }

                    List<User> users = new ArrayList<>();
                    assignments.forEach(assignment -> users.add(assignment.user()));

                    System.out.println("This role assigned next users:");
                    users.forEach(user -> System.out.println(user.format()));
                });

        parser.registerCommand("assignment-active", "List of all active assignments",
                (scanner, system) -> {
                    List<RoleAssignment> assignments = system.getAssignmentManager().getActiveAssignments();

                    if (assignments.isEmpty()) {
                        System.out.println("missing");
                        return;
                    }

                    assignments.forEach(roleAssignment -> {
                        AbstractRoleAssignment assignment = (AbstractRoleAssignment) roleAssignment;
                        System.out.println(assignment.summary());
                    });
                });

        parser.registerCommand("assignment-expired", "List of all expired temporary assignments",
                (scanner, system) -> {
                    List<RoleAssignment> assignments = system.getAssignmentManager().getExpiredAssignments()
                            .stream().filter(assignment -> assignment.assignmentType().equals("TEMPORARY"))
                            .toList();

                    if (assignments.isEmpty()) {
                        System.out.println("missing");
                        return;
                    }

                    assignments.forEach(roleAssignment -> {
                        AbstractRoleAssignment assignment = (AbstractRoleAssignment) roleAssignment;
                        System.out.println(assignment.summary());
                    });
                });

        parser.registerCommand("assignment-extend", "Extend temporary assignment",
                (scanner, system) -> {
                    List<RoleAssignment> assignments = system.getAssignmentManager()
                            .findByFilter(AssignmentFilters.byType("TEMPORARY"));

                    if (assignments.isEmpty()) {
                        System.out.println("missing");
                        return;
                    }

                    System.out.println("Temporary assignments:");
                    assignments.forEach(roleAssignment -> {
                        AbstractRoleAssignment assignment = (AbstractRoleAssignment) roleAssignment;
                        System.out.println(assignment.summary());
                    });

                    System.out.print("\nEnter assignment id: ");
                    String assignmentId = scanner.nextLine().trim();

                    System.out.print("Enter new expiration date (ex. 2026-03-04 22:00:00 +07:00): ");
                    String expiresAt = scanner.nextLine().trim();

                    try {
                        system.getAssignmentManager().extendTemporaryAssignment(assignmentId, expiresAt);
                    } catch (Exception e) {
                        System.out.printf("Error extending temporary assignment: %s\n", e.getMessage());
                        return;
                    }

                    System.out.println("Temporary assignment has been successfully extended");
                });

        parser.registerCommand("assignment-search", "Search assignments by filters",
                (scanner, system) -> {
                    String[] options = {"by username",
                            "by roleName",
                            "by assignment type (PERMANENT / TEMPORARY)",
                            "by status (ACTIVE / NOT ACTIVE)",
                            "assigned after date",
                            "expiring before date"};

                    int i = 1;
                    for (String option : options) {
                        System.out.println("\t" + i + ". " + option);
                        ++i;
                    }

                    System.out.print("Select filter number: ");
                    int filterNumber = scanner.nextInt();
                    scanner.nextLine();

                    AssignmentFilter filter = null;
                    switch (filterNumber) {
                        case 1 -> {
                            System.out.print("Enter username: ");
                            String username = scanner.nextLine().trim();
                            filter = AssignmentFilters.byUsername(username);
                        }

                        case 2 -> {
                            System.out.print("Enter role name: ");
                            String roleName = scanner.nextLine().trim();
                            filter = AssignmentFilters.byRoleName(roleName);
                        }

                        case 3 -> {
                            System.out.print("Enter type: ");
                            String type = scanner.nextLine().trim();
                            filter = AssignmentFilters.byType(type);
                        }

                        case 4 -> {
                            System.out.print("Enter status: ");
                            String status = scanner.nextLine().trim();
                            filter = status.equalsIgnoreCase("ACTIVE") ? AssignmentFilters.activeOnly()
                                                                                : AssignmentFilters.inactiveOnly();
                        }

                        case 5 -> {
                            System.out.print("Enter datetime (ex. 2026-03-04 22:00:00 +07:00): ");
                            String datetime = scanner.nextLine().trim();
                            filter = AssignmentFilters.assignedAfter(datetime);
                        }

                        case 6 -> {
                            System.out.print("Enter datetime (ex. 2026-03-04 22:00:00 +07:00): ");
                            String datetime = scanner.nextLine().trim();
                            filter = AssignmentFilters.expiringBefore(datetime);
                        }

                        default -> {
                            System.out.println("Cancelling");
                            return;
                        }
                    }

                    List<RoleAssignment> filteredAssignments = system.getAssignmentManager().findByFilter(filter);
                    if (filteredAssignments.isEmpty()) {
                        System.out.println("missing");
                        return;
                    }
                    System.out.println("Filtered assignments:");
                    filteredAssignments.forEach(roleAssignment -> {
                        AbstractRoleAssignment assignment = (AbstractRoleAssignment) roleAssignment;
                        System.out.println(assignment.summary());
                    });
                });
    }

    private static void registerPermissionCommands(CommandParser parser) {
        parser.registerCommand("permissions-user", "All permissions of specific user",
                (scanner, system) -> {
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine().trim();

                    Optional<User> user = system.getUserManager().findByUsername(username);
                    if (user.isEmpty()) {
                        System.out.printf("User with username '%s' not found\n", username);
                        return;
                    }

                    List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user.get());
                    if (assignments.isEmpty()) {
                        System.out.printf("User with username '%s' does not have any roles\n", username);
                        return;
                    }

                    Map<String, Set<Permission>> permissionsByResource = assignments.stream()
                            .filter(RoleAssignment::isActive)
                            .flatMap(roleAssignment -> roleAssignment.role().getPermissions().stream())
                            .collect(Collectors.groupingBy(
                                    Permission::resource,
                                    Collectors.toCollection(HashSet::new)
                            ));

                    if (permissionsByResource.isEmpty()) {
                        System.out.printf("User with username '%s' has no permissions\n", username);
                        return;
                    }
                    permissionsByResource.forEach((resource, permissions) -> {
                        System.out.printf("\nResource: %s\n", resource);
                        System.out.println("  Permissions:");
                        permissions.forEach(perm -> System.out.println(perm.format()));
                    });
                });

        parser.registerCommand("permissions-check", "Check if user has specific permission",
                (scanner, system) -> {
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine().trim();

                    Optional<User> user = system.getUserManager().findByUsername(username);
                    if (user.isEmpty()) {
                        System.out.printf("User with username '%s' not found\n", username);
                        return;
                    }

                    System.out.print("Enter permission name: ");
                    String permissionName = scanner.nextLine().trim();

                    System.out.print("Enter permission resource: ");
                    String permissionResource = scanner.nextLine().trim();

                    boolean userHasPermission = system.getAssignmentManager().userHasPermission(user.get(),
                            permissionName, permissionResource);

                    if (!userHasPermission) {
                        System.out.println("missing");
                        return;
                    }

                    List<RoleAssignment> roleAssignments = system.getAssignmentManager().findByUser(user.get());
                    System.out.print("Roles that have this permission: ");
                    for (RoleAssignment assignment : roleAssignments) {
                        if (assignment.role().hasPermission(permissionName, permissionResource)) {
                            System.out.print(assignment.role().getName());
                        }
                    }
                });
    }

    private static void registerServiceCommands(CommandParser parser) {
        parser.registerCommand("help", "List of available commands",
                (scanner, system) -> {
                    parser.printHelp();
                });

        parser.registerCommand("stats", "System statistics",
                (scanner, system) -> {
                    System.out.println(system.generateStatistics());
                });

        parser.registerCommand("audit-logs", "Audit logs",
                (scanner, system) -> {
                    system.getLogger().printLogs();
                });

        parser.registerCommand("clear", "Clear screen",
                (scanner, system) -> {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                });

        parser.registerCommand("exit", "Exiting the program",
                (scanner, system) -> {
                    System.out.print("Confirm exiting the program? (y/n): ");
                    String answer1 = scanner.nextLine().toLowerCase();
                    if (answer1.equals("y")) {
                        System.out.print("Save data? (y/n): ");
                        String answer2 = scanner.nextLine().toLowerCase();

                        if (answer2.equals("y")) {
                            parser.executeCommand("save", scanner, system);
                        }

                        system.getLogger().saveToFile("rbac-log/log.txt");
                        System.out.println("You can view logs in rbac-log/log.txt");
                        scanner.close();
                        System.exit(0);
                    }
                    System.out.println("Cancelling exiting the program");
                });

        parser.registerCommand("save", "Save data to JSON file",
                (scanner, system) -> {
                    String filepath = "rbac-data/data.json";
                    Path path = Paths.get(filepath);

                    try {
                        Files.createDirectories(path.getParent());

                        if (!Files.exists(path)) {
                            Files.createFile(path);
                        }

                        List<User> users = system.getUserManager().findAll();
                        List<Role> roles = system.getRoleManager().findAll();
                        List<AbstractRoleAssignment> assignments = system.getAssignmentManager().findAll()
                                .stream()
                                .map(assignment -> (AbstractRoleAssignment) assignment)
                                .toList();

                        RBACSystemData systemData = new RBACSystemData(users, roles, assignments);

                        ObjectMapper mapper = new ObjectMapper();

                        mapper.enable(SerializationFeature.INDENT_OUTPUT);

                        mapper.writeValue(new File(filepath), systemData);
                    } catch (Exception e) {
                        System.out.printf("Error saving data to JSON file: %s\n", e.getMessage());
                        return;
                    }

                    System.out.printf("Data has been successfully saved to %s\n", filepath);
                });
    }
}