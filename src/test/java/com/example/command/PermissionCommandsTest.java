package com.example.command;

import com.example.manager.UserManager;
import com.example.manager.RoleManager;
import com.example.manager.AssignmentManager;
import com.example.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionCommandsTest {

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

    private User testUser;
    private Role testRole;
    private Permission readPermission;
    private Permission writePermission;
    private AssignmentMetadata metadata;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        setField(system, "userManager", userManager);
        setField(system, "roleManager", roleManager);
        setField(system, "assignmentManager", assignmentManager);
        system.setCurrentUser("testadmin");

        parser = new CommandParser();
        CommandRegistry.registerAllCommands(parser);

        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        testUser = User.validate("john", "John Doe", "john@test.com");
        readPermission = new Permission("READ", "users", "Can read users");
        writePermission = new Permission("WRITE", "users", "Can write users");
        testRole = createTestRole("UserRole", "Test role", Set.of(readPermission, writePermission));
        metadata = AssignmentMetadata.now("testadmin", "test assignment");
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
        }
        return new Role(name, description, permissions != null ? permissions : new HashSet<>());
    }

    @Nested
    @DisplayName("permissions-user command")
    class PermissionsUserCommandTests {

        @Test
        @DisplayName("Отображение прав пользователей по ресурсам")
        void shouldShowUserPermissions() {
            when(scanner.nextLine()).thenReturn("john");

            RoleAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);

            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(assignmentManager.findByUser(testUser)).thenReturn(List.of(assignment));

            parser.parseAndExecute("permissions-user", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Resource: users"));
            assertTrue(output.contains(readPermission.format()));
            assertTrue(output.contains(writePermission.format()));
        }

        @Test
        @DisplayName("Обработка отсутствия роли у пользователя")
        void shouldShowMessageWhenNoRoles() {
            when(scanner.nextLine()).thenReturn("john");

            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(assignmentManager.findByUser(testUser)).thenReturn(List.of());

            parser.parseAndExecute("permissions-user", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("User with username 'john' does not have any roles"));
        }

        @Test
        @DisplayName("Обработка отсутствия у роли пользователя прав")
        void shouldShowMessageWhenNoPermissions() {
            Role emptyRole = createTestRole("EmptyRole", "Empty role", new HashSet<>());
            RoleAssignment assignment = new PermanentAssignment(testUser, emptyRole, metadata);

            when(scanner.nextLine()).thenReturn("john");
            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(assignmentManager.findByUser(testUser)).thenReturn(List.of(assignment));

            parser.parseAndExecute("permissions-user", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("User with username 'john' has no permissions"));
        }

        @Test
        @DisplayName("Обработка отсутствия пользователя")
        void shouldShowErrorWhenUserNotFound() {
            when(scanner.nextLine()).thenReturn("unknown");
            when(userManager.findByUsername("unknown")).thenReturn(Optional.empty());

            parser.parseAndExecute("permissions-user", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("User with username 'unknown' not found"));
        }
    }

    @Nested
    @DisplayName("permissions-check command")
    class PermissionsCheckCommandTests {

        @Test
        @DisplayName("Отображение ролей, которые имеют право и которые назначены пользователю")
        void shouldShowRolesWhenUserHasPermission() {
            when(scanner.nextLine())
                    .thenReturn("john")
                    .thenReturn("READ")
                    .thenReturn("users");

            RoleAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);

            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(assignmentManager.userHasPermission(testUser, "READ", "users")).thenReturn(true);
            when(assignmentManager.findByUser(testUser)).thenReturn(List.of(assignment));

            parser.parseAndExecute("permissions-check", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Roles that have this permission:"));
            assertTrue(output.contains("UserRole"));
        }

        @Test
        @DisplayName("Обработка случая пользователь не имеет право")
        void shouldShowMissingWhenUserDoesNotHavePermission() {
            when(scanner.nextLine())
                    .thenReturn("john")
                    .thenReturn("DELETE")
                    .thenReturn("users");

            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(assignmentManager.userHasPermission(testUser, "DELETE", "users")).thenReturn(false);

            parser.parseAndExecute("permissions-check", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("missing"));
        }

        @Test
        @DisplayName("Обработка отсутствия пользователя")
        void shouldShowErrorWhenUserNotFound() {
            when(scanner.nextLine())
                    .thenReturn("unknown")
                    .thenReturn("READ")
                    .thenReturn("users");

            when(userManager.findByUsername("unknown")).thenReturn(Optional.empty());

            parser.parseAndExecute("permissions-check", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("User with username 'unknown' not found"));
        }
    }
}