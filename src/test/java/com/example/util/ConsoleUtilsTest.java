package com.example.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConsoleUtilsTest {

    @Mock
    private Scanner mockScanner;

    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private PrintStream originalSystemOut;

    @BeforeEach
    void setUp() {
        originalSystemOut = System.out;
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Test
    @DisplayName("Запрос строки с валидным входом")
    void testPromptStringWithValidInput() {
        when(mockScanner.nextLine()).thenReturn("valid input");

        String result = ConsoleUtils.promptString(mockScanner, "Enter name: ");

        assertEquals("valid input", result);
        verify(mockScanner).nextLine();
    }

    @Test
    @DisplayName("Запрос строки с пустым входом (blank)")
    void testPromptStringWithBlankInputThenValidInput() {
        when(mockScanner.nextLine())
                .thenReturn("   ")
                .thenReturn("valid input");

        String result = ConsoleUtils.promptString(mockScanner, "Enter name: ");

        assertEquals("valid input", result);
        verify(mockScanner, times(2)).nextLine();
        assertTrue(outputStreamCaptor.toString().contains("input must not be null or empty"));
    }

    @Test
    @DisplayName("Запрос строки с пустым входом")
    void testPromptStringWithEmptyInputThenValidInput() {
        when(mockScanner.nextLine())
                .thenReturn("")
                .thenReturn("valid input");

        String result = ConsoleUtils.promptString(mockScanner, "Enter name: ");

        assertEquals("valid input", result);
        verify(mockScanner, times(2)).nextLine();
        assertTrue(outputStreamCaptor.toString().contains("input must not be null or empty"));
    }

    @Test
    @DisplayName("Запрос числа с валидным входом")
    void testPromptIntWithValidInput() {
        when(mockScanner.hasNextInt()).thenReturn(true);
        when(mockScanner.nextInt()).thenReturn(5);

        int result = ConsoleUtils.promptInt(mockScanner, "Enter number: ", 1, 10);

        assertEquals(5, result);
        verify(mockScanner).nextInt();
        verify(mockScanner).nextLine();
    }

    @Test
    @DisplayName("Запрос числа с невалидным входом")
    void testPromptIntWithInvalidNumberThenValidInput() {
        when(mockScanner.hasNextInt())
                .thenReturn(false)
                .thenReturn(true);
        when(mockScanner.nextInt()).thenReturn(5);

        int result = ConsoleUtils.promptInt(mockScanner, "Enter number: ", 1, 10);

        assertEquals(5, result);
        verify(mockScanner, times(2)).hasNextInt();
        verify(mockScanner, times(2)).nextLine(); // Изменено с 1 на 2
        verify(mockScanner).nextInt();
        assertTrue(outputStreamCaptor.toString().contains("Please enter a valid number"));
    }

    @Test
    @DisplayName("Запрос числа с валидным входом (ниже диапазона)")
    void testPromptIntWithValueBelowMinThenValidInput() {
        when(mockScanner.hasNextInt()).thenReturn(true);
        when(mockScanner.nextInt())
                .thenReturn(0)
                .thenReturn(5);

        int result = ConsoleUtils.promptInt(mockScanner, "Enter number: ", 1, 10);

        assertEquals(5, result);
        verify(mockScanner, times(2)).nextInt();
        verify(mockScanner, times(2)).nextLine();
        assertTrue(outputStreamCaptor.toString().contains("Number must be between 1 and 10"));
    }

    @Test
    @DisplayName("Запрос числа с валидным входом (выше диапазона)")
    void testPromptIntWithValueAboveMaxThenValidInput() {
        when(mockScanner.hasNextInt()).thenReturn(true);
        when(mockScanner.nextInt())
                .thenReturn(15)
                .thenReturn(5);

        int result = ConsoleUtils.promptInt(mockScanner, "Enter number: ", 1, 10);

        assertEquals(5, result);
        verify(mockScanner, times(2)).nextInt();
        verify(mockScanner, times(2)).nextLine();
        assertTrue(outputStreamCaptor.toString().contains("Number must be between 1 and 10"));
    }

    @Test
    @DisplayName("Запрос ответа (да)")
    void testPromptYesNoWithYes() {
        when(mockScanner.nextLine()).thenReturn("y");

        boolean result = ConsoleUtils.promptYesNo(mockScanner, "Continue? ");

        assertTrue(result);
        verify(mockScanner).nextLine();
    }

    @Test
    @DisplayName("Запрос ответа (да в UPPERCASE)")
    void testPromptYesNoWithYesUpperCase() {
        when(mockScanner.nextLine()).thenReturn("Y");

        boolean result = ConsoleUtils.promptYesNo(mockScanner, "Continue? ");

        assertTrue(result);
    }

    @Test
    @DisplayName("Запрос ответа (да с пробелами)")
    void testPromptYesNoWithYesWithSpaces() {
        when(mockScanner.nextLine()).thenReturn("  y  ");

        boolean result = ConsoleUtils.promptYesNo(mockScanner, "Continue? ");

        assertTrue(result);
    }

    @Test
    @DisplayName("Запрос ответа (нет)")
    void testPromptYesNoWithNo() {
        when(mockScanner.nextLine()).thenReturn("n");

        boolean result = ConsoleUtils.promptYesNo(mockScanner, "Continue? ");

        assertFalse(result);
    }

    @Test
    @DisplayName("Запрос ответа (другой вход)")
    void testPromptYesNoWithAnyOtherInput() {
        when(mockScanner.nextLine()).thenReturn("maybe");

        boolean result = ConsoleUtils.promptYesNo(mockScanner, "Continue? ");

        assertFalse(result);
    }

    @Test
    @DisplayName("Запрос выбора из предложенного")
    void testPromptChoiceWithValidSelection() {
        List<String> options = Arrays.asList("Option A", "Option B", "Option C");

        when(mockScanner.hasNextInt()).thenReturn(true);
        when(mockScanner.nextInt()).thenReturn(2);

        int result = ConsoleUtils.promptChoice(mockScanner, "Choose option: ", options);

        assertEquals(1, result);
        verify(mockScanner).nextInt();
        verify(mockScanner).nextLine();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("1. Option A"));
        assertTrue(output.contains("2. Option B"));
        assertTrue(output.contains("3. Option C"));
    }

    @Test
    @DisplayName("Запрос выбора из предложенного (первое)")
    void testPromptChoiceWithFirstOption() {
        List<String> options = Arrays.asList("Option A", "Option B");

        when(mockScanner.hasNextInt()).thenReturn(true);
        when(mockScanner.nextInt()).thenReturn(1);

        int result = ConsoleUtils.promptChoice(mockScanner, "Choose option: ", options);

        assertEquals(0, result);
    }

    @Test
    @DisplayName("Запрос выбора из предложенного (последнее)")
    void testPromptChoiceWithLastOption() {
        List<String> options = Arrays.asList("Option A", "Option B", "Option C");

        when(mockScanner.hasNextInt()).thenReturn(true);
        when(mockScanner.nextInt()).thenReturn(3);

        int result = ConsoleUtils.promptChoice(mockScanner, "Choose option: ", options);

        assertEquals(2, result);
    }

    @Test
    @DisplayName("Запрос выбора из предложенного (невалидный вход)")
    void testPromptChoiceWithInvalidThenValidSelection() {
        List<String> options = Arrays.asList("Option A", "Option B");

        when(mockScanner.hasNextInt())
                .thenReturn(false)
                .thenReturn(true);
        when(mockScanner.nextInt()).thenReturn(2);

        int result = ConsoleUtils.promptChoice(mockScanner, "Choose option: ", options);

        assertEquals(1, result);
        verify(mockScanner, times(2)).hasNextInt();
        verify(mockScanner, times(2)).nextLine();
        verify(mockScanner).nextInt();

        assertTrue(outputStreamCaptor.toString().contains("Please enter a valid number"));
    }

    @Test
    @DisplayName("Запрос выбора из предложенного (только одна опция существует)")
    void testPromptChoiceWithSingleOption() {
        List<String> options = List.of("Only Option");

        when(mockScanner.hasNextInt()).thenReturn(true);
        when(mockScanner.nextInt()).thenReturn(1);

        int result = ConsoleUtils.promptChoice(mockScanner, "Choose option: ", options);

        assertEquals(0, result);

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("1. Only Option"));
    }
}