package com.example.command;

import com.example.filter.UserFilter;
import com.example.filter.UserFilters;
import com.example.model.Permission;
import com.example.model.RoleAssignment;
import com.example.model.User;

import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {
    public static void registerAllCommands(CommandParser parser) {
        registerUserCommands(parser);
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
                        System.out.printf("Error creating user: %s\n", e.getMessage());
                    }

                    if (newUser != null) {
                        system.getUserManager().add(newUser);
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
                    String answer = scanner.nextLine().trim();
                    if (!answer.equals("y".toLowerCase())) {
                        System.out.println("Canceling user deletion");
                        return;
                    }

                    Optional<User> user = system.getUserManager().findByUsername(username);
                    if (user.isEmpty()) {
                        System.out.printf("User with username '%s' not found\n", username);
                        return;
                    }

                    system.getAssignmentManager().findByUser(user.get()).forEach(
                            roleAssignment -> system.getAssignmentManager().remove(roleAssignment)
                    );

                    system.getUserManager().remove(user.get());
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

                    System.out.print("Select filter number (default 1): ");
                    int filterNumber = scanner.nextInt();
                    scanner.nextLine();

                    System.out.print("Enter filter key: ");
                    String filterKey = scanner.next().trim();
                    scanner.nextLine();

                    UserFilter filter = null;
                    switch (filterNumber) {
                        case 2 -> filter = UserFilters.byEmail(filterKey);
                        case 3 -> filter = UserFilters.byEmailDomain(filterKey);
                        case 4 -> filter = UserFilters.byFullNameContains(filterKey);
                        default -> filter = UserFilters.byUsernameContains(filterKey);
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

            parser.registerCommand("clear", "Clear screen",
                    (scanner, system) -> {
                        System.out.print("\033[H\033[2J");
                        System.out.flush();
                    });

            parser.registerCommand("exit", "Exiting the program",
                    (scanner, system) -> {
                        System.out.print("Confirm exiting the program? (y/n): ");
                        String answer = scanner.nextLine().toLowerCase();
                        if (answer.equals("y".toLowerCase())) {
                            scanner.close();
                            System.exit(0);
                        } else {
                            System.out.println("Cancelling exiting the program");
                        }
                    });
    }
}