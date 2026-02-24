package com.example.manager;

import com.example.filter.RoleFilter;
import com.example.filter.RoleFilters;
import com.example.model.*;
import com.example.sorter.RoleSorters;
import org.junit.jupiter.api.*;

import java.util.*;

public class RoleManagerTest {
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;

    @BeforeEach
    public void initRoleManager() {
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager();
        roleManager.setAssignmentManager(assignmentManager);
    }

    @Test
    @DisplayName("Успешное добавление новой роли")
    public void addNewRole() {
        Role role = new Role("Admin", "Administrator role", Collections.emptySet());

        roleManager.add(role);
        Assertions.assertTrue(roleManager.findAll().contains(role));
    }

    @Test
    @DisplayName("Добавление существующей роли c тем же name - дожл")
    public void addExistingRole() {
        Role role = new Role("Manager", "Manager role", Collections.emptySet());

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new Role("Manager", "Manager role", Collections.emptySet()));

    }

    @Test
    @DisplayName("Успешное удаление роли без привязанных назначений")
    public void removeRoleWithoutAssignments() {
        Role role = new Role("admin", "Administrator role", Collections.emptySet());

        roleManager.add(role);
        boolean removed = roleManager.remove(role);

        Assertions.assertTrue(removed);
        Assertions.assertFalse(roleManager.findAll().contains(role));
    }

    @Test
    @DisplayName("Удаление роли с привязанными назначениями - удаление не должно произойти")
    public void removeRoleWithAssignments() {
        Role role = new Role("manager", "Manager role", Collections.emptySet());
        User user = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");

        roleManager.add(role);

        UserManager userManager = new UserManager();
        userManager.add(user);

        RoleManager roleManager = new RoleManager();
        roleManager.add(role);

        RoleAssignment assignment = new PermanentAssignment(user, role, AssignmentMetadata.now("Ivan", null));
        assignmentManager.setUserManager(userManager);
        assignmentManager.setRoleManager(roleManager);
        assignmentManager.add(assignment);

        roleManager.setAssignmentManager(assignmentManager);
        boolean removed = roleManager.remove(role);

        Assertions.assertFalse(removed);
        Assertions.assertTrue(roleManager.findAll().contains(role));
    }

    @Test
    @DisplayName("Удаление роли без установленного AssignmentManager - должно выброситься NullPointerException")
    public void removeRoleWithoutAssignmentManager() {
        RoleManager roleManagerWithoutAssignment = new RoleManager();
        Role role = new Role("adminka", "Administrator role", Collections.emptySet());
        roleManagerWithoutAssignment.add(role);

        Assertions.assertThrows(NullPointerException.class, () -> roleManagerWithoutAssignment.remove(role));
    }

    @Test
    @DisplayName("Успешный поиск роли по id")
    public void findRoleById() {
        Role role = new Role("Adminkaa", "Administrator role", Collections.emptySet());
        roleManager.add(role);

        Assertions.assertTrue(roleManager.findById(role.getId()).isPresent());
        Assertions.assertEquals(role, roleManager.findById(role.getId()).get());
    }

    @Test
    @DisplayName("Поиск несуществующей роли по id - должен вернуть пустой Optional")
    public void findNonExistingRoleById() {
        Assertions.assertTrue(roleManager.findById("non-existent-id").isEmpty());
    }

    @Test
    @DisplayName("Успешный поиск роли по имени")
    public void findRoleByName() {
        Role role = new Role("BigRole", "Big role", Collections.emptySet());

        roleManager.add(role);

        Assertions.assertTrue(roleManager.findByName("BigRole").isPresent());
        Assertions.assertEquals(role, roleManager.findByName("BigRole").get());
    }

    @Test
    @DisplayName("Поиск несуществующей роли по имени - должен вернуть пустой Optional")
    public void findNonExistingRoleByName() {
        Assertions.assertTrue(roleManager.findByName("USER").isEmpty());
    }

    @Test
    @DisplayName("Успешный поиск всех ролей")
    public void findAllRoles() {
        Role role1 = new Role("role1", "first role", Collections.emptySet());
        Role role2 = new Role("role2", "second role", Collections.emptySet());
        Role role3 = new Role("role3", "third role", Collections.emptySet());

        List<Role> expectedRoleList = List.of(role1, role2, role3);

        roleManager.add(role1);
        roleManager.add(role2);
        roleManager.add(role3);

        Assertions.assertTrue(roleManager.findAll().containsAll(expectedRoleList));
        Assertions.assertEquals(3, roleManager.count());
    }

    @Test
    @DisplayName("Успешный поиск ролей по фильтру")
    public void findRolesWithFilter() {
        Role role1 = new Role("admin19", "Administrator role", new HashSet<>());
        Role role2 = new Role("viewer", "Regular user role", new HashSet<>());
        Role role3 = new Role("manager11", "Manager role", new HashSet<>());
        Role role4 = new Role("superadmin", "Super administrator role", new HashSet<>());

        role1.addPermission(new Permission("READ", "files", "descr"));
        role1.addPermission(new Permission("WRITE", "files", "descr"));
        role2.addPermission(new Permission("READ", "files", "descr"));
        role3.addPermission(new Permission("READ", "files", "descr"));
        role3.addPermission(new Permission("EXECUTE", "files", "descr"));
        role4.addPermission(new Permission("READ", "files", "descr"));
        role4.addPermission(new Permission("WRITE", "files", "descr"));
        role4.addPermission(new Permission("DELETE", "files", "descr"));

        RoleFilter filter = RoleFilters.hasPermission("WRITE", "files");
        List<Role> expectedRoleList = List.of(role1, role4);

        roleManager.add(role1);
        roleManager.add(role2);
        roleManager.add(role3);
        roleManager.add(role4);

        List<Role> filteredRoles = roleManager.findByFilter(filter);
        Assertions.assertTrue(filteredRoles.containsAll(expectedRoleList));
        Assertions.assertEquals(2, filteredRoles.size());
    }

    @Test
    @DisplayName("Успешный поиск ролей по фильтру и с учётом порядка")
    public void findRolesWithFilterAndSorter() {
        Role role1 = new Role("admin132", "Administrator role", new HashSet<>());
        Role role2 = new Role("viewer22", "Regular user role", new HashSet<>());
        Role role3 = new Role("manager12", "Manager role", new HashSet<>());
        Role role4 = new Role("aasuperadmin2", "Super administrator role", new HashSet<>());

        role1.addPermission(new Permission("READ", "files", "descr"));
        role1.addPermission(new Permission("WRITE", "files", "descr"));
        role2.addPermission(new Permission("READ", "files", "descr"));
        role3.addPermission(new Permission("READ", "files", "descr"));
        role3.addPermission(new Permission("EXECUTE", "files", "descr"));
        role4.addPermission(new Permission("READ", "files", "descr"));
        role4.addPermission(new Permission("WRITE", "files", "descr"));
        role4.addPermission(new Permission("DELETE", "files", "descr"));

        RoleFilter filter = RoleFilters.hasPermission("WRITE", "files");
        Comparator<Role> sorter = RoleSorters.byName();
        List<Role> expectedRoleList = List.of(role4, role1);

        roleManager.add(role1);
        roleManager.add(role2);
        roleManager.add(role3);
        roleManager.add(role4);

        Assertions.assertEquals(expectedRoleList, roleManager.findAll(filter, sorter));
    }

    @Test
    @DisplayName("Успешная проверка существования роли по имени")
    public void checkExistingRoleByName() {
        Role role = new Role("administrator", "Administrator role", Collections.emptySet());

        roleManager.add(role);

        Assertions.assertTrue(roleManager.exists("administrator"));
        Assertions.assertTrue(roleManager.exists(" administrator "));
    }


    @Test
    @DisplayName("Успешное добавление разрешения к роли")
    public void addPermissionToRole() {
        Role role = new Role("mainadmin", "Administrator role", new HashSet<>());
        Permission permission = new Permission("WRITE", "FILE", "descr");

        roleManager.add(role);
        roleManager.addPermissionToRole("mainadmin", permission);

        Optional<Role> updatedRole = roleManager.findByName("mainadmin");

        Assertions.assertTrue(updatedRole.isPresent());
        Assertions.assertTrue(updatedRole.get().hasPermission(permission));
    }

    @Test
    @DisplayName("Добавление разрешения к несуществующей роли - должно выброситься IllegalArgumentException")
    public void addPermissionToNonExistingRole() {
        Permission permission = new Permission("WRITE", "FILE", "descr");

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> roleManager.addPermissionToRole("non", permission));
    }

    @Test
    @DisplayName("Успешное удаление разрешения из роли")
    public void removePermissionFromRole() {
        Role role = new Role("mainadmin1", "Administrator role", new HashSet<>());
        Permission permission = new Permission("WRITE", "FILE", "descr");

        roleManager.add(role);
        roleManager.addPermissionToRole("mainadmin1", permission);

        Optional<Role> updatedRole = roleManager.findByName("mainadmin1");

        Assertions.assertTrue(updatedRole.isPresent());

        roleManager.removePermissionFromRole("mainadmin1", permission);

        Assertions.assertFalse(updatedRole.get().hasPermission(permission));
    }

    @Test
    @DisplayName("Удаление разрешения из несуществующей роли - должно выброситься IllegalArgumentException")
    public void removePermissionFromNonExistingRole() {
        Permission permission = new Permission("WRITE", "FILE", "DESCR");

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> roleManager.removePermissionFromRole("NON", permission));
    }

    @Test
    @DisplayName("Успешный поиск ролей по разрешению")
    public void findRolesWithPermission() {
        Role role1 = new Role("NEWADMIN", "Administrator role", new HashSet<>());
        Role role2 = new Role("NEWUSER", "Regular user role", new HashSet<>());
        Role role3 = new Role("NEWMANAGER", "Manager role", new HashSet<>());

        role1.addPermission(new Permission("READ", "FILE", "descr"));
        role1.addPermission(new Permission("WRITE", "FILE", "descr"));
        role2.addPermission(new Permission("READ", "FILE", "descr"));
        role3.addPermission(new Permission("READ", "FILE", "descr"));
        role3.addPermission(new Permission("WRITE", "FILE", "descr"));

        roleManager.add(role1);
        roleManager.add(role2);
        roleManager.add(role3);

        List<Role> rolesWithWritePermission = roleManager.findRolesWithPermission("WRITE", "FILE");

        Assertions.assertEquals(2, rolesWithWritePermission.size());
        Assertions.assertTrue(rolesWithWritePermission.contains(role1));
        Assertions.assertTrue(rolesWithWritePermission.contains(role3));
    }
}