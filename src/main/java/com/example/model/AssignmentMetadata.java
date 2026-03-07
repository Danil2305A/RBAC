package com.example.model;

import java.time.ZonedDateTime;

import static com.example.util.ValidationUtils.*;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {
    public static AssignmentMetadata now(String assignedBy, String reason) {
        assignedBy = normalizeString(assignedBy);
        validateAssignedBy(assignedBy);

        String assignedAt = ZonedDateTime.now().format(DATE_TIME_FORMATTER);
        reason = (reason == null || reason.isBlank()) ? "not specified" : reason.trim();

        return new AssignmentMetadata(assignedBy, assignedAt, reason);
    }

    public String format() {
        return String.format("""
                Assigned by: %s
                Assigned at: %s
                Reason: %s""", assignedBy, assignedAt, reason);
    }
}