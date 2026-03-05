package com.example.command;

import com.example.manager.UserManager;
import com.example.manager.RoleManager;
import com.example.manager.AssignmentManager;
import com.example.model.*;
import com.example.filter.AssignmentFilter;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentCommandsTest {

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
    private Permission testPermission;

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
        testPermission = new Permission("READ", "users", "Can read users");
        testRole = createTestRole("UserRole", "Test role", Set.of(testPermission));
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
    @DisplayName("assign-role command")
    class AssignRoleCommandTests {

        @Test
        @DisplayName("Назначение постоянной роли пользователю")
        void shouldAssignPermanentRole() {
            when(scanner.nextLine())
                    .thenReturn("john")           // username
                    .thenReturn("UserRole")        // role name
                    .thenReturn("permanent")        // assignment type
                    .thenReturn("Test assignment"); // reason

            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(roleManager.findByName("UserRole")).thenReturn(Optional.of(testRole));
            when(roleManager.findAll()).thenReturn(List.of(testRole));

            parser.parseAndExecute("assign-role", scanner, system);

            verify(assignmentManager, times(1)).add(any(PermanentAssignment.class));
            String output = outContent.toString();
            assertTrue(output.contains("Role has been successfully assigned for user"));
        }

        @Test
        @DisplayName("Назначение временной роли пользователю")
        void shouldAssignTemporaryRole() {
            when(scanner.nextLine())
                    .thenReturn("john")                    // username
                    .thenReturn("UserRole")                 // role name
                    .thenReturn("temporary")                 // assignment type
                    .thenReturn("2025-12-31 23:59:59 +00:00") // expiration date
                    .thenReturn("Test temporary assignment"); // reason

            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(roleManager.findByName("UserRole")).thenReturn(Optional.of(testRole));
            when(roleManager.findAll()).thenReturn(List.of(testRole));

            parser.parseAndExecute("assign-role", scanner, system);

            verify(assignmentManager, times(1)).add(any(TemporaryAssignment.class));
            String output = outContent.toString();
            assertTrue(output.contains("Role has been successfully assigned for user"));
        }

        @Test
        @DisplayName("Обработка отсутствия пользователя")
        void shouldShowErrorWhenUserNotFound() {
            when(scanner.nextLine())
                    .thenReturn("unknown")
                    .thenReturn("UserRole");

            when(userManager.findByUsername("unknown")).thenReturn(Optional.empty());

            parser.parseAndExecute("assign-role", scanner, system);

            verify(assignmentManager, never()).add(any());
            String output = outContent.toString();
            assertTrue(output.contains("User with username 'unknown' not found"));
        }

        @Test
        @DisplayName("Обработка отсутствия роли")
        void shouldShowErrorWhenRoleNotFound() {
            when(scanner.nextLine())
                    .thenReturn("john")
                    .thenReturn("UnknownRole");

            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(roleManager.findAll()).thenReturn(List.of(testRole));

            parser.parseAndExecute("assign-role", scanner, system);

            verify(assignmentManager, never()).add(any());
            String output = outContent.toString();
            assertTrue(output.contains("Cancelling"));
        }

        @Test
        @DisplayName("Обработка ошибки назначения")
        void shouldHandleAssignmentErrors() {
            when(scanner.nextLine())
                    .thenReturn("john")
                    .thenReturn("UserRole")
                    .thenReturn("permanent")
                    .thenReturn("Test assignment");

            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(roleManager.findByName("UserRole")).thenReturn(Optional.of(testRole));
            when(roleManager.findAll()).thenReturn(List.of(testRole));

            doThrow(new IllegalStateException("User already has active assignment"))
                    .when(assignmentManager).add(any(RoleAssignment.class));

            parser.parseAndExecute("assign-role", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Error assigning role for user"));
        }
    }

    @Nested
    @DisplayName("revoke-role command")
    class RevokeRoleCommandTests {

        @Test
        @DisplayName("Отозвать роль")
        void shouldRevokeRole() {
            String assignmentId = "assignment_123";
            AssignmentMetadata metadata = AssignmentMetadata.now("testadmin", "test");
            RoleAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);

            when(scanner.nextLine())
                    .thenReturn("john")           // username
                    .thenReturn(assignmentId);     // assignment id

            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(assignmentManager.findByUser(testUser)).thenReturn(List.of(assignment));

            parser.parseAndExecute("revoke-role", scanner, system);

            verify(assignmentManager, times(1)).revokeAssignment(assignmentId);
            String output = outContent.toString();
            assertTrue(output.contains("Role has been successfully revoked from user"));
        }

        @Test
        @DisplayName("Отсутствие активных назначений")
        void shouldShowErrorWhenNoActiveAssignments() {
            when(scanner.nextLine())
                    .thenReturn("john");

            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(assignmentManager.findByUser(testUser)).thenReturn(List.of());

            parser.parseAndExecute("revoke-role", scanner, system);
            String output = outContent.toString();

            verify(assignmentManager, never()).revokeAssignment(anyString());
            assertTrue(output.contains("No active assignments for this user"));
        }

        @Test
        @DisplayName("Обработка ошибки отзыва назначения")
        void shouldHandleRevokeErrors() {
            String assignmentId = "assignment_123";
            AssignmentMetadata metadata = AssignmentMetadata.now("testadmin", "test");
            RoleAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);

            when(scanner.nextLine())
                    .thenReturn("john")
                    .thenReturn(assignmentId);

            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(assignmentManager.findByUser(testUser)).thenReturn(List.of(assignment));

            doThrow(new IllegalArgumentException("Assignment not found"))
                    .when(assignmentManager).revokeAssignment(assignmentId);

            parser.parseAndExecute("revoke-role", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Error revoking role from user"));
        }
    }

    @Nested
    @DisplayName("assignment-list command")
    class AssignmentListCommandTests {

        @Test
        @DisplayName("Отображение всех назначений")
        void shouldShowAllAssignments() {
            AssignmentMetadata metadata = AssignmentMetadata.now("testadmin", "test");
            RoleAssignment assignment1 = new PermanentAssignment(testUser, testRole, metadata);

            User user2 = User.validate("jane", "Jane Doe", "jane@test.com");
            Role role2 = createTestRole("Manager", "Manager role", new HashSet<>());
            RoleAssignment assignment2 = new TemporaryAssignment(user2, role2, metadata,
                    "2025-12-31 23:59:59 +00:00", false);

            when(assignmentManager.findAll()).thenReturn(List.of(assignment1, assignment2));

            parser.parseAndExecute("assignment-list", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("john"));
            assertTrue(output.contains("UserRole"));
            assertTrue(output.contains("PERMANENT"));
            assertTrue(output.contains("jane"));
            assertTrue(output.contains("Manager"));
            assertTrue(output.contains("TEMPORARY"));
        }

        @Test
        @DisplayName("Обработка отсутствия назначений")
        void shouldShowMissingWhenNoAssignments() {
            when(assignmentManager.findAll()).thenReturn(List.of());

            parser.parseAndExecute("assignment-list", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("missing"));
        }
    }

    @Nested
    @DisplayName("assignment-list-user command")
    class AssignmentListUserCommandTests {

        @Test
        @DisplayName("Отображение назначений по пользователю")
        void shouldShowUserAssignments() {
            when(scanner.nextLine()).thenReturn("john");

            AssignmentMetadata metadata = AssignmentMetadata.now("testadmin", "test");
            RoleAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);

            when(userManager.findByUsername("john")).thenReturn(Optional.of(testUser));
            when(assignmentManager.findByUser(testUser)).thenReturn(List.of(assignment));

            parser.parseAndExecute("assignment-list-user", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Assignment ID"));
            assertTrue(output.contains(testUser.username()));
            assertTrue(output.contains(testRole.getName()));
        }

        @Test
        @DisplayName("Обработка отсутствия пользователя")
        void shouldShowErrorWhenUserNotFound() {
            when(scanner.nextLine()).thenReturn("unknown");
            when(userManager.findByUsername("unknown")).thenReturn(Optional.empty());

            parser.parseAndExecute("assignment-list-user", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("User with username 'unknown' not found"));
        }
    }

    @Nested
    @DisplayName("assignment-list-role command")
    class AssignmentListRoleCommandTests {

        @Test
        @DisplayName("Отображение назначений по роли")
        void shouldShowRoleAssignments() {
            when(scanner.nextLine()).thenReturn("UserRole");

            AssignmentMetadata metadata = AssignmentMetadata.now("testadmin", "test");
            RoleAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);

            when(roleManager.findByName("UserRole")).thenReturn(Optional.of(testRole));
            when(assignmentManager.findByRole(testRole)).thenReturn(List.of(assignment));

            parser.parseAndExecute("assignment-list-role", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("This role assigned next users"));
            assertTrue(output.contains(testUser.format()));
        }

        @Test
        @DisplayName("Обработка отсутствия роли")
        void shouldShowErrorWhenRoleNotFound() {
            when(scanner.nextLine()).thenReturn("unknown");
            when(roleManager.findByName("unknown")).thenReturn(Optional.empty());

            parser.parseAndExecute("assignment-list-role", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Role with name 'unknown' not found"));
        }
    }

    @Nested
    @DisplayName("assignment-active command")
    class AssignmentActiveCommandTests {

        @Test
        @DisplayName("Отображение всех активных назначений")
        void shouldShowActiveAssignments() {
            AssignmentMetadata metadata = AssignmentMetadata.now("testadmin", "test");
            RoleAssignment activeAssignment = new PermanentAssignment(testUser, testRole, metadata);

            when(assignmentManager.getActiveAssignments()).thenReturn(List.of(activeAssignment));

            parser.parseAndExecute("assignment-active", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("ACTIVE"));
            assertTrue(output.contains(testUser.username()));
        }

        @Test
        @DisplayName("Обработка отсутствия активных назначений")
        void shouldShowMissingWhenNoActiveAssignments() {
            when(assignmentManager.getActiveAssignments()).thenReturn(List.of());

            parser.parseAndExecute("assignment-active", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("missing"));
        }
    }

    @Nested
    @DisplayName("assignment-expired command")
    class AssignmentExpiredCommandTests {

        @Test
        @DisplayName("Отображение просроченных временных назначений")
        void shouldShowExpiredAssignments() {
            AssignmentMetadata metadata = AssignmentMetadata.now("testadmin", "test");
            RoleAssignment expiredAssignment = new TemporaryAssignment(testUser, testRole, metadata,
                    "2020-01-01 00:00:00 +00:00", false);

            when(assignmentManager.getExpiredAssignments()).thenReturn(List.of(expiredAssignment));

            parser.parseAndExecute("assignment-expired", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("TEMPORARY"));
            assertTrue(output.contains(testUser.username()));
        }

        @Test
        @DisplayName("Обработка отсутствия просроченных временных назначений")
        void shouldShowMissingWhenNoExpiredAssignments() {
            when(assignmentManager.getExpiredAssignments()).thenReturn(List.of());

            parser.parseAndExecute("assignment-expired", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("missing"));
        }
    }

    @Nested
    @DisplayName("assignment-extend command")
    class AssignmentExtendCommandTests {

        @Test
        @DisplayName("Продление временного назначения")
        void shouldExtendTemporaryAssignment() {
            String assignmentId = "assignment_123";
            AssignmentMetadata metadata = AssignmentMetadata.now("testadmin", "test");
            RoleAssignment assignment = new TemporaryAssignment(testUser, testRole, metadata,
                    "2025-01-01 00:00:00 +00:00", false);

            when(assignmentManager.findByFilter(any(AssignmentFilter.class)))
                    .thenReturn(List.of(assignment));

            when(scanner.nextLine())
                    .thenReturn(assignmentId)
                    .thenReturn("2026-01-01 00:00:00 +00:00");

            parser.parseAndExecute("assignment-extend", scanner, system);

            verify(assignmentManager, times(1)).extendTemporaryAssignment(eq(assignmentId), anyString());
            String output = outContent.toString();
            assertTrue(output.contains("Temporary assignment has been successfully extended"));
        }

        @Test
        @DisplayName("Обработка отсутствия временного назначения")
        void shouldShowErrorWhenNoTemporaryAssignments() {
            when(assignmentManager.findByFilter(any(AssignmentFilter.class)))
                    .thenReturn(List.of());

            parser.parseAndExecute("assignment-extend", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("missing"));
            verify(assignmentManager, never()).extendTemporaryAssignment(any(), any());
        }

        @Test
        @DisplayName("Обработка ошибки продления")
        void shouldHandleExtensionErrors() {
            String assignmentId = "assignment_123";
            AssignmentMetadata metadata = AssignmentMetadata.now("testadmin", "test");
            RoleAssignment assignment = new TemporaryAssignment(testUser, testRole, metadata,
                    "2025-01-01 00:00:00 +00:00", false);

            when(assignmentManager.findByFilter(any(AssignmentFilter.class)))
                    .thenReturn(List.of(assignment));

            when(scanner.nextLine())
                    .thenReturn(assignmentId)
                    .thenReturn("invalid-date");

            doThrow(new IllegalArgumentException("Invalid expiration date"))
                    .when(assignmentManager).extendTemporaryAssignment(eq(assignmentId), anyString());

            parser.parseAndExecute("assignment-extend", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Error extending temporary assignment"));
        }
    }
}