package com.example.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.example.util.ValidationUtils.DATE_TIME_FORMATTER;
import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilsTest {
    @Test
    @DisplayName("Получение текущей даты в формате yyyy-MM-dd")
    void getCurrentDateShouldReturnCurrentDateInCorrectFormat() {
        String currentDate = DateTimeUtils.getCurrentDate();

        assertNotNull(currentDate);
        assertTrue(currentDate.matches("\\d{4}-\\d{2}-\\d{2}"));

        LocalDate expectedDate = LocalDate.now();
        assertEquals(expectedDate.toString(), currentDate);
    }

    @Test
    @DisplayName("Получение текущих даты и времени в формате yyyy-MM-dd HH:mm:ss XXX")
    void getCurrentDateTimeShouldReturnCurrentDateTimeWithTimezone() {
        String currentDateTime = DateTimeUtils.getCurrentDateTime();

        assertNotNull(currentDateTime);
        assertTrue(currentDateTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} [+-]\\d{2}:\\d{2}"));

        assertDoesNotThrow(() -> {
            ZonedDateTime.parse(currentDateTime, DATE_TIME_FORMATTER);
        });
    }

    @Test
    @DisplayName("Первая дата до второй даты - правда")
    void isBeforeShouldReturnTrueWhenFirstIsBeforeSecond() {
        String dateTime1 = "2024-01-01 10:00:00 +03:00";
        String dateTime2 = "2024-01-01 15:00:00 +03:00";

        boolean result = DateTimeUtils.isBefore(dateTime1, dateTime2);

        assertTrue(result);
    }

    @Test
    @DisplayName("Первая дата до второй даты - ложь")
    void isBeforeShouldReturnFalseWhenFirstIsAfterSecond() {
        String dateTime1 = "2024-01-01 15:00:00 +03:00";
        String dateTime2 = "2024-01-01 10:00:00 +03:00";

        boolean result = DateTimeUtils.isBefore(dateTime1, dateTime2);

        assertFalse(result);
    }

    @Test
    @DisplayName("Первая дата до второй даты - ложь при равенстве")
    void isBeforeShouldReturnFalseWhenDatesAreEqual() {
        String dateTime1 = "2024-01-01 10:00:00 +03:00";
        String dateTime2 = "2024-01-01 10:00:00 +03:00";

        boolean result = DateTimeUtils.isBefore(dateTime1, dateTime2);

        assertFalse(result);
    }

    @Test
    @DisplayName("Первая дата до второй даты - с разным часовыми зонами")
    void isBeforeShouldHandleDifferentTimezones() {
        String dateTime1 = "2024-01-01 10:00:00 +03:00";
        String dateTime2 = "2024-01-01 09:00:00 +02:00";

        boolean result = DateTimeUtils.isBefore(dateTime1, dateTime2);

        ZonedDateTime zdt1 = ZonedDateTime.parse(dateTime1, DATE_TIME_FORMATTER);
        ZonedDateTime zdt2 = ZonedDateTime.parse(dateTime2, DATE_TIME_FORMATTER);
        assertEquals(zdt1.isBefore(zdt2), result);
    }

    @Test
    @DisplayName("Первая дата после второй даты - правда")
    void isAfterShouldReturnTrueWhenFirstIsAfterSecond() {
        String dateTime1 = "2024-01-01 15:00:00 +03:00";
        String dateTime2 = "2024-01-01 10:00:00 +03:00";

        boolean result = DateTimeUtils.isAfter(dateTime1, dateTime2);

        assertTrue(result);
    }

    @Test
    @DisplayName("Первая дата после второй даты - ложь")
    void isAfterShouldReturnFalseWhenFirstIsBeforeSecond() {
        String dateTime1 = "2024-01-01 10:00:00 +03:00";
        String dateTime2 = "2024-01-01 15:00:00 +03:00";

        boolean result = DateTimeUtils.isAfter(dateTime1, dateTime2);

        assertFalse(result);
    }

    @Test
    @DisplayName("Первая дата после второй даты - ложь при равенстве")
    void isAfterShouldReturnFalseWhenDatesAreEqual() {
        String dateTime1 = "2024-01-01 10:00:00 +03:00";
        String dateTime2 = "2024-01-01 10:00:00 +03:00";

        boolean result = DateTimeUtils.isAfter(dateTime1, dateTime2);

        assertFalse(result);
    }

    @Test
    @DisplayName("Первая дата после второй даты - с разными часовыми зонами")
    void isAfterShouldHandleDifferentTimezones() {
        String dateTime1 = "2024-01-01 09:00:00 +02:00";
        String dateTime2 = "2024-01-01 10:00:00 +03:00";

        boolean result = DateTimeUtils.isAfter(dateTime1, dateTime2);

        ZonedDateTime zdt1 = ZonedDateTime.parse(dateTime1, DATE_TIME_FORMATTER);
        ZonedDateTime zdt2 = ZonedDateTime.parse(dateTime2, DATE_TIME_FORMATTER);
        assertEquals(zdt1.isAfter(zdt2), result);
    }

    @ParameterizedTest
    @CsvSource({
            "2024-01-01 10:00:00 +03:00, 5, 2024-01-06 10:00:00 +03:00",
            "2024-02-28 23:59:59 +03:00, 1, 2024-02-29 23:59:59 +03:00",
            "2024-12-30 12:00:00 +03:00, 2, 2025-01-01 12:00:00 +03:00",
            "2024-01-01 10:00:00 +03:00, 0, 2024-01-01 10:00:00 +03:00",
            "2024-01-01 10:00:00 +03:00, -1, 2023-12-31 10:00:00 +03:00"
    })
    @DisplayName("Добавление дней")
    void addDaysShouldAddDaysCorrectly(String dateTime, long days, String expected) {
        String result = DateTimeUtils.addDays(dateTime, days);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Добавление дней - часовая зона сохраняется")
    void addDaysShouldPreserveTimezone() {
        String dateTime = "2024-01-01 10:00:00 +05:00";
        String result = DateTimeUtils.addDays(dateTime, 1);

        ZonedDateTime parsedResult = ZonedDateTime.parse(result, DATE_TIME_FORMATTER);
        assertEquals(ZoneId.of("+05:00"), parsedResult.getZone());
        assertEquals(10, parsedResult.getHour());
        assertEquals(0, parsedResult.getMinute());
    }
}