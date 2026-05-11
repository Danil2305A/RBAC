package com.example.auditlog;

public record AuditEntry(
        String timestamp,
        String action,
        String performer,
        String target,
        String details) {
}