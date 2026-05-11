package com.example.util;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static com.example.util.ValidationUtils.DATE_TIME_FORMATTER;

public class DateTimeUtils {
    public static String getCurrentDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public static String getCurrentDateTime() {
        return ZonedDateTime.now().format(DATE_TIME_FORMATTER);
    }

    public static boolean isBefore(String dateTime1, String dateTime2) {
        ZonedDateTime zonedDateTime1 = ZonedDateTime.parse(dateTime1, DATE_TIME_FORMATTER);
        ZonedDateTime zonedDateTime2 = ZonedDateTime.parse(dateTime2, DATE_TIME_FORMATTER);

        return zonedDateTime1.isBefore(zonedDateTime2);
    }

    public static boolean isAfter(String dateTime1, String dateTime2) {
        ZonedDateTime zonedDateTime1 = ZonedDateTime.parse(dateTime1, DATE_TIME_FORMATTER);
        ZonedDateTime zonedDateTime2 = ZonedDateTime.parse(dateTime2, DATE_TIME_FORMATTER);

        return zonedDateTime1.isAfter(zonedDateTime2);
    }

    public static String addDays(String dateTime, long days) {
        ZonedDateTime zonedDateTime1 = ZonedDateTime.parse(dateTime, DATE_TIME_FORMATTER);
        return zonedDateTime1.plusDays(days).format(DATE_TIME_FORMATTER);
    }
}