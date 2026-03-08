package com.example.report;

import com.example.manager.UserManager;
import com.example.manager.RoleManager;
import com.example.manager.AssignmentManager;
import com.example.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ReportGeneratorTest {

    private ReportGenerator reportGenerator;
    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;

    @TempDir
    Path tempDir;

    private User user1;
    private User user2;
    private User user3;
    private Role role1;
    private Role role2;
    private Role role3;
    private Permission permission1;
    private Permission permission2;
    private Permission permission3;
    private AssignmentMetadata metadata;

    @BeforeEach
    void setUp() {
        reportGenerator = new ReportGenerator();

        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager();

        assignmentManager.setUserManager(userManager);
        assignmentManager.setRoleManager(roleManager);
        roleManager.setAssignmentManager(assignmentManager);

        setupTestData();
    }

    private void setupTestData() {
        try {
            var usedNamesField = Role.class.getDeclaredField("usedNames");
            usedNamesField.setAccessible(true);
            Set<String> usedNames = (Set<String>) usedNamesField.get(null);
            usedNames.clear();
        } catch (Exception e) {
        }

        user1 = User.validate("john", "John Doe", "john@test.com");
        user2 = User.validate("jane", "Jane Smith", "jane@test.com");
        user3 = User.validate("bob", "Bob Wilson", "bob@test.com");

        userManager.add(user1);
        userManager.add(user2);
        userManager.add(user3);

        permission1 = new Permission("READ", "users", "Can read users");
        permission2 = new Permission("WRITE", "users", "Can write users");
        permission3 = new Permission("READ", "reports", "Can read reports");

        Set<Permission> adminPermissions = new HashSet<>();
        adminPermissions.add(permission1);
        adminPermissions.add(permission2);
        adminPermissions.add(permission3);

        Set<Permission> viewerPermissions = new HashSet<>();
        viewerPermissions.add(permission1);

        Set<Permission> editorPermissions = new HashSet<>();
        editorPermissions.add(permission1);
        editorPermissions.add(permission2);

        role1 = new Role("Admin", "Administrator role", adminPermissions);
        role2 = new Role("Viewer", "Viewer role", viewerPermissions);
        role3 = new Role("Editor", "Editor role", editorPermissions);

        roleManager.add(role1);
        roleManager.add(role2);
        roleManager.add(role3);

        metadata = AssignmentMetadata.now("system", "test assignment");

        assignmentManager.add(new PermanentAssignment(user1, role1, metadata)); // john - Admin
        assignmentManager.add(new PermanentAssignment(user2, role2, metadata)); // jane - Viewer
        assignmentManager.add(new PermanentAssignment(user3, role3, metadata)); // bob - Editor

        TemporaryAssignment expiredAssignment = new TemporaryAssignment(
                user2, role3, metadata, "2020-01-01 00:00:00 +00:00", false
        );
        assignmentManager.add(expiredAssignment);
    }

    @Test
    @DisplayName("Генерация user report при наличии активных назначенных ролей")
    void testGenerateUserReport() {
        String report = reportGenerator.generateUserReport(userManager, assignmentManager);

        assertTrue(report.contains("john: Admin"));
        assertTrue(report.contains("jane: Viewer"));
        assertTrue(report.contains("bob: Editor"));
        assertFalse(report.contains("no active roles"));
    }

    @Test
    @DisplayName("Генерация user report при отсутствиии активных назначенных ролей")
    void testGenerateUserReportWithNoRoles() {
        User newUser = User.validate("alice", "Alice Brown", "alice@test.com");
        userManager.add(newUser);

        String report = reportGenerator.generateUserReport(userManager, assignmentManager);

        assertTrue(report.contains("alice: no active roles"));
    }

    @Test
    @DisplayName("Генерация user report при отсуствии пользователей")
    void testGenerateUserReportWithNoUsers() {
        userManager.clear();

        String report = reportGenerator.generateUserReport(userManager, assignmentManager);

        assertEquals("No users found in the system\n", report);
    }

    @Test
    @DisplayName("Генерация role report с ненулевыми количествами пользователей")
    void testGenerateRoleReport() {
        String report = reportGenerator.generateRoleReport(roleManager, assignmentManager);

        assertTrue(report.contains("Admin: 1"));
        assertTrue(report.contains("Viewer: 1"));
        assertTrue(report.contains("Editor: 1"));

        assertFalse(report.contains("Editor: 2"));
    }

    @Test
    @DisplayName("Генерация role report с нулевыми количествами пользователей")
    void testGenerateRoleReportWithNoUsers() {
        Role emptyRole = new Role("Empty", "Empty role", new HashSet<>());
        roleManager.add(emptyRole);

        String report = reportGenerator.generateRoleReport(roleManager, assignmentManager);

        assertTrue(report.contains("Empty: 0"));
    }

    @Test
    @DisplayName("Генерация role-report при отсутствии ролей")
    void testGenerateRoleReportWithNoRoles() {
        roleManager.clear();

        String report = reportGenerator.generateRoleReport(roleManager, assignmentManager);

        assertEquals("No roles found in the system\n", report);
    }

    @Test
    @DisplayName("Генерация permission matrix")
    void testGeneratePermissionMatrix() {
        String report = reportGenerator.generatePermissionMatrix(userManager, assignmentManager);

        assertTrue(report.contains("User\\Resource"));
        assertTrue(report.contains("users"));
        assertTrue(report.contains("reports"));

        assertTrue(report.contains("john"));
        assertTrue(report.contains("R"));
        assertTrue(report.contains("W"));

        assertTrue(report.contains("jane"));
        assertTrue(report.contains("R"));

        assertTrue(report.contains("bob"));
        assertTrue(report.contains("R"));
        assertTrue(report.contains("W"));

        assertTrue(report.contains("Legend (first letters):"));
        assertTrue(report.contains("— = no permissions"));
    }

    @Test
    @DisplayName("Генерация permission matrix при отсутствии пользователей")
    void testGeneratePermissionMatrixWithNoUsers() {
        userManager.clear();

        String report = reportGenerator.generatePermissionMatrix(userManager, assignmentManager);

        assertEquals("No users found in the system\n", report);
    }

    @Test
    @DisplayName("Генерация permission matrix при отсутствии прав")
    void testGeneratePermissionMatrixWithNoPermissions() {
        userManager.clear();
        roleManager.clear();
        assignmentManager.clear();

        User newUser = User.validate("alice", "Alice Brown", "alice@test.com");
        userManager.add(newUser);

        Role emptyRole = new Role("Empty", "Empty role", new HashSet<>());
        roleManager.add(emptyRole);

        String report = reportGenerator.generatePermissionMatrix(userManager, assignmentManager);

        assertTrue(report.contains("No permissions found in the system"));
    }

    @Test
    @DisplayName("Экспорт отчёта в текстовый файл")
    void testExportToFile() throws IOException {
        String report = "Test report content";
        String filepath = tempDir.resolve("test_report.txt").toString();

        reportGenerator.exportToFile(report, filepath);

        Path path = Paths.get(filepath);
        assertTrue(Files.exists(path));

        String content = Files.readString(path);
        assertEquals(report, content);
    }

    @Test
    @DisplayName("Создание директорий при экспорте отчёта")
    void testExportToFileCreatesDirectories() throws IOException {
        String report = "Test report content";
        String filepath = tempDir.resolve("subdir/nested/test_report.txt").toString();

        reportGenerator.exportToFile(report, filepath);

        Path path = Paths.get(filepath);
        assertTrue(Files.exists(path));
        assertTrue(Files.exists(path.getParent()));

        String content = Files.readString(path);
        assertEquals(report, content);
    }

    @Test
    @DisplayName("Проверка, что несколько пользователей могут иметь одинаковые роли")
    void testMultipleUsersSameRole() {
        User user4 = User.validate("charlie", "Charlie Brown", "charlie@test.com");
        userManager.add(user4);
        assignmentManager.add(new PermanentAssignment(user4, role2, metadata));

        String userReport = reportGenerator.generateUserReport(userManager, assignmentManager);
        String roleReport = reportGenerator.generateRoleReport(roleManager, assignmentManager);

        assertTrue(userReport.contains("charlie: Viewer"));
        assertTrue(roleReport.contains("Viewer: 2"));
    }

    @Test
    @DisplayName("Проверка, что пользователь может иметь несколько ролей")
    void testUserWithMultipleRoles() {
        assignmentManager.add(new PermanentAssignment(user1, role2, metadata));

        String userReport = reportGenerator.generateUserReport(userManager, assignmentManager);
        String johnLine = userReport.lines()
                .filter(line -> line.startsWith("john:"))
                .findFirst()
                .orElse("");

        assertTrue(johnLine.contains("Admin"));
        assertTrue(johnLine.contains("Viewer"));

        String[] roles = johnLine.replace("john:", "").trim().split(", ");
        assertEquals(2, roles.length);
        assertTrue(Arrays.asList(roles).contains("Admin"));
        assertTrue(Arrays.asList(roles).contains("Viewer"));
    }

    @Test
    @DisplayName("Игнорирование неактивных назначений в отчетах")
    void testIgnoreInactiveAssignments() {
        TemporaryAssignment expiredAssignment = new TemporaryAssignment(
                user1, role3, metadata, "2020-01-01 00:00:00 +00:00", false
        );
        assignmentManager.add(expiredAssignment);

        String userReport = reportGenerator.generateUserReport(userManager, assignmentManager);
        String roleReport = reportGenerator.generateRoleReport(roleManager, assignmentManager);

        assertTrue(userReport.contains("john: Admin"));
        assertFalse(userReport.contains("john: Admin, Editor"));

        assertTrue(roleReport.contains("Editor: 1"));
        assertFalse(roleReport.contains("Editor: 2"));
    }

    @Test
    @DisplayName("Обработка случая, когда пустой user list во всех отчетах")
    void testEmptyUserListInAllReports() {
        userManager.clear();
        roleManager.clear();

        assertDoesNotThrow(() -> {
            reportGenerator.generateUserReport(userManager, assignmentManager);
            reportGenerator.generateRoleReport(roleManager, assignmentManager);
            reportGenerator.generatePermissionMatrix(userManager, assignmentManager);
        });
    }
}