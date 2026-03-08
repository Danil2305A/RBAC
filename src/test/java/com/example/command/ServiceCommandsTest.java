package com.example.command;

import com.example.auditlog.AuditLog;
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
import java.util.Scanner;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceCommandsTest {
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
        system.setLogger(new AuditLog());

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
    @DisplayName("help command")
    class HelpCommandTests {

        @Test
        @DisplayName("Вывод всех доступных команд")
        void shouldShowAllCommands() {
            parser.parseAndExecute("help", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("user-list"));
            assertTrue(output.contains("user-create"));
            assertTrue(output.contains("role-list"));
            assertTrue(output.contains("role-create"));
            assertTrue(output.contains("assign-role"));
            assertTrue(output.contains("permissions-user"));
            assertTrue(output.contains("stats"));
            assertTrue(output.contains("exit"));
        }
    }

    @Nested
    @DisplayName("clear command")
    class ClearCommandTests {

        @Test
        @DisplayName("Очистка консоли")
        void shouldClearScreen() {
            parser.parseAndExecute("clear", scanner, system);
            String output = outContent.toString();

            assertEquals("\033[H\033[2J", output);
        }
    }

    @Nested
    @DisplayName("exit command")
    class ExitCommandTests {

        @Test
        @DisplayName("Выход из программы с положительным подтверждением")
        void shouldExitWhenConfirmed() {
            when(scanner.nextLine()).thenReturn("y");

            System systemMock = mock(System.class);

            assertThrows(SystemExitException.class, () -> {
                try {
                    parser.parseAndExecute("exit", scanner, system);
                } catch (SystemExitException e) {
                    verify(scanner, times(1)).close();
                    throw e;
                }
            });
        }

        @Test
        @DisplayName("Выход из программы с отрицательным подтверждением")
        void shouldNotExitWhenNotConfirmed() {
            when(scanner.nextLine()).thenReturn("n");

            parser.parseAndExecute("exit", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Cancelling exiting the program"));
            verify(scanner, never()).close();
        }
    }

    @Nested
    @DisplayName("save command")
    class SaveCommandTests {

        @Test
        @DisplayName("Успешное сохранение данных в JSON")
        void shouldSaveDataToJson() {
            when(userManager.findAll()).thenReturn(List.of(testUser));
            when(roleManager.findAll()).thenReturn(List.of(testRole));

            RoleAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);
            when(assignmentManager.findAll()).thenReturn(List.of(assignment));

            parser.parseAndExecute("save", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Data has been successfully saved to rbac-data/data.json"));
        }
    }

    static class SystemExitException extends SecurityException {
        public SystemExitException() {
            super("System.exit() called");
        }
    }
}