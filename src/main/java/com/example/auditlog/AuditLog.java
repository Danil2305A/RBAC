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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.example.util.ValidationUtils.DATE_TIME_FORMATTER;

public class AuditLog {
    private final List<AuditEntry> entries = new ArrayList<>();

    private final BlockingQueue<AuditEntry> logQueue = new LinkedBlockingDeque<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread logProcessorThread;

    public AuditLog() {
        logProcessorThread = new Thread(() -> {
            while (running.get() || !logQueue.isEmpty()) {
                try {
                    AuditEntry entry = logQueue.take();
                    synchronized (entries) {
                        entries.add(entry);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            List<AuditEntry> remaining = new ArrayList<>();
            logQueue.drainTo(remaining);
            synchronized (entries) {
                entries.addAll(remaining);
            }
        }, "AuditLogProcessor");
        logProcessorThread.setDaemon(true);
        logProcessorThread.start();
    }

    public void shutdown() {
        running.set(false);
        logProcessorThread.interrupt();
        try {
            logProcessorThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public List<AuditEntry> getAll() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    public List<AuditEntry> getByPerformer(String performer) {
        synchronized (entries) {
            return entries.stream()
                    .filter(entry -> entry.performer().equals(performer))
                    .toList();
        }
    }

    public List<AuditEntry> getByAction(String action) {
        synchronized (entries) {
            return entries.stream()
                    .filter(entry -> entry.action().equals(action))
                    .toList();
        }
    }

    public void log(String action, String performer, String target, String details) {
        String timestamp = ZonedDateTime.now().format(DATE_TIME_FORMATTER);
        AuditEntry entry = new AuditEntry(timestamp, action, performer, target, details != null ? details : "not specified");

        logQueue.offer(entry);
    }

    public void printLogs() {
        synchronized (entries) {
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
    }

    public void saveToFile(String filepath) {
        Path path = Paths.get(filepath);

        try {
            Files.createDirectories(path.getParent());

            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            List<String> lines;
            synchronized (entries) {
                lines = entries.stream()
                        .map(e -> String.format("%s | %s | %s | %s | %s",
                                e.timestamp(), e.action(), e.performer(), e.target(), e.details()))
                        .toList();
            }
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.printf("Error saving audit entries: %s\n", e.getMessage());
        }
    }
}