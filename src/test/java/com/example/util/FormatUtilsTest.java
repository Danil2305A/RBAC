package com.example.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FormatUtilsTest {

    @Nested
    @DisplayName("Тесты метода formatTable")
    class FormatTableTests {

        @Test
        @DisplayName("Должен корректно форматировать таблицу с данными")
        void shouldFormatTableWithData() {
            String[] headers = {"Name", "Age", "City"};
            List<String[]> rows = List.of(
                    new String[]{"John", "25", "New York"},
                    new String[]{"Alice", "30", "London"}
            );

            String result = FormatUtils.formatTable(headers, rows);

            assertAll(
                    () -> assertTrue(result.contains("Name")),
                    () -> assertTrue(result.contains("Age")),
                    () -> assertTrue(result.contains("City")),
                    () -> assertTrue(result.contains("John")),
                    () -> assertTrue(result.contains("Alice")),
                    () -> assertTrue(result.contains("25")),
                    () -> assertTrue(result.contains("30")),
                    () -> assertTrue(result.contains("New York")),
                    () -> assertTrue(result.contains("London")),
                    () -> assertTrue(result.startsWith("+")),
                    () -> assertTrue(result.endsWith("+")),
                    () -> assertTrue(result.contains("|"))
            );
        }

        @Test
        @DisplayName("Должен обрабатывать null значения в ячейках")
        void shouldHandleNullCells() {
            String[] headers = {"Header1", "Header2"};
            List<String[]> rows = List.of(
                    new String[]{null, "value2"},
                    new String[]{"value3", null}
            );

            String result = FormatUtils.formatTable(headers, rows);

            assertAll(
                    () -> assertTrue(result.contains("Header1")),
                    () -> assertTrue(result.contains("Header2")),
                    () -> assertTrue(result.contains("value2")),
                    () -> assertTrue(result.contains("value3")),
                    () -> assertFalse(result.contains("null"))
            );
        }

        @Test
        @DisplayName("Должен возвращать пустую строку при null заголовках")
        void shouldReturnEmptyWhenHeadersNull() {
            List<String[]> rows = List.<String[]>of(new String[]{"data"});
            String result = FormatUtils.formatTable(null, rows);
            assertEquals("", result);
        }

        @Test
        @DisplayName("Должен возвращать пустую строку при пустых заголовках")
        void shouldReturnEmptyWhenHeadersEmpty() {
            String[] headers = {};
            List<String[]> rows = List.<String[]>of(new String[]{"data"});
            String result = FormatUtils.formatTable(headers, rows);
            assertEquals("", result);
        }

        @Test
        @DisplayName("Должен корректно обрабатывать пустой список строк")
        void shouldHandleEmptyRows() {
            String[] headers = {"Header1", "Header2"};
            List<String[]> rows = List.of();

            String result = FormatUtils.formatTable(headers, rows);

            assertAll(
                    () -> assertTrue(result.contains("Header1")),
                    () -> assertTrue(result.contains("Header2")),
                    () -> assertTrue(result.contains("+")),
                    () -> assertTrue(result.contains("|"))
            );
        }

        @Test
        @DisplayName("Должен автоматически подбирать ширину столбцов")
        void shouldAutoAdjustColumnWidths() {
            String[] headers = {"Short", "Medium Length", "Very Very Long Header"};
            List<String[]> rows = List.of(
                    new String[]{"A", "B", "C"},
                    new String[]{"Very Long Cell Content Here", "Normal", "Short"}
            );

            String result = FormatUtils.formatTable(headers, rows);
            String[] lines = result.split("\n");

            assertTrue(result.contains("Very Long Cell Content Here"));
            assertTrue(result.contains("Very Very Long Header"));

            int firstLineLength = lines[0].length();
            for (String line : lines) {
                assertEquals(firstLineLength, line.length(),
                        "Все строки таблицы должны быть одинаковой длины");
            }
        }
    }

    @Nested
    @DisplayName("Тесты метода formatBox")
    class FormatBoxTests {

        @Test
        @DisplayName("Должен обрамлять текст в рамку")
        void shouldWrapTextInBox() {
            String text = "Hello";
            String result = FormatUtils.formatBox(text);

            String[] lines = result.split("\n");

            assertAll(
                    () -> assertEquals(3, lines.length),
                    () -> assertTrue(lines[0].startsWith("+")),
                    () -> assertTrue(lines[0].endsWith("+")),
                    () -> assertEquals("| Hello |", lines[1]),
                    () -> assertTrue(lines[2].startsWith("+")),
                    () -> assertTrue(lines[2].endsWith("+"))
            );
        }

        @Test
        @DisplayName("Должен обрабатывать многострочный текст")
        void shouldHandleMultilineText() {
            String text = "Line1\nLine2\nLine3";
            String result = FormatUtils.formatBox(text);

            String[] lines = result.split("\n");

            assertAll(
                    () -> assertEquals(5, lines.length),
                    () -> assertTrue(lines[0].startsWith("+")),
                    () -> assertEquals("| Line1 |", lines[1]),
                    () -> assertEquals("| Line2 |", lines[2]),
                    () -> assertEquals("| Line3 |", lines[3]),
                    () -> assertTrue(lines[4].startsWith("+"))
            );
        }

        @Test
        @DisplayName("Должен обрабатывать пустую строку")
        void shouldHandleEmptyString() {
            String result = FormatUtils.formatBox("");

            String[] lines = result.split("\n");

            assertAll(
                    () -> assertEquals(3, lines.length),
                    () -> assertTrue(lines[0].startsWith("+")),
                    () -> assertTrue(lines[1].startsWith("|")),
                    () -> assertTrue(lines[1].endsWith("|")),
                    () -> assertTrue(lines[2].startsWith("+"))
            );
        }

        @Test
        @DisplayName("Должен обрабатывать null")
        void shouldHandleNull() {
            String result = FormatUtils.formatBox(null);

            String[] lines = result.split("\n");

            assertAll(
                    () -> assertEquals(3, lines.length),
                    () -> assertTrue(lines[0].startsWith("+")),
                    () -> assertTrue(lines[1].startsWith("|")),
                    () -> assertTrue(lines[1].endsWith("|")),
                    () -> assertTrue(lines[2].startsWith("+"))
            );
        }

        @Test
        @DisplayName("Должен подбирать ширину под самый длинный текст")
        void shouldAdjustWidthToLongestLine() {
            String text = "Short\nThis is a very long line\nTiny";
            String result = FormatUtils.formatBox(text);

            String[] lines = result.split("\n");
            int boxWidth = lines[0].length();

            for (String line : lines) {
                assertEquals(boxWidth, line.length());
            }

            assertTrue(lines[2].contains("This is a very long line"));
        }
    }

    @Nested
    @DisplayName("Тесты метода formatHeader")
    class FormatHeaderTests {

        @Test
        @DisplayName("Должен форматировать заголовок")
        void shouldFormatHeader() {
            String text = "Test Header";
            String result = FormatUtils.formatHeader(text);

            String[] lines = result.split("\n");

            assertAll(
                    () -> assertEquals(3, lines.length),
                    () -> assertTrue(lines[0].matches("=+")),
                    () -> assertEquals("= " + text + " =", lines[1]),
                    () -> assertTrue(lines[2].matches("=+")),
                    () -> assertEquals(lines[0].length(), lines[2].length())
            );
        }

        @Test
        @DisplayName("Должен обрабатывать пустую строку")
        void shouldHandleEmptyString() {
            String result = FormatUtils.formatHeader("");
            assertEquals("", result);
        }

        @Test
        @DisplayName("Должен обрабатывать null")
        void shouldHandleNull() {
            String result = FormatUtils.formatHeader(null);
            assertEquals("", result);
        }
    }

    @Nested
    @DisplayName("Тесты метода truncate")
    class TruncateTests {

        @ParameterizedTest
        @CsvSource({
                "'Hello World', 5, 'He...'",
                "'Hello', 10, 'Hello'",
                "'', 5, ''",
                "'Hello World', 8, 'Hello...'"
        })
        @DisplayName("Должен корректно обрезать строки")
        void shouldTruncateStringsCorrectly(String input, int maxLength, String expected) {
            assertEquals(expected, FormatUtils.truncate(input, maxLength));
        }

        @Test
        @DisplayName("Должен обрабатывать null")
        void shouldHandleNull() {
            assertEquals("", FormatUtils.truncate(null, 10));
        }

        @Test
        @DisplayName("Должен обрабатывать maxLength меньше 3")
        void shouldHandleMaxLengthLessThan3() {
            assertEquals("H", FormatUtils.truncate("Hello", 1));
            assertEquals("He", FormatUtils.truncate("Hello", 2));
        }
    }

    @Nested
    @DisplayName("Тесты методов padRight и padLeft")
    class PaddingTests {

        @ParameterizedTest
        @CsvSource({
                "'Hello', 10, 'Hello     '",
                "'', 5, '     '",
                "'Hello', 3, 'Hello'",
                "'Hello World', 15, 'Hello World    '"
        })
        @DisplayName("padRight должен корректно дополнять строку справа")
        void padRightShouldAddSpacesToRight(String input, int length, String expected) {
            assertEquals(expected, FormatUtils.padRight(input, length));
        }

        @ParameterizedTest
        @CsvSource({
                "'Hello', 10, '     Hello'",
                "'', 5, '     '",
                "'Hello', 3, 'Hello'",
                "'Hello World', 15, '    Hello World'"
        })
        @DisplayName("padLeft должен корректно дополнять строку слева")
        void padLeftShouldAddSpacesToLeft(String input, int length, String expected) {
            assertEquals(expected, FormatUtils.padLeft(input, length));
        }

        @Test
        @DisplayName("padRight должен обрабатывать null")
        void padRightShouldHandleNull() {
            assertEquals("     ", FormatUtils.padRight(null, 5));
        }

        @Test
        @DisplayName("padLeft должен обрабатывать null")
        void padLeftShouldHandleNull() {
            assertEquals("     ", FormatUtils.padLeft(null, 5));
        }
    }

    @Nested
    @DisplayName("Тесты приватного метода padCenter")
    class PadCenterTests {
        @Test
        @DisplayName("Должен центрировать текст в ячейках таблицы")
        void shouldCenterTextInTableCells() {
            String[] headers = {"A"};
            List<String[]> rows = List.<String[]>of(
                    new String[]{"Hello"}
            );

            String result = FormatUtils.formatTable(headers, rows);

            assertTrue(result.contains("|  Hello  |") ||
                    result.contains("| Hello |") ||
                    result.contains("|   Hello   |"));
        }
    }

    @Nested
    @DisplayName("Тесты приватного метода repeatChar")
    class RepeatCharTests {
        @Test
        @DisplayName("Должен повторять символы корректно")
        void shouldRepeatCharactersCorrectly() {
            String header = FormatUtils.formatHeader("Test");
            assertTrue(header.startsWith("===="));

            String box = FormatUtils.formatBox("Hi");
            assertTrue(box.startsWith("+----+"));
        }

        @Test
        @DisplayName("Должен обрабатывать нулевое количество повторений")
        void shouldHandleZeroCount() {
            String result = FormatUtils.padRight("Hello", 3);
            assertEquals("Hello", result);
        }
    }

    @Nested
    @DisplayName("Интеграционные тесты")
    class IntegrationTests {

        @Test
        @DisplayName("Должен корректно комбинировать несколько методов")
        void shouldCombineMultipleMethods() {
            String header = FormatUtils.formatHeader("USER REPORT");

            String[] tableHeaders = {"Name", "Status"};
            List<String[]> rows = List.of(
                    new String[]{"John Doe", "Active"},
                    new String[]{"Jane Smith", "Inactive"}
            );
            String table = FormatUtils.formatTable(tableHeaders, rows);

            String summary = FormatUtils.formatBox("Total users: 2");

            String combined = header + "\n" + table + "\n" + summary;

            assertAll(
                    () -> assertTrue(combined.contains("USER REPORT")),
                    () -> assertTrue(combined.contains("John Doe")),
                    () -> assertTrue(combined.contains("Jane Smith")),
                    () -> assertTrue(combined.contains("Total users: 2")),
                    () -> assertTrue(combined.contains("=")),
                    () -> assertTrue(combined.contains("+")),
                    () -> assertTrue(combined.contains("|"))
            );
        }

        @Test
        @DisplayName("Должен обрабатывать длинные строки в таблице с обрезкой")
        void shouldHandleLongStringsWithTruncation() {
            String[] headers = {"Column1", "Column2"};
            String longString = "This is a very long string that might need truncation";
            String truncated = FormatUtils.truncate(longString, 20);

            List<String[]> rows = List.<String[]>of(
                    new String[]{longString, truncated}
            );

            String table = FormatUtils.formatTable(headers, rows);

            assertTrue(table.contains(truncated));
        }
    }
}