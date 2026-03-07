package com.example.command;

import com.example.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandParserTest {

    private CommandParser parser;
    private RBACSystem system;
    private Scanner scanner;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        system = mock(RBACSystem.class);
        scanner = mock(Scanner.class);

        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @Nested
    @DisplayName("Command Registration Tests")
    class CommandRegistrationTests {

        @Test
        @DisplayName("Успешная регистрация команды")
        void shouldRegisterCommand() {
            Command testCommand = (s, sys) -> System.out.println("test executed");

            parser.registerCommand("test", "Test command", testCommand);

            assertDoesNotThrow(() -> parser.executeCommand("test", scanner, system));
        }

        @Test
        @DisplayName("Выброс исключения при выполнении незарегистрированной команды")
        void shouldThrowExceptionForUnregisteredCommand() {
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> parser.executeCommand("unknown", scanner, system)
            );

            assertEquals("command 'unknown' not found in command registry", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Command Execution Tests")
    class CommandExecutionTests {

        @Test
        @DisplayName("Успешное выполнение зарегистрированной команды")
        void shouldExecuteRegisteredCommand() {
            Command mockCommand = mock(Command.class);
            parser.registerCommand("test", "Test command", mockCommand);

            parser.executeCommand("test", scanner, system);

            verify(mockCommand, times(1)).execute(scanner, system);
        }

        @Test
        @DisplayName("Успешный парсинг и выполнение зарегистрированной команды")
        void shouldParseAndExecuteCommand() {
            Command mockCommand = mock(Command.class);
            parser.registerCommand("test", "Test command", mockCommand);

            parser.parseAndExecute("test", scanner, system);

            verify(mockCommand, times(1)).execute(scanner, system);
        }

        @Test
        @DisplayName("Выброс исключения при input == null")
        void shouldThrowExceptionForNullInput() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> parser.parseAndExecute(null, scanner, system)
            );

            assertEquals("input must not be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Выброс исключения при пустом input")
        void shouldThrowExceptionForEmptyInput() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> parser.parseAndExecute("", scanner, system)
            );

            assertEquals("input must not be null or empty", exception.getMessage());
        }
    }
}