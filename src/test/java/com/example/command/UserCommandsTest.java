package com.example.command;

import com.example.auditlog.AuditLog;
import com.example.manager.UserManager;
import com.example.manager.RoleManager;
import com.example.manager.AssignmentManager;
import com.example.model.User;
import com.example.model.Role;
import com.example.model.RoleAssignment;
import com.example.model.PermanentAssignment;
import com.example.model.AssignmentMetadata;
import com.example.util.ConsoleUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCommandsTest {

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

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("user-list command")
    class UserListCommandTests {

        @Test
        @DisplayName("Вывод сообщения если пользователи не существуют")
        void shouldShowMissingWhenNoUsers() {
            when(userManager.findAll()).thenReturn(List.of());

            parser.parseAndExecute("user-list", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Missing users"));
        }

        @Test
        @DisplayName("Вывод всех пользователей в таблице")
        void shouldDisplayAllUsers() {
            User user1 = User.validate("john", "John Doe", "john@test.com");
            User user2 = User.validate("jane", "Jane Smith", "jane@test.com");
            when(userManager.findAll()).thenReturn(List.of(user1, user2));

            parser.parseAndExecute("user-list", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("john"));
            assertTrue(output.contains("John Doe"));
            assertTrue(output.contains("john@test.com"));
            assertTrue(output.contains("jane"));
            assertTrue(output.contains("Jane Smith"));
            assertTrue(output.contains("jane@test.com"));
            assertTrue(output.contains("Username"));
            assertTrue(output.contains("Full name"));
            assertTrue(output.contains("Email"));
        }
    }

    @Nested
    @DisplayName("user-create command")
    class UserCreateCommandTests {

        @Test
        @DisplayName("Успешное создание пользователя")
        void shouldCreateUser() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter username: ")))
                        .thenReturn("newuser");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter fullName: ")))
                        .thenReturn("New User");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter email: ")))
                        .thenReturn("new@test.com");

                parser.parseAndExecute("user-create", scanner, system);

                verify(userManager, times(1)).add(any(User.class));
                String output = outContent.toString();
                assertTrue(output.contains("New user has been successfully created"));
            }
        }

        @Test
        @DisplayName("Обработка ошибки валидации")
        void shouldHandleValidationErrors() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter username: ")))
                        .thenReturn("");  // Invalid username
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter fullName: ")))
                        .thenReturn("New User");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter email: ")))
                        .thenReturn("new@test.com");

                parser.parseAndExecute("user-create", scanner, system);

                verify(userManager, never()).add(any(User.class));
                String output = outContent.toString();
                assertTrue(output.contains("Error creating user"));
            }
        }
    }

    @Nested
    @DisplayName("user-view command")
    class UserViewCommandTests {

        @Test
        @DisplayName("Отображение информации о существующем пользователе")
        void shouldShowUserDetails() {
            String username = "john";
            User user = User.validate(username, "John Doe", "john@test.com");

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter username: ")))
                        .thenReturn(username);
                when(userManager.findByUsername(username)).thenReturn(Optional.of(user));
                when(assignmentManager.findByUser(user)).thenReturn(List.of());

                parser.parseAndExecute("user-view", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains(user.format()));
            }
        }

        @Test
        @DisplayName("Вывод ошибки, если пользователя не существует")
        void shouldShowErrorWhenUserNotFound() {
            String username = "unknown";

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter username: ")))
                        .thenReturn(username);
                when(userManager.findByUsername(username)).thenReturn(Optional.empty());

                parser.parseAndExecute("user-view", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains("User with username 'unknown' not found"));
            }
        }
    }

    @Nested
    @DisplayName("user-update command")
    class UserUpdateCommandTests {

        @Test
        @DisplayName("Успешное обновление пользователя")
        void shouldUpdateUser() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter username: ")))
                        .thenReturn("john");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter new fullName: ")))
                        .thenReturn("John Updated");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter new email: ")))
                        .thenReturn("john.updated@test.com");

                parser.parseAndExecute("user-update", scanner, system);

                verify(userManager, times(1)).update("john", "John Updated", "john.updated@test.com");
                String output = outContent.toString();
                assertTrue(output.contains("User data has been successfully updated"));
            }
        }

        @Test
        @DisplayName("Обработка ошибок при обновлении пользователя")
        void shouldHandleUpdateErrors() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter username: ")))
                        .thenReturn("john");
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter new fullName: ")))
                        .thenReturn("");  // Invalid full name
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter new email: ")))
                        .thenReturn("john@test.com");

                doThrow(new IllegalArgumentException("Invalid full name"))
                        .when(userManager).update(eq("john"), eq(""), eq("john@test.com"));

                parser.parseAndExecute("user-update", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains("Error updating user data"));
            }
        }
    }

    @Nested
    @DisplayName("user-delete command")
    class UserDeleteCommandTests {

        @Test
        @DisplayName("Успешное удаление пользователя с положительным подтверждением")
        void shouldDeleteUserWhenConfirmed() {
            String username = "john";
            User user = User.validate(username, "John Doe", "john@test.com");

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter username: ")))
                        .thenReturn(username);
                mockedUtils.when(() -> ConsoleUtils.promptYesNo(eq(scanner), eq("Confirm user deletion? (y/n): ")))
                        .thenReturn(true);

                when(userManager.findByUsername(username)).thenReturn(Optional.of(user));
                when(assignmentManager.findByUser(user)).thenReturn(List.of());

                parser.parseAndExecute("user-delete", scanner, system);

                verify(userManager, times(1)).remove(user);
                String output = outContent.toString();
                assertTrue(output.contains("User and his assignments have been successfully deleted"));
            }
        }

        @Test
        @DisplayName("Отмена удаления пользователя")
        void shouldNotDeleteUserWhenNotConfirmed() {
            String username = "john";

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter username: ")))
                        .thenReturn(username);
                mockedUtils.when(() -> ConsoleUtils.promptYesNo(eq(scanner), eq("Confirm user deletion? (y/n): ")))
                        .thenReturn(false);

                parser.parseAndExecute("user-delete", scanner, system);

                verify(userManager, never()).remove(any());
                verify(userManager, never()).findByUsername(any());
                String output = outContent.toString();
                assertTrue(output.contains("Canceling user deletion"));
            }
        }

        @Test
        @DisplayName("Запрет удаления пользователя с ролью Admin")
        void shouldNotDeleteAdminUser() {
            String username = "admin";
            User user = User.validate(username, "Admin User", "admin@test.com");
            Role adminRole = new Role("Admin", "Admin role", new HashSet<>());
            AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
            RoleAssignment assignment = new PermanentAssignment(user, adminRole, metadata);

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter username: ")))
                        .thenReturn(username);
                mockedUtils.when(() -> ConsoleUtils.promptYesNo(eq(scanner), eq("Confirm user deletion? (y/n): ")))
                        .thenReturn(true);

                when(userManager.findByUsername(username)).thenReturn(Optional.of(user));
                when(assignmentManager.findByUser(user)).thenReturn(List.of(assignment));

                parser.parseAndExecute("user-delete", scanner, system);

                verify(userManager, never()).remove(any());
                String output = outContent.toString();
                assertTrue(output.contains("is admin"));
            }
        }

        @Test
        @DisplayName("Обработка ошибок при несуществующем пользователе")
        void shouldShowErrorWhenUserNotFound() {
            String username = "unknown";

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter username: ")))
                        .thenReturn(username);
                mockedUtils.when(() -> ConsoleUtils.promptYesNo(eq(scanner), eq("Confirm user deletion? (y/n): ")))
                        .thenReturn(true);
                when(userManager.findByUsername(username)).thenReturn(Optional.empty());

                parser.parseAndExecute("user-delete", scanner, system);

                verify(userManager, never()).remove(any());
                String output = outContent.toString();
                assertTrue(output.contains("User with username 'unknown' not found"));
            }
        }
    }

    @Nested
    @DisplayName("user-search command")
    class UserSearchCommandTests {

        @Test
        @DisplayName("Успешный поиск пользователей по username")
        void shouldSearchByUsernameContains() {
            User user = User.validate("john123", "John Doe", "john@test.com");

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptChoice(eq(scanner), eq("Select filter number: "), anyList()))
                        .thenReturn(0); // by username (contains)
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter filter key: ")))
                        .thenReturn("john");

                when(userManager.findByFilter(any())).thenReturn(List.of(user));

                parser.parseAndExecute("user-search", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains(user.format()));
            }
        }

        @Test
        @DisplayName("Успешный поиск пользователей по email")
        void shouldSearchByEmail() {
            User user = User.validate("john", "John Doe", "john@test.com");

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptChoice(eq(scanner), eq("Select filter number: "), anyList()))
                        .thenReturn(1); // by email (contains)
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter filter key: ")))
                        .thenReturn("john@test.com");

                when(userManager.findByFilter(any())).thenReturn(List.of(user));

                parser.parseAndExecute("user-search", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains(user.format()));
            }
        }

        @Test
        @DisplayName("Успешный поиск пользователей по email domain")
        void shouldSearchByEmailDomain() {
            User user = User.validate("john", "John Doe", "john@test.com");

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptChoice(eq(scanner), eq("Select filter number: "), anyList()))
                        .thenReturn(2); // by email domain
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter filter key: ")))
                        .thenReturn("@test.com");

                when(userManager.findByFilter(any())).thenReturn(List.of(user));

                parser.parseAndExecute("user-search", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains(user.format()));
            }
        }

        @Test
        @DisplayName("Успешный поиск пользователей по full name")
        void shouldSearchByFullNameContains() {
            User user = User.validate("john", "John Doe", "john@test.com");

            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptChoice(eq(scanner), eq("Select filter number: "), anyList()))
                        .thenReturn(3); // by full name (contains)
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter filter key: ")))
                        .thenReturn("Doe");

                when(userManager.findByFilter(any())).thenReturn(List.of(user));

                parser.parseAndExecute("user-search", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains(user.format()));
            }
        }

        @Test
        @DisplayName("Обработка случая при отсутствующих пользователях")
        void shouldShowMissingWhenNoUsersFound() {
            try (MockedStatic<ConsoleUtils> mockedUtils = mockStatic(ConsoleUtils.class)) {
                mockedUtils.when(() -> ConsoleUtils.promptChoice(eq(scanner), eq("Select filter number: "), anyList()))
                        .thenReturn(0); // by username
                mockedUtils.when(() -> ConsoleUtils.promptString(eq(scanner), eq("Enter filter key: ")))
                        .thenReturn("nonexistent");

                when(userManager.findByFilter(any())).thenReturn(List.of());

                parser.parseAndExecute("user-search", scanner, system);
                String output = outContent.toString();

                assertTrue(output.contains("missing"));
            }
        }
    }
}