package com.example.util;

import java.util.List;

public class FormatUtils {

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return "";
        }

        int[] columnWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            columnWidths[i] = headers[i].length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < Math.min(row.length, headers.length); i++) {
                if (row[i] != null && row[i].length() > columnWidths[i]) {
                    columnWidths[i] = row[i].length();
                }
            }
        }

        for (int i = 0; i < columnWidths.length; i++) {
            columnWidths[i] += 2;
        }

        StringBuilder table = new StringBuilder();

        table.append("+");
        for (int i = 0; i < columnWidths.length; i++) {
            table.append(repeatChar('-', columnWidths[i]));
            if (i < columnWidths.length - 1) {
                table.append("+");
            }
        }
        table.append("+\n");

        table.append("|");
        for (int i = 0; i < headers.length; i++) {
            table.append(padCenter(headers[i], columnWidths[i]));
            table.append("|");
        }
        table.append("\n");

        table.append("+");
        for (int i = 0; i < columnWidths.length; i++) {
            table.append(repeatChar('-', columnWidths[i]));
            if (i < columnWidths.length - 1) {
                table.append("+");
            }
        }
        table.append("+\n");

        for (String[] row : rows) {
            table.append("|");
            for (int i = 0; i < headers.length; i++) {
                String cell = (i < row.length && row[i] != null) ? row[i] : "";
                table.append(padCenter(cell, columnWidths[i]));
                table.append("|");
            }
            table.append("\n");
        }

        table.append("+");
        for (int i = 0; i < columnWidths.length; i++) {
            table.append(repeatChar('-', columnWidths[i]));
            if (i < columnWidths.length - 1) {
                table.append("+");
            }
        }
        table.append("+");

        return table.toString();
    }

    public static String formatBox(String text) {
        if (text == null || text.isEmpty()) {
            return "+--+\n|  |\n+--+";
        }

        String[] lines = text.split("\n");
        int maxLength = 0;
        for (String line : lines) {
            maxLength = Math.max(maxLength, line.length());
        }

        int boxWidth = maxLength + 2;

        StringBuilder box = new StringBuilder();

        box.append("+").append(repeatChar('-', boxWidth)).append("+\n");

        for (String line : lines) {
            box.append("| ").append(padRight(line, maxLength)).append(" |\n");
        }

        box.append("+").append(repeatChar('-', boxWidth)).append("+");

        return box.toString();
    }

    public static String formatHeader(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder header = new StringBuilder();
        String line = repeatChar('=', text.length() + 4);

        header.append(line).append("\n");
        header.append("= ").append(text).append(" =\n");
        header.append(line);

        return header.toString();
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        if (text.length() <= maxLength) {
            return text;
        }

        if (maxLength < 3) {
            return text.substring(0, maxLength);
        }

        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) {
            text = "";
        }

        if (text.length() >= length) {
            return text;
        }

        return text + repeatChar(' ', length - text.length());
    }

    public static String padLeft(String text, int length) {
        if (text == null) {
            text = "";
        }

        if (text.length() >= length) {
            return text;
        }

        return repeatChar(' ', length - text.length()) + text;
    }

    private static String padCenter(String text, int width) {
        if (text == null) {
            text = "";
        }

        if (text.length() >= width) {
            return text;
        }

        int leftPadding = (width - text.length()) / 2;
        int rightPadding = width - text.length() - leftPadding;

        return repeatChar(' ', leftPadding) + text + repeatChar(' ', rightPadding);
    }

    private static String repeatChar(char c, int count) {
        if (count <= 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}