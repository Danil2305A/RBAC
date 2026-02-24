package com.example;

import com.example.filter.AssignmentFilter;
import com.example.filter.AssignmentFilters;
import com.example.manager.AssignmentManager;
import com.example.manager.RoleManager;
import com.example.manager.UserManager;
import com.example.sorter.AssignmentSorters;
import org.junit.jupiter.api.*;

import java.util.*;

public class AssignmentManagerTest {
    private AssignmentManager assignmentManager;
    private UserManager userManager;
    private RoleManager roleManager;

    @BeforeEach
    public void initAssignmentManager() {
        assignmentManager = new AssignmentManager();
        userManager = new UserManager();
        roleManager = new RoleManager();

        assignmentManager.setUserManager(userManager);
        assignmentManager.setRoleManager(roleManager);
    }

    @Test
    @DisplayName("Успешное добавление нового назначения роли")
    public void addNewAssignment() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role = new Role("adminKAA", "Admin role", Collections.emptySet());
        RoleAssignment assignment = new PermanentAssignment(user, role, AssignmentMetadata.now("Ivan", null));

        userManager.add(user);
        roleManager.add(role);
        assignmentManager.add(assignment);

        Assertions.assertTrue(assignmentManager.findAll().contains(assignment));
        Assertions.assertEquals(1, assignmentManager.count());
    }

    @Test
    @DisplayName("Добавление назначения без установленных зависимостей - должно выброситься NullPointerException")
    public void addAssignmentWithoutDependencies() {
        AssignmentManager assignmentManagerWithoutDeps = new AssignmentManager();
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role = new Role("admin1", "Admin role", Collections.emptySet());
        RoleAssignment assignment = new PermanentAssignment(user, role, AssignmentMetadata.now("Ivan", null));

        userManager.add(user);
        roleManager.add(role);

        Assertions.assertThrows(NullPointerException.class,
                () -> assignmentManagerWithoutDeps.add(assignment));
    }

    @Test
    @DisplayName("Добавление назначения для несуществующего пользователя - должно выброситься IllegalArgumentException")
    public void addAssignmentForNonExistingUser() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role = new Role("admin2", "Admin role", Collections.emptySet());
        RoleAssignment assignment = new PermanentAssignment(user, role, AssignmentMetadata.now("Ivan", null));

        roleManager.add(role);

        Assertions.assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(assignment));
    }

    @Test
    @DisplayName("Добавление назначения для несуществующей роли - должно выброситься IllegalArgumentException")
    public void addAssignmentForNonExistingRole() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role = new Role("admin3", "Admin role", Collections.emptySet());
        RoleAssignment assignment = new PermanentAssignment(user, role, AssignmentMetadata.now("Ivan", null));

        userManager.add(user);

        Assertions.assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(assignment));
    }

    @Test
    @DisplayName("Добавление дублирующегося активного назначения - должно выброситься IllegalStateException")
    public void addDuplicateActiveAssignment() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role = new Role("admin4", "Admin role", Collections.emptySet());
        RoleAssignment assignment = new PermanentAssignment(user, role, AssignmentMetadata.now("Ivan", null));

        userManager.add(user);
        roleManager.add(role);
        assignmentManager.add(assignment);

        Assertions.assertThrows(IllegalStateException.class,
                () -> assignmentManager.add(assignment));
    }

    @Test
    @DisplayName("Успешное удаление назначения")
    public void removeAssignment() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role = new Role("admin11", "Admin role", Collections.emptySet());
        RoleAssignment assignment = new PermanentAssignment(user, role, AssignmentMetadata.now("Ivan", null));

        userManager.add(user);
        roleManager.add(role);
        assignmentManager.add(assignment);

        Assertions.assertTrue(assignmentManager.remove(assignment));
        Assertions.assertFalse(assignmentManager.findAll().contains(assignment));
        Assertions.assertEquals(0, assignmentManager.count());
    }

    @Test
    @DisplayName("Успешный поиск назначения по id")
    public void findAssignmentById() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role = new Role("admin12", "Admin role", Collections.emptySet());
        RoleAssignment assignment = new PermanentAssignment(user, role, AssignmentMetadata.now("Ivan", null));

        userManager.add(user);
        roleManager.add(role);
        assignmentManager.add(assignment);

        Assertions.assertTrue(assignmentManager.findById(assignment.assignmentId()).isPresent());
        Assertions.assertEquals(assignment, assignmentManager.findById(assignment.assignmentId()).get());
    }

    @Test
    @DisplayName("Поиск несуществующего назначения по id - должен вернуть пустой Optional")
    public void findNonExistingAssignmentById() {
        Assertions.assertTrue(assignmentManager.findById("non-existent-id").isEmpty());
    }

    @Test
    @DisplayName("Успешный поиск всех назначений")
    public void findAllAssignments() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role1 = new Role("admin13", "Admin role", Collections.emptySet());
        Role role2 = new Role("managerMain", "Manager role", Collections.emptySet());
        RoleAssignment assignment1 = new PermanentAssignment(user, role1, AssignmentMetadata.now("Ivan", null));
        RoleAssignment assignment2 = new PermanentAssignment(user, role2, AssignmentMetadata.now("Ivan", null));

        userManager.add(user);
        roleManager.add(role1);
        roleManager.add(role2);
        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);

        List<RoleAssignment> expectedAssignments = List.of(assignment1, assignment2);

        Assertions.assertTrue(assignmentManager.findAll().containsAll(expectedAssignments));
        Assertions.assertEquals(2, assignmentManager.count());
    }

    @Test
    @DisplayName("Успешный поиск назначений по пользователю")
    public void findAssignmentsByUser() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role1 = new Role("admin14", "Admin role", Collections.emptySet());
        Role role2 = new Role("manager1", "Manager role", Collections.emptySet());
        RoleAssignment assignment1 = new PermanentAssignment(user, role1, AssignmentMetadata.now("Ivan", null));
        RoleAssignment assignment2 = new PermanentAssignment(user, role2, AssignmentMetadata.now("Ivan", null));

        userManager.add(user);
        roleManager.add(role1);
        roleManager.add(role2);
        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);

        List<RoleAssignment> actualAssignments = assignmentManager.findByUser(user);

        Assertions.assertEquals(2, actualAssignments.size());
        Assertions.assertTrue(actualAssignments.contains(assignment1));
        Assertions.assertTrue(actualAssignments.contains(assignment2));
    }

    @Test
    @DisplayName("Успешный поиск назначений по роли")
    public void findAssignmentsByRole() {
        User user = User.validate("akuskodanya", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role1 = new Role("admin15", "Admin role", Collections.emptySet());
        Role role2 = new Role("manager2", "Manager role", Collections.emptySet());
        RoleAssignment assignment1 = new PermanentAssignment(user, role1, AssignmentMetadata.now("Ivan", null));
        RoleAssignment assignment2 = new PermanentAssignment(user, role2, AssignmentMetadata.now("Ivan", null));

        userManager.add(user);
        roleManager.add(role1);
        roleManager.add(role2);
        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);

        List<RoleAssignment> actualAssignments = assignmentManager.findByRole(role2);

        Assertions.assertEquals(1, actualAssignments.size());
        Assertions.assertTrue(actualAssignments.contains(assignment2));
    }

    @Test
    @DisplayName("Успешный поиск назначений по фильтру")
    public void findAssignmentsWithFilter() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role1 = new Role("admin45", "Admin role", Collections.emptySet());
        Role role2 = new Role("manager4", "Manager role", Collections.emptySet());
        Role role3 = new Role("viewer1", "Viewer role", Collections.emptySet());
        RoleAssignment assignment1 = new PermanentAssignment(user, role1, AssignmentMetadata.now("Ivan", null));
        RoleAssignment assignment2 = new PermanentAssignment(user, role2, AssignmentMetadata.now("Ivan", null));
        PermanentAssignment assignment3 = new PermanentAssignment(user, role3, AssignmentMetadata.now("Ivan", null));
        assignment3.revoke();

        userManager.add(user);
        roleManager.add(role1);
        roleManager.add(role2);
        roleManager.add(role3);
        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);
        assignmentManager.add(assignment3);

        AssignmentFilter filter = AssignmentFilters.activeOnly();
        List<RoleAssignment> actualAssignments = assignmentManager.findByFilter(filter);
        List<RoleAssignment> expectedAssignments = List.of(assignment1, assignment2);

        Assertions.assertTrue(actualAssignments.containsAll(expectedAssignments));
    }

    @Test
    @DisplayName("Успешный поиск назначений по фильтру и с учётом порядка")
    public void findAssignmentsWithFilterAndSorter() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role1 = new Role("admin55", "Admin role", Collections.emptySet());
        Role role2 = new Role("aamanager5", "Manager role", Collections.emptySet());
        Role role3 = new Role("viewer2", "Viewer role", Collections.emptySet());
        RoleAssignment assignment1 = new PermanentAssignment(user, role1, AssignmentMetadata.now("Ivan", null));
        RoleAssignment assignment2 = new PermanentAssignment(user, role2, AssignmentMetadata.now("Ivan", null));
        PermanentAssignment assignment3 = new PermanentAssignment(user, role3, AssignmentMetadata.now("Ivan", null));
        assignment3.revoke();

        userManager.add(user);
        roleManager.add(role1);
        roleManager.add(role2);
        roleManager.add(role3);
        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);
        assignmentManager.add(assignment3);

        AssignmentFilter filter = AssignmentFilters.activeOnly();
        Comparator<RoleAssignment> sorter = AssignmentSorters.byRoleName();
        List<RoleAssignment> actualAssignments = assignmentManager.findAll(filter, sorter);
        List<RoleAssignment> expectedAssignments = List.of(assignment2, assignment1);

        Assertions.assertEquals(expectedAssignments, actualAssignments);
    }

    @Test
    @DisplayName("Успешное получение активных назначений")
    public void getActiveAssignments() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role1 = new Role("admin65", "Admin role", Collections.emptySet());
        Role role2 = new Role("manager5", "Manager role", Collections.emptySet());
        Role role3 = new Role("viewer3", "Viewer role", Collections.emptySet());
        RoleAssignment assignment1 = new PermanentAssignment(user, role1, AssignmentMetadata.now("Ivan", null));
        RoleAssignment assignment2 = new PermanentAssignment(user, role2, AssignmentMetadata.now("Ivan", null));
        PermanentAssignment assignment3 = new PermanentAssignment(user, role3, AssignmentMetadata.now("Ivan", null));
        assignment3.revoke();

        userManager.add(user);
        roleManager.add(role1);
        roleManager.add(role2);
        roleManager.add(role3);
        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);
        assignmentManager.add(assignment3);

        List<RoleAssignment> actualAssignments = assignmentManager.getActiveAssignments();
        List<RoleAssignment> expectedAssignments = List.of(assignment1, assignment2);

        Assertions.assertTrue(actualAssignments.containsAll(expectedAssignments));
        Assertions.assertEquals(expectedAssignments.size(), actualAssignments.size());
    }

    @Test
    @DisplayName("Успешное получение истекших назначений")
    public void getExpiredAssignments() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role1 = new Role("admin50", "Admin role", Collections.emptySet());
        Role role2 = new Role("manager6", "Manager role", Collections.emptySet());
        Role role3 = new Role("viewer4", "Viewer role", Collections.emptySet());
        RoleAssignment assignment1 = new PermanentAssignment(user, role1, AssignmentMetadata.now("Ivan", null));
        RoleAssignment assignment2 = new TemporaryAssignment(user,
                role2,
                AssignmentMetadata.now("Ivan", null),
                "2026-02-24 15:00:00 +07:00", false);
        PermanentAssignment assignment3 = new PermanentAssignment(user, role3, AssignmentMetadata.now("Ivan", null));
        assignment3.revoke();

        userManager.add(user);
        roleManager.add(role1);
        roleManager.add(role2);
        roleManager.add(role3);
        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);
        assignmentManager.add(assignment3);

        List<RoleAssignment> actualAssignments = assignmentManager.getExpiredAssignments();
        List<RoleAssignment> expectedAssignments = List.of(assignment2, assignment3);

        Assertions.assertTrue(actualAssignments.containsAll(expectedAssignments));
        Assertions.assertEquals(expectedAssignments.size(), actualAssignments.size());
    }

    @Test
    @DisplayName("Успешная проверка наличия роли у пользователя")
    public void userHasRole() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role1 = new Role("admin75", "Admin role", Collections.emptySet());
        Role role2 = new Role("manager7", "Manager role", Collections.emptySet());
        Role role3 = new Role("viewer8", "Viewer role", Collections.emptySet());
        RoleAssignment assignment1 = new PermanentAssignment(user, role1, AssignmentMetadata.now("Ivan", null));
        RoleAssignment assignment2 = new PermanentAssignment(user, role2, AssignmentMetadata.now("Ivan", null));
        PermanentAssignment assignment3 = new PermanentAssignment(user, role3, AssignmentMetadata.now("Ivan", null));
        assignment3.revoke();

        userManager.add(user);
        roleManager.add(role1);
        roleManager.add(role2);
        roleManager.add(role3);
        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);
        assignmentManager.add(assignment3);

        Assertions.assertTrue(assignmentManager.userHasRole(user, role1));
        Assertions.assertTrue(assignmentManager.userHasRole(user, role2));
        Assertions.assertFalse(assignmentManager.userHasRole(user, role3));
    }

    @Test
    @DisplayName("Успешная проверка наличия разрешения у пользователя")
    public void userHasPermission() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role = new Role("admin85", "Admin role", new HashSet<>());
        RoleAssignment assignment = new PermanentAssignment(user, role, AssignmentMetadata.now("Ivan", null));

        userManager.add(user);
        roleManager.add(role);

        Permission permission = new Permission("WRITE", "files", "DESCR");
        roleManager.addPermissionToRole("admin85", permission);

        assignmentManager.add(assignment);

        Assertions.assertTrue(assignmentManager.userHasPermission(user, "WRITE", "files"));
    }

    @Test
    @DisplayName("Успешное получение всех разрешений пользователя")
    public void getUserPermissions() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role = new Role("admin95", "Admin role", new HashSet<>());
        RoleAssignment assignment = new PermanentAssignment(user, role, AssignmentMetadata.now("Ivan", null));

        userManager.add(user);
        roleManager.add(role);

        Permission permission1 = new Permission("WRITE", "files", "DESCR");
        Permission permission2 = new Permission("read", "files", "DESCR");
        roleManager.addPermissionToRole("admin95", permission1);
        roleManager.addPermissionToRole("admin95", permission2);

        assignmentManager.add(assignment);

        Assertions.assertEquals(2, assignmentManager.getUserPermissions(user).size());
    }

    @Test
    @DisplayName("Успешный отзыв назначения")
    public void revokePermanentAssignment() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role = new Role("admin05", "Admin role", Collections.emptySet());
        PermanentAssignment assignment = new PermanentAssignment(user, role, AssignmentMetadata.now("Ivan", null));

        userManager.add(user);
        roleManager.add(role);
        assignmentManager.add(assignment);

        assignmentManager.revokeAssignment(assignment.assignmentId());

        Assertions.assertTrue(assignment.isRevoked());
        Assertions.assertEquals(1, assignmentManager.getExpiredAssignments().size());
    }

    @Test
    @DisplayName("Успешное удаление временного назначения при отзыве")
    public void revokeTemporaryAssignment() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role = new Role("admin06", "Admin role", Collections.emptySet());
        TemporaryAssignment assignment = new TemporaryAssignment(user,
                role,
                AssignmentMetadata.now("Ivan", null),
                "2026-02-24 15:00:00 +07:00", false);

        userManager.add(user);
        roleManager.add(role);
        assignmentManager.add(assignment);

        assignmentManager.revokeAssignment(assignment.assignmentId());

        Assertions.assertFalse(assignmentManager.findAll().contains(assignment));
    }

    @Test
    @DisplayName("Отзыв несуществующего назначения - должно выброситься IllegalArgumentException")
    public void revokeNonExistingAssignment() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> assignmentManager.revokeAssignment("non-existent-id"));
    }

    @Test
    @DisplayName("Успешное продление временного назначения")
    public void extendTemporaryAssignment() {
        User user = User.validate("akuskodan", "Akusko Danil Dmitrievich", "akusko@gmail.com");
        Role role = new Role("admin07", "Admin role", Collections.emptySet());
        TemporaryAssignment assignment = new TemporaryAssignment(user,
                role,
                AssignmentMetadata.now("Ivan", null),
                "2026-02-24 15:00:00 +07:00", false);

        userManager.add(user);
        roleManager.add(role);
        assignmentManager.add(assignment);

        assignmentManager.extendTemporaryAssignment(assignment.assignmentId(), "3000-02-24 17:00:00 +07:00");

        Assertions.assertTrue(assignmentManager.getActiveAssignments().getFirst().isActive());
        Assertions.assertEquals("3000-02-24 17:00:00 +07:00", assignment.getExpiresAt());
    }

    @Test
    @DisplayName("Продление несуществующего назначения - должно выброситься IllegalArgumentException")
    public void extendNonExistingAssignment() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> assignmentManager.extendTemporaryAssignment("non-existent-id",
                        "3000-02-24 17:00:00 +07:00"));
    }
}