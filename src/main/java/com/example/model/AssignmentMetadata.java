package com.example.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {
    public static AssignmentMetadata now(String assignedBy, String reason) {
        assignedBy = assignedBy != null ? assignedBy.trim() : null;
        validateAssignedBy(assignedBy);

        String assignedAt = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX"));
        reason = (reason == null || reason.isBlank()) ? "not specified" : reason.trim();

        return new AssignmentMetadata(assignedBy, assignedAt, reason);
    }

    private static void validateAssignedBy(String assignedBy) {
        if (assignedBy == null || assignedBy.isBlank()) {
            throw new IllegalArgumentException("assigned by must not be null or blank");
        }
    }

    public String format() {
        return String.format("""
                Assigned by: %s
                Assigned at: %s
                Reason: %s""", assignedBy, assignedAt, reason);
    }
}