package com.example.auditlog;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {
    private AuditLog auditLog;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Получение всех логов при их отсутствии")
    void getAll_ShouldReturnEmptyList_WhenNoEntries() {
        List<AuditEntry> entries = auditLog.getAll();
        assertTrue(entries.isEmpty());
    }

    @Test
    @DisplayName("Получение всех существующих логов")
    void getAll_ShouldReturnAllEntries_WhenEntriesExist() {
        auditLog.log("LOGIN", "john.doe", "System", null);
        auditLog.log("LOGOUT", "john.doe", "System", "Session ended");

        List<AuditEntry> entries = auditLog.getAll();

        assertEquals(2, entries.size());
        assertEquals("LOGIN", entries.get(0).action());
        assertEquals("LOGOUT", entries.get(1).action());
    }

    @Test
    @DisplayName("Получение всех несуществующих логов по performer")
    void getByPerformer_ShouldReturnEmptyList_WhenNoEntriesForPerformer() {
        auditLog.log("LOGIN", "john.doe", "System", null);

        List<AuditEntry> entries = auditLog.getByPerformer("jane.doe");

        assertTrue(entries.isEmpty());
    }

    @Test
    @DisplayName("Получение всех существующих логов по performer")
    void getByPerformer_ShouldReturnEntriesForSpecificPerformer() {
        auditLog.log("LOGIN", "john.doe", "System", null);
        auditLog.log("LOGOUT", "john.doe", "System", "Session ended");
        auditLog.log("LOGIN", "jane.doe", "System", null);

        List<AuditEntry> entries = auditLog.getByPerformer("john.doe");

        assertEquals(2, entries.size());
        assertTrue(entries.stream().allMatch(entry -> entry.performer().equals("john.doe")));
    }

    @Test
    @DisplayName("Получение всех несуществующих логов по action")
    void getByAction_ShouldReturnEmptyList_WhenNoEntriesForAction() {
        auditLog.log("LOGIN", "john.doe", "System", null);

        List<AuditEntry> entries = auditLog.getByAction("DELETE");

        assertTrue(entries.isEmpty());
    }

    @Test
    @DisplayName("Получение всех существующих логов по action")
    void getByAction_ShouldReturnEntriesForSpecificAction() {
        auditLog.log("LOGIN", "john.doe", "System", null);
        auditLog.log("LOGOUT", "john.doe", "System", "Session ended");
        auditLog.log("LOGIN", "jane.doe", "System", null);

        List<AuditEntry> entries = auditLog.getByAction("LOGIN");

        assertEquals(2, entries.size());
        assertTrue(entries.stream().allMatch(entry -> entry.action().equals("LOGIN")));
    }

    @Test
    @DisplayName("Добавление лога")
    void log_ShouldAddEntryWithCurrentTimestamp() {
        auditLog.log("LOGIN", "john.doe", "System", "Test details");

        List<AuditEntry> entries = auditLog.getAll();

        assertEquals(1, entries.size());
        AuditEntry entry = entries.getFirst();
        assertEquals("LOGIN", entry.action());
        assertEquals("john.doe", entry.performer());
        assertEquals("System", entry.target());
        assertEquals("Test details", entry.details());
        assertNotNull(entry.timestamp());
    }

    @Test
    @DisplayName("Вывод пустого списка логов")
    void printLogs_ShouldPrintMissingEntriesMessage_WhenNoEntries() {
        auditLog.printLogs();

        String output = outputStreamCaptor.toString().trim();
        assertEquals("missing audit entries", output);
    }

    @Test
    @DisplayName("Вывод всех логов")
    void printLogs_ShouldPrintFormattedTable_WhenEntriesExist() {
        auditLog.log("LOGIN", "john.doe", "System", "Test login");

        auditLog.printLogs();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Timestamp"));
        assertTrue(output.contains("Action"));
        assertTrue(output.contains("Performer"));
        assertTrue(output.contains("Target"));
        assertTrue(output.contains("Details"));
        assertTrue(output.contains("LOGIN"));
        assertTrue(output.contains("john.doe"));
        assertTrue(output.contains("System"));
        assertTrue(output.contains("Test login"));
    }

    @Test
    @DisplayName("Сохранение логов и создание файла")
    void saveToFile_ShouldCreateFileAndWriteEntries(@TempDir Path tempDir) throws Exception {
        Path filePath = tempDir.resolve("audit.log");
        auditLog.log("LOGIN", "john.doe", "System", "Test login");
        auditLog.log("LOGOUT", "john.doe", "System", "Test logout");

        auditLog.saveToFile(filePath.toString());

        assertTrue(Files.exists(filePath));
        List<String> lines = Files.readAllLines(filePath);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("LOGIN | john.doe | System | Test login"));
        assertTrue(lines.get(1).contains("LOGOUT | john.doe | System | Test logout"));
    }

    @Test
    @DisplayName("Сохранение логов при несуществующих директориях")
    void saveToFile_ShouldCreateDirectories_WhenTheyDontExist(@TempDir Path tempDir) throws Exception {
        Path nestedPath = tempDir.resolve("logs/subdir/audit.log");
        auditLog.log("LOGIN", "john.doe", "System", "Test login");

        auditLog.saveToFile(nestedPath.toString());

        assertTrue(Files.exists(nestedPath));
        assertTrue(Files.exists(nestedPath.getParent()));
    }

    @Test
    @DisplayName("Сохранение логов в непустой файл - перезапись")
    void saveToFile_ShouldOverwriteExistingFile(@TempDir Path tempDir) throws Exception {
        Path filePath = tempDir.resolve("audit.log");
        Files.write(filePath, List.of("old content"));

        auditLog.log("LOGIN", "john.doe", "System", "Test login");
        auditLog.saveToFile(filePath.toString());

        List<String> lines = Files.readAllLines(filePath);
        assertEquals(1, lines.size());
        assertTrue(lines.getFirst().contains("LOGIN"));
    }

    @Test
    @DisplayName("Обработка исключения при сохранении файла")
    void saveToFile_ShouldHandleIOException() {
        Path invalidPath = Paths.get("/invalid/path/that/does/not/exist/audit.log");

        auditLog.log("LOGIN", "john.doe", "System", "Test login");

        assertDoesNotThrow(() -> auditLog.saveToFile(invalidPath.toString()));

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Error saving audit entries"));
    }
}