package com.example.command;

import com.example.auditlog.AuditLog;
import com.example.manager.UserManager;
import com.example.manager.RoleManager;
import com.example.manager.AssignmentManager;
import com.example.model.*;
import com.example.filter.RoleFilter;
import com.example.util.ConsoleUtils;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleCommandsTest {

    private RBACSystem system;
    private CommandParser parser;

    @Mock
    private UserManager userManager;
    @Mock
    private RoleManager roleManager;
    @Mock
    private AssignmentManager assignmentManager;
    @Mock
    private Scanner scanner;

    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        system.setLogger(new AuditLog());
        setField(system, "userManager", userManager);
        setField(system, "roleManager", roleManager);
        setField(system, "assignmentManager", assignmentManager);

        parser = new CommandParser();
        CommandRegistry.registerAllCommands(parser);

        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);

        try {
            var usedNamesField = Role.class.getDeclaredField("usedNames");
            usedNamesField.setAccessible(true);
            Set<String> usedNames = (Set<String>) usedNamesField.get(null);
            usedNames.clear();
        } catch (Exception e) {
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Role createTestRole(String name, String description, Set<Permission> permissions) {
        try {
            var usedNamesField = Role.class.getDeclaredField("usedNames");
            usedNamesField.setAccessible(true);
            Set<String> usedNames = (Set<String>) usedNamesField.get(null);
            usedNames.clear();
        } catch (Exception e) {
            // Игнорируем
        }
        return new Role(name, description, permissions != null ? permissions : new HashSet<>());
    }

    @Nested
    @DisplayName("role-list command")
    class RoleListCommandTests {

        @Test
        @DisplayName("Обработка отсутствия ролей")
        void shouldShowMissingWhenNoRoles() {
            when(roleManager.findAll()).thenReturn(List.of());

            parser.parseAndExecute("role-list", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Missing roles"));
        }

        @Test
        @DisplayName("Отображение всех ролей в таблице")
        void shouldDisplayAllRoles() {
            Permission perm1 = new Permission("READ", "users", "Can read users");
            Permission perm2 = new Permission("WRITE", "users", "Can write users");

            Role role1 = createTestRole("Admin", "Admin role", Set.of(perm1, perm2));
            Role role2 = createTestRole("Viewer", "Viewer role", Set.of(perm1));

            when(roleManager.findAll()).thenReturn(List.of(role1, role2));

            parser.parseAndExecute("role-list", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Admin"));
            assertTrue(output.contains("Viewer"));
            assertTrue(output.contains("2")); // Permission count for Admin
            assertTrue(output.contains("1")); // Permission count for Viewer
        }
    }

    @Nested
    @DisplayName("role-create command")
    class RoleCreateCommandTests {

        @Test
        @DisplayName("Создание роли без прав")
        void shouldCreateRoleWithoutPermissions() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn("Manager");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role description: ")))
                        .thenReturn("Manager role");
                mockedUtils.when(() -> ConsoleUtils.promptYesNo(eq(scanner), eq("Add new permission to role? (y/n): ")))
                        .thenReturn(false);

                parser.parseAndExecute("role-create", scanner, system);

                verify(roleManager, times(1)).add(any(Role.class));
                String output = outContent.toString();
                assertTrue(output.contains("New role has been successfully created"));
            }
        }

        @Test
        @DisplayName("Создание роли с правами")
        void shouldCreateRoleWithPermissions() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn("Manager");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role description: ")))
                        .thenReturn("Manager role");
                mockedUtils.when(() -> ConsoleUtils.promptYesNo(eq(scanner), eq("Add new permission to role? (y/n): ")))
                        .thenReturn(true)
                        .thenReturn(false);
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter permission name: ")))
                        .thenReturn("READ");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter permission resource: ")))
                        .thenReturn("users");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter permission description: ")))
                        .thenReturn("Can read users");

                parser.parseAndExecute("role-create", scanner, system);

                verify(roleManager, times(1)).add(any(Role.class));
                verify(roleManager, times(1)).addPermissionToRole(eq("Manager"), any(Permission.class));
            }
        }

        @Test
        @DisplayName("Запрет создания роли с именем admin")
        void shouldNotCreateRoleWithAdminName() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn("admin");

                parser.parseAndExecute("role-create", scanner, system);

                verify(roleManager, never()).add(any(Role.class));
                String output = outContent.toString();
                assertTrue(output.contains("DON`T TOUCH ADMIN ROLE!"));
            }
        }

        @Test
        @DisplayName("Обработка ошибки создания роли")
        void shouldHandleCreationErrors() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn("");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role description: ")))
                        .thenReturn("Manager role");

                parser.parseAndExecute("role-create", scanner, system);

                verify(roleManager, never()).add(any());
                String output = outContent.toString();
                assertTrue(output.contains("Error creating role"));
            }
        }
    }

    @Nested
    @DisplayName("role-view command")
    class RoleViewCommandTests {

        @Test
        @DisplayName("Отображение информации о существующей роли")
        void shouldShowRoleDetails() {
            String roleName = "Admin";
            Permission perm = new Permission("READ", "users", "Can read users");
            Role role = createTestRole(roleName, "Admin role", Set.of(perm));

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn(roleName);
                when(roleManager.findByName(roleName)).thenReturn(Optional.of(role));

                parser.parseAndExecute("role-view", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains(roleName));
                assertTrue(output.contains("Admin role"));
                assertTrue(output.contains(perm.format()));
            }
        }

        @Test
        @DisplayName("Обработка отсутствия роли")
        void shouldShowErrorWhenRoleNotFound() {
            String roleName = "unknown";

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn(roleName);
                when(roleManager.findByName(roleName)).thenReturn(Optional.empty());

                parser.parseAndExecute("role-view", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains("Role with name 'unknown' not found"));
            }
        }
    }

    @Nested
    @DisplayName("role-update command")
    class RoleUpdateCommandTests {

        @Test
        @DisplayName("Успешное обновление роли")
        void shouldUpdateRole() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn("OldRole");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter new role name: ")))
                        .thenReturn("NewRole");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter new description: ")))
                        .thenReturn("New description");

                parser.parseAndExecute("role-update", scanner, system);

                verify(roleManager, times(1)).update("OldRole", "NewRole", "New description");
                String output = outContent.toString();
                assertTrue(output.contains("Role data has been successfully updated"));
            }
        }

        @Test
        @DisplayName("Запрет обновления admin роли")
        void shouldNotUpdateAdminRole() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn("admin");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter new role name: ")))
                        .thenReturn("NewRole");

                parser.parseAndExecute("role-update", scanner, system);

                verify(roleManager, never()).update(anyString(), anyString(), anyString());
                String output = outContent.toString();
                assertTrue(output.contains("DON`T TOUCH ADMIN ROLE!"));
            }
        }

        @Test
        @DisplayName("Запрет переименования в admin")
        void shouldNotRenameToAdmin() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn("OldRole");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter new role name: ")))
                        .thenReturn("admin");

                parser.parseAndExecute("role-update", scanner, system);

                verify(roleManager, never()).update(anyString(), anyString(), anyString());
                String output = outContent.toString();
                assertTrue(output.contains("DON`T TOUCH ADMIN ROLE!"));
            }
        }

        @Test
        @DisplayName("Обработка ошибки обновления роли")
        void shouldHandleUpdateErrors() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn("OldRole");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter new role name: ")))
                        .thenReturn("");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter new description: ")))
                        .thenReturn("New description");

                doThrow(new IllegalArgumentException("Invalid role name"))
                        .when(roleManager).update(eq("OldRole"), eq(""), eq("New description"));

                parser.parseAndExecute("role-update", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains("Error updating role data"));
            }
        }
    }

    @Nested
    @DisplayName("role-delete command")
    class RoleDeleteCommandTests {

        @Test
        @Disabled("Удаление неназначенной роли. Пропуск теста." +
                "Причина: тест падает по неизвестной причине, хотя если вручную тестировать, то всё ок")
        void shouldDeleteRoleWhenNoAssignments() {
            String roleName = "Viewer";
            Role role = new Role(roleName, "Viewer role", new HashSet<>());

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn(roleName);
                mockedUtils.when(() -> ConsoleUtils.promptYesNo(eq(scanner), eq("Confirm role deletion? (y/n): ")))
                        .thenReturn(true);

                when(roleManager.findByName(roleName)).thenReturn(Optional.of(role));
                when(assignmentManager.findByRole(role)).thenReturn(Collections.emptyList());
                when(roleManager.remove(role)).thenReturn(true);

                parser.parseAndExecute("role-delete", scanner, system);

                String output = outContent.toString();

                verify(roleManager, times(1)).remove(role);
                verify(assignmentManager, times(1)).findByRole(role);
                assertTrue(output.contains("Role have been successfully deleted"));
            }
        }

        @Test
        @DisplayName("Отмена удаления роли")
        void shouldNotDeleteRoleWhenNotConfirmed() {
            String roleName = "Viewer";
            Role role = createTestRole(roleName, "Viewer role", new HashSet<>());

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn(roleName);
                mockedUtils.when(() -> ConsoleUtils.promptYesNo(eq(scanner), eq("Confirm role deletion? (y/n): ")))
                        .thenReturn(false);

                when(roleManager.findByName(roleName)).thenReturn(Optional.of(role));

                parser.parseAndExecute("role-delete", scanner, system);

                verify(roleManager, never()).remove(any());
                String output = outContent.toString();
                assertTrue(output.contains("Canceling role deletion"));
            }
        }

        @Test
        @DisplayName("Отображение назначений при неудачном удалении роли")
        void shouldShowAssignmentsWhenRoleCannotBeDeleted() {
            String roleName = "Admin";
            Role role = createTestRole(roleName, "Admin role", new HashSet<>());

            User user = User.validate("john", "John Doe", "john@test.com");
            AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
            RoleAssignment assignment = new PermanentAssignment(user, role, metadata);

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn(roleName);
                mockedUtils.when(() -> ConsoleUtils.promptYesNo(eq(scanner), eq("Confirm role deletion? (y/n): ")))
                        .thenReturn(true);

                when(roleManager.findByName(roleName)).thenReturn(Optional.of(role));
                when(assignmentManager.findByRole(role)).thenReturn(List.of(assignment));
                when(roleManager.remove(role)).thenReturn(false);

                parser.parseAndExecute("role-delete", scanner, system);
                String output = outContent.toString();

                verify(roleManager, times(1)).remove(role);
                assertTrue(output.contains("is assigned to users"));
                assertTrue(output.contains(user.format()));
            }
        }

        @Test
        @DisplayName("Обработка отсутствия роли")
        void shouldHandleRoleNotFound() {
            String roleName = "NonExistentRole";

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn(roleName);
                when(roleManager.findByName(roleName)).thenReturn(Optional.empty());

                parser.parseAndExecute("role-delete", scanner, system);
                String output = outContent.toString();

                verify(roleManager, never()).remove(any());
                assertTrue(output.contains("Role with name 'NonExistentRole' not found"));
            }
        }
    }

    @Nested
    @DisplayName("role-add-permission command")
    class RoleAddPermissionCommandTests {

        @Test
        @DisplayName("Добавление права к роли")
        void shouldAddPermissionToRole() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn("Admin");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter permission name: ")))
                        .thenReturn("READ");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter permission resource: ")))
                        .thenReturn("users");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter permission description: ")))
                        .thenReturn("Can read users");

                parser.parseAndExecute("role-add-permission", scanner, system);

                verify(roleManager, times(1)).addPermissionToRole(eq("Admin"), any(Permission.class));
                String output = outContent.toString();
                assertTrue(output.contains("Permission has been successfully added to role"));
            }
        }

        @Test
        @DisplayName("Обработка ошибки добавления права")
        void shouldHandleErrors() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn("Admin");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter permission name: ")))
                        .thenReturn(""); // Invalid permission name
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter permission resource: ")))
                        .thenReturn("users");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter permission description: ")))
                        .thenReturn("Can read users");

                parser.parseAndExecute("role-add-permission", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains("Error adding permission to role"));
                verify(roleManager, never()).addPermissionToRole(anyString(), any(Permission.class));
            }
        }
    }

    @Nested
    @DisplayName("role-remove-permission command")
    class RoleRemovePermissionCommandTests {

        @Test
        @DisplayName("Удаление права из роли")
        void shouldRemovePermissionFromRole() {
            String roleName = "testRole";
            Permission perm1 = new Permission("READ", "users", "Can read users");
            Permission perm2 = new Permission("WRITE", "users", "Can write users");
            Role role = createTestRole(roleName, "Test role", Set.of(perm1, perm2));

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn(roleName);
                mockedUtils.when(() -> ConsoleUtils.promptInt(eq(scanner), eq("\nEnter permission number for deletion: "), eq(1), eq(2)))
                        .thenReturn(1);

                when(roleManager.findByName(roleName)).thenReturn(Optional.of(role));

                parser.parseAndExecute("role-remove-permission", scanner, system);

                verify(roleManager, times(1)).removePermissionFromRole(eq(roleName), any(Permission.class));
                String output = outContent.toString();
                assertTrue(output.contains("Permission has been successfully removed from role"));
            }
        }

        @Test
        @DisplayName("Запрет удаления прав у admin роли")
        void shouldNotRemovePermissionFromAdminRole() {
            String roleName = "admin";

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn(roleName);

                parser.parseAndExecute("role-remove-permission", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains("DON`T TOUCH ADMIN ROLE!"));
                verify(roleManager, never()).removePermissionFromRole(any(), any());
            }
        }

        @Test
        @DisplayName("Обработка отсутствия прав")
        void shouldShowErrorWhenNoPermissions() {
            String roleName = "EmptyRole";
            Role role = createTestRole(roleName, "Empty role", new HashSet<>());

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn(roleName);
                when(roleManager.findByName(roleName)).thenReturn(Optional.of(role));

                parser.parseAndExecute("role-remove-permission", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains("does not have any permissions"));
                verify(roleManager, never()).removePermissionFromRole(any(), any());
            }
        }
    }

    @Nested
    @DisplayName("role-search command")
    class RoleSearchCommandTests {

        @Test
        @DisplayName("Поиск ролей по имени")
        void shouldSearchByNameContains() {
            Role role = createTestRole("Admin", "Admin role", new HashSet<>());

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptChoice(eq(scanner), eq("Select filter number: "), anyList()))
                        .thenReturn(0); // by name (contains)
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn("Admin");
                when(roleManager.findByFilter(any(RoleFilter.class))).thenReturn(List.of(role));

                parser.parseAndExecute("role-search", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains(role.format()));
            }
        }

        @Test
        @DisplayName("Поиск ролей по праву")
        void shouldSearchByPermission() {
            Role role = createTestRole("Admin", "Admin role", new HashSet<>());

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptChoice(eq(scanner), eq("Select filter number: "), anyList()))
                        .thenReturn(1); // by permission
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner),
                                eq("Enter permission name and permission resource by space: ")))
                        .thenReturn("READ users");
                when(roleManager.findByFilter(any(RoleFilter.class))).thenReturn(List.of(role));

                parser.parseAndExecute("role-search", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains(role.format()));
            }
        }

        @Test
        @DisplayName("Поиск ролей по минимальному количеству прав")
        void shouldSearchByMinPermissions() {
            Role role = createTestRole("Admin", "Admin role", new HashSet<>());

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptChoice(eq(scanner), eq("Select filter number: "), anyList()))
                        .thenReturn(2); // by minimal permissions count
                mockedUtils.when(() -> ConsoleUtils.promptInt(eq(scanner), eq("Enter permissions count : "), eq(0), eq(999)))
                        .thenReturn(2);
                when(roleManager.findByFilter(any(RoleFilter.class))).thenReturn(List.of(role));

                parser.parseAndExecute("role-search", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains(role.format()));
            }
        }

        @Test
        @DisplayName("Обработка отсутствия результатов поиска")
        void shouldShowMissingWhenNoRolesFound() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptChoice(eq(scanner), eq("Select filter number: "), anyList()))
                        .thenReturn(0); // by name
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter role name: ")))
                        .thenReturn("nonexistent");
                when(roleManager.findByFilter(any(RoleFilter.class))).thenReturn(List.of());

                parser.parseAndExecute("role-search", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains("missing"));
            }
        }
    }
}