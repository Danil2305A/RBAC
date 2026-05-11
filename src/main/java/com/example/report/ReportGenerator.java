package com.example.report;

import com.example.manager.UserManager;
import com.example.manager.RoleManager;
import com.example.manager.AssignmentManager;
import com.example.model.User;
import com.example.model.Role;
import com.example.model.RoleAssignment;
import com.example.model.Permission;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {
    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            sb.append("No users found in the system\n");
            return sb.toString();
        }

        for (User user : users) {
            sb.append(String.format("%s: ", user.username()));

            List<RoleAssignment> activeAssignments = assignmentManager.findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .toList();

            if (activeAssignments.isEmpty()) {
                sb.append("no active roles");
            } else {
                String roles = activeAssignments.stream()
                        .map(assignment -> assignment.role().getName())
                        .collect(Collectors.joining(", "));
                sb.append(roles);
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        List<Role> roles = roleManager.findAll();

        if (roles.isEmpty()) {
            sb.append("No roles found in the system\n");
            return sb.toString();
        }

        for (Role role : roles) {
            List<RoleAssignment> assignments = assignmentManager.findByRole(role);

            long userCount = assignments.stream()
                    .filter(RoleAssignment::isActive)
                    .map(RoleAssignment::user)
                    .distinct()
                    .count();

            sb.append(String.format("%s: %d\n", role.getName(), userCount));
        }

        return sb.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            sb.append("No users found in the system\n");
            return sb.toString();
        }

        Set<String> allResources = new TreeSet<>();
        Map<User, Set<Permission>> userPermissions = new HashMap<>();

        for (User user : users) {
            Set<Permission> permissions = assignmentManager.getUserPermissions(user);
            userPermissions.put(user, permissions);
            permissions.stream()
                    .map(Permission::resource)
                    .forEach(allResources::add);
        }

        if (allResources.isEmpty()) {
            sb.append("No permissions found in the system\n");
            return sb.toString();
        }

        Map<User, Map<String, String>> matrix = new HashMap<>();

        for (User user : users) {
            Map<String, String> userResourcePermissions = new TreeMap<>();
            Set<Permission> permissions = userPermissions.getOrDefault(user, Collections.emptySet());

            for (String resource : allResources) {
                String permissionLetters = permissions.stream()
                        .filter(p -> p.resource().equals(resource))
                        .map(p -> String.valueOf(p.name().charAt(0))) // Берём первую букву
                        .sorted()
                        .collect(Collectors.joining(""));

                if (!permissionLetters.isEmpty()) {
                    userResourcePermissions.put(resource, permissionLetters);
                }
            }

            matrix.put(user, userResourcePermissions);
        }

        sb.append(String.format("%-20s", "User\\Resource"));
        for (String resource : allResources) {
            sb.append(String.format(" | %-10s", truncate(resource, 10)));
        }
        sb.append("\n");
        sb.append("-".repeat(20 + allResources.size() * 13)).append("\n");

        for (User user : users) {
            sb.append(String.format("%-20s", truncate(user.username(), 20)));

            Map<String, String> userResourcePermissions = matrix.get(user);

            for (String resource : allResources) {
                String perms = userResourcePermissions.getOrDefault(resource, "");

                if (perms.isEmpty()) {
                    sb.append(String.format(" | %-10s", "—"));
                } else {
                    sb.append(String.format(" | %-10s", perms));
                }
            }
            sb.append("\n");
        }

        sb.append("\nLegend (first letters):\n");

        Set<String> allFirstLetters = new TreeSet<>();
        for (User user : users) {
            Set<Permission> permissions = userPermissions.get(user);
            permissions.stream()
                    .map(p -> String.valueOf(p.name().charAt(0)))
                    .forEach(allFirstLetters::add);
        }

        if (!allFirstLetters.isEmpty()) {
            sb.append("  ");
            for (String letter : allFirstLetters) {
                String fullName = userPermissions.values().stream()
                        .flatMap(Set::stream)
                        .filter(p -> p.name().startsWith(letter))
                        .map(Permission::name)
                        .findFirst()
                        .orElse(letter);
                sb.append(String.format("%s=%s ", letter, fullName));
            }
            sb.append("\n");
        }

        sb.append("  — = no permissions\n");

        return sb.toString();
    }

    public String generateUserReportParallel(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            sb.append("No users found in the system\n");
            return sb.toString();
        }

        Map<String, String> userToRolesMap = users.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        User::username,
                        user -> {
                            List<RoleAssignment> activeAssignments = assignmentManager.findByUser(user).stream()
                                    .filter(RoleAssignment::isActive)
                                    .toList();

                            if (activeAssignments.isEmpty()) {
                                return "no active roles";
                            } else {
                                return activeAssignments.stream()
                                        .map(assignment -> assignment.role().getName())
                                        .collect(Collectors.joining(", "));
                            }
                        }
                ));

        userToRolesMap.forEach((key, value) -> sb.append(String.format("%s: %s\n", key, value)));

        return sb.toString();
    }

    public String generatePermissionMatrixParallel(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            sb.append("No users found in the system\n");
            return sb.toString();
        }

        Map<User, Set<Permission>> userPermissions = users.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        user -> user,
                        assignmentManager::getUserPermissions
                ));

        Set<String> allResources = userPermissions.values().stream()
                .flatMap(Set::stream)
                .map(Permission::resource)
                .collect(Collectors.toCollection(TreeSet::new));

        if (allResources.isEmpty()) {
            sb.append("No permissions found in the system\n");
            return sb.toString();
        }

        Map<User, Map<String, String>> matrix = users.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        user -> user,
                        user -> {
                            Map<String, String> userResourcePermissions = new TreeMap<>();
                            Set<Permission> permissions = userPermissions.getOrDefault(user, Collections.emptySet());

                            for (String resource : allResources) {
                                String permissionLetters = permissions.stream()
                                        .filter(p -> p.resource().equals(resource))
                                        .map(p -> String.valueOf(p.name().charAt(0)))
                                        .sorted()
                                        .collect(Collectors.joining(""));

                                if (!permissionLetters.isEmpty()) {
                                    userResourcePermissions.put(resource, permissionLetters);
                                }
                            }
                            return userResourcePermissions;
                        }
                ));

        sb.append(String.format("%-20s", "User\\Resource"));
        for (String resource : allResources) {
            sb.append(String.format(" | %-10s", truncate(resource, 10)));
        }
        sb.append("\n");
        sb.append("-".repeat(20 + allResources.size() * 13)).append("\n");

        for (User user : users) {
            sb.append(String.format("%-20s", truncate(user.username(), 20)));

            Map<String, String> userResourcePermissions = matrix.get(user);

            for (String resource : allResources) {
                String perms = userResourcePermissions != null ? userResourcePermissions.getOrDefault(resource, "") : "";

                if (perms.isEmpty()) {
                    sb.append(String.format(" | %-10s", "—"));
                } else {
                    sb.append(String.format(" | %-10s", perms));
                }
            }
            sb.append("\n");
        }

        sb.append("\nLegend (first letters):\n");
        Set<String> allFirstLetters = userPermissions.values().stream()
                .flatMap(Set::stream)
                .map(p -> String.valueOf(p.name().charAt(0)))
                .collect(Collectors.toCollection(TreeSet::new));

        if (!allFirstLetters.isEmpty()) {
            sb.append("  ");
            for (String letter : allFirstLetters) {
                String fullName = userPermissions.values().stream()
                        .flatMap(Set::stream)
                        .filter(p -> p.name().startsWith(letter))
                        .map(Permission::name)
                        .findFirst()
                        .orElse(letter);
                sb.append(String.format("%s=%s ", letter, fullName));
            }
            sb.append("\n");
        }

        sb.append("  — = no permissions\n");

        return sb.toString();
    }

    public void exportToFile(String report, String filepath) throws IOException {
        Path path = Paths.get(filepath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            writer.print(report);
        }

        System.out.printf("Report successfully exported to %s\n", filepath);
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 4) + "...";
    }
}