package com.example.command;

import com.example.manager.UserManager;
import com.example.manager.RoleManager;
import com.example.manager.AssignmentManager;
import com.example.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
            when(scanner.nextLine())
                    .thenReturn("newuser")
                    .thenReturn("New User")
                    .thenReturn("new@test.com");

            parser.parseAndExecute("user-create", scanner, system);

            verify(userManager, times(1)).add(any(User.class));
            String output = outContent.toString();
            assertTrue(output.contains("New user has been successfully created"));
        }

        @Test
        @DisplayName("Обработка ошибки валидации")
        void shouldHandleValidationErrors() {
            when(scanner.nextLine())
                    .thenReturn("")  // Invalid username
                    .thenReturn("New User")
                    .thenReturn("new@test.com");

            parser.parseAndExecute("user-create", scanner, system);

            verify(userManager, never()).add(any(User.class));
            String output = outContent.toString();
            assertTrue(output.contains("Error creating user"));
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

            when(scanner.nextLine()).thenReturn(username);
            when(userManager.findByUsername(username)).thenReturn(Optional.of(user));
            when(assignmentManager.findByUser(user)).thenReturn(List.of());

            parser.parseAndExecute("user-view", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains(user.format()));
        }

        @Test
        @DisplayName("Вывод ошибки, если пользователя не существует")
        void shouldShowErrorWhenUserNotFound() {
            String username = "unknown";
            when(scanner.nextLine()).thenReturn(username);
            when(userManager.findByUsername(username)).thenReturn(Optional.empty());

            parser.parseAndExecute("user-view", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("User with username 'unknown' not found"));
        }
    }

    @Nested
    @DisplayName("user-update command")
    class UserUpdateCommandTests {

        @Test
        @DisplayName("Успешное обновление пользователя")
        void shouldUpdateUser() {
            when(scanner.nextLine())
                    .thenReturn("john")
                    .thenReturn("John Updated")
                    .thenReturn("john.updated@test.com");

            parser.parseAndExecute("user-update", scanner, system);

            verify(userManager, times(1)).update("john", "John Updated", "john.updated@test.com");
            String output = outContent.toString();
            assertTrue(output.contains("User data has been successfully updated"));
        }

        @Test
        @DisplayName("Обработка ошибок при обновлении пользователя")
        void shouldHandleUpdateErrors() {
            when(scanner.nextLine())
                    .thenReturn("john")
                    .thenReturn("")  // Invalid full name
                    .thenReturn("john@test.com");

            doThrow(new IllegalArgumentException("Invalid full name"))
                    .when(userManager).update(eq("john"), eq(""), eq("john@test.com"));

            parser.parseAndExecute("user-update", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Error updating user data"));
        }
    }

    @Nested
    @DisplayName("user-delete command")
    class UserDeleteCommandTests {

        @Test
        @DisplayName("Успешное удаление пользователя с положительным подтвержением")
        void shouldDeleteUserWhenConfirmed() {
            String username = "john";
            User user = User.validate(username, "John Doe", "john@test.com");

            when(scanner.nextLine())
                    .thenReturn(username)
                    .thenReturn("y");  // Confirm deletion

            when(userManager.findByUsername(username)).thenReturn(Optional.of(user));
            when(assignmentManager.findByUser(user)).thenReturn(List.of());

            parser.parseAndExecute("user-delete", scanner, system);

            verify(userManager, times(1)).remove(user);
            String output = outContent.toString();
            assertTrue(output.contains("User and his assignments have been successfully deleted"));
        }

        @Test
        @DisplayName("Успешный откат удаления пользователя")
        void shouldNotDeleteUserWhenNotConfirmed() {
            String username = "john";
            User user = User.validate(username, "John Doe", "john@test.com");

            when(scanner.nextLine())
                    .thenReturn(username)
                    .thenReturn("n");

            lenient().when(userManager.findByUsername(username)).thenReturn(Optional.of(user));

            parser.parseAndExecute("user-delete", scanner, system);

            verify(userManager, never()).remove(any());
            String output = outContent.toString();
            assertTrue(output.contains("Canceling user deletion"));
        }

        @Test
        @DisplayName("Обработка ошибок при несуществующем пользователе")
        void shouldShowErrorWhenUserNotFound() {
            String username = "unknown";
            when(scanner.nextLine()).thenReturn(username).thenReturn("y");
            when(userManager.findByUsername(username)).thenReturn(Optional.empty());

            parser.parseAndExecute("user-delete", scanner, system);

            verify(userManager, never()).remove(any());
            String output = outContent.toString();
            assertTrue(output.contains("User with username 'unknown' not found"));
        }
    }

    @Nested
    @DisplayName("user-search command")
    class UserSearchCommandTests {

        @Test
        @DisplayName("Успешный поиск пользователей по username")
        void shouldSearchByUsernameContains() {
            when(scanner.nextInt()).thenReturn(1);
            when(scanner.nextLine())
                    .thenReturn("")
                    .thenReturn("john");

            User user = User.validate("john123", "John Doe", "john@test.com");
            when(userManager.findByFilter(any())).thenReturn(List.of(user));

            parser.parseAndExecute("user-search", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains(user.format()));
        }

        @Test
        @DisplayName("Успешный поиск пользователей по email")
        void shouldSearchByEmail() {
            when(scanner.nextInt()).thenReturn(2);
            when(scanner.nextLine())
                    .thenReturn("")
                    .thenReturn("john@test.com");

            User user = User.validate("john", "John Doe", "john@test.com");
            when(userManager.findByFilter(any())).thenReturn(List.of(user));

            parser.parseAndExecute("user-search", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains(user.format()));
        }

        @Test
        @DisplayName("Успешный поиск пользователей по email domain")
        void shouldSearchByEmailDomain() {
            when(scanner.nextInt()).thenReturn(3);
            when(scanner.nextLine())
                    .thenReturn("")
                    .thenReturn("@test.com");

            User user = User.validate("john", "John Doe", "john@test.com");
            when(userManager.findByFilter(any())).thenReturn(List.of(user));

            parser.parseAndExecute("user-search", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains(user.format()));
        }

        @Test
        @DisplayName("Успешный поиск пользователей по full name")
        void shouldSearchByFullNameContains() {
            when(scanner.nextInt()).thenReturn(4);
            when(scanner.nextLine())
                    .thenReturn("")
                    .thenReturn("Doe");

            User user = User.validate("john", "John Doe", "john@test.com");
            when(userManager.findByFilter(any())).thenReturn(List.of(user));

            parser.parseAndExecute("user-search", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains(user.format()));
        }

        @Test
        @DisplayName("Обработка случая при отсутствующих пользователях")
        void shouldShowMissingWhenNoUsersFound() {
            when(scanner.nextInt()).thenReturn(1);
            when(scanner.nextLine())
                    .thenReturn("")
                    .thenReturn("nonexistent");

            when(userManager.findByFilter(any())).thenReturn(List.of());

            parser.parseAndExecute("user-search", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("missing"));
        }

        @Test
        @DisplayName("Обработка принудительной отмены фильтрации")
        void shouldCancelOnInvalidFilterNumber() {
            when(scanner.nextInt()).thenReturn(99);
            when(scanner.nextLine()).thenReturn("");

            parser.parseAndExecute("user-search", scanner, system);
            String output = outContent.toString();

            assertTrue(output.contains("Cancelling"));
            verify(userManager, never()).findByFilter(any());
        }
    }
}