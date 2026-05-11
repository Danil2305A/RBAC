package com.example.util;

import com.example.model.Permission;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

    public static void validateUsername(String username) {
        requireNonBlank(username, "username");

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("username must contain only latin letters, digits, underscores " +
                    "and his length must be between 3 and 20 symbols");
        }
    }

    public static void validateFullName(String fullName) {
        requireNonBlank(fullName, "user full name");
    }

    public static void validateEmail(String email) {
        requireNonBlank(email, "user email");

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("user email must match the basic format");
        }
    }

    public static void validatePermissionName(String name) {
        requireNonBlank(name, "permission name");

        if (name.contains(" ")) {
            throw new IllegalArgumentException("permission name must not contain spaces");
        }
    }

    public static void validatePermissionResource(String resource) {
        requireNonBlank(resource, "permission resource");
    }

    public static void validatePermissionDescription(String description) {
        requireNonBlank(description, "permission description");
    }

    public static void validateRoleName(String name) {
        requireNonBlank(name, "role name");
    }

    public static void validateRoleDescription(String description) {
        requireNonBlank(description, "role description");
    }

    public static void validateRolePermissions(Set<Permission> permissions) {
        if (permissions == null) {
            throw new IllegalArgumentException("role permissions must not be null");
        }
    }

    public static void validateAssignedBy(String assignedBy) {
        requireNonBlank(assignedBy, "assigned by");
    }

    public static void validateExpirationDate(String expirationDate) {
        requireNonBlank(expirationDate, "expiration date");

        try {
            ZonedDateTime.parse(expirationDate, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("expiration date must be in format: yyyy-MM-dd HH:mm:ss XXX", e);
        }
    }

    public static String normalizeString(String input) {
        return input != null ? input.trim() : null;
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(String.format("%s must not be null or blank", fieldName));
        }
    }
}