package com.example.auditlog;

import com.example.util.FormatUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.util.ValidationUtils.DATE_TIME_FORMATTER;

public class AuditLog {
    private final List<AuditEntry> entries = new ArrayList<>();

    public List<AuditEntry> getAll() {
        return entries;
    }

    public List<AuditEntry> getByPerformer(String performer) {
        return entries.stream()
                .filter(entry -> entry.performer().equals(performer))
                .toList();
    }

    public List<AuditEntry> getByAction(String action) {
        return entries.stream()
                .filter(entry -> entry.action().equals(action))
                .toList();
    }

    public void log(String action, String performer, String target, String details) {
        String timestamp = ZonedDateTime.now().format(DATE_TIME_FORMATTER);
        entries.add(new AuditEntry(timestamp, action, performer, target, details != null ? details : "not specified"));
    }

    public void printLogs() {
        if (entries.isEmpty()) {
            System.out.println("missing audit entries");
            return;
        }

        String[] headers = {"Timestamp", "Action", "Performer", "Target", "Details"};
        List<String[]> rows = entries.stream()
                .map(entry -> new String[]{
                        entry.timestamp(),
                        entry.action(),
                        entry.performer(),
                        entry.target(),
                        entry.details()
                })
                .collect(Collectors.toList());
        System.out.println(FormatUtils.formatTable(headers, rows));
    }

    public void saveToFile(String filepath) {
        Path path = Paths.get(filepath);

        try {
            Files.createDirectories(path.getParent());

            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            List<String> lines = entries.stream()
                    .map(e -> String.format("%s | %s | %s | %s | %s",
                            e.timestamp(), e.action(), e.performer(), e.target(), e.details()))
                    .toList();
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.printf("Error saving audit entries: %s\n", e.getMessage());
        }
    }
}