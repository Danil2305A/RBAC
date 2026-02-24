package com.example.command;

import com.example.manager.AssignmentManager;
import com.example.manager.RoleManager;
import com.example.manager.UserManager;
import com.example.model.*;

import java.util.HashSet;
import java.util.Set;

public class RBACSystem {
    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private String currentUser;

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
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
    }

    public String generateStatistics() {
        return String.format(
                "Count of users: %d\nCount of roles: %d\nCount of assignments: %d",
                userManager.count(), roleManager.count(), assignmentManager.count()
        );
    }
}