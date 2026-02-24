package com.example.filter;

import com.example.model.Role;
import com.example.model.RoleAssignment;
import com.example.model.TemporaryAssignment;
import com.example.model.User;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class AssignmentFilters {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

    public static AssignmentFilter byUser(User user) {
        return assignment -> assignment.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        return assignment -> assignment.user().username()
                .equals(username);
    }

    public static AssignmentFilter byRole(Role role) {
        return assignment -> assignment.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        return assignment -> assignment.role().getName()
                .equals(roleName);
    }

    public static AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() {
        return assignment -> !assignment.isActive();
    }

    public static AssignmentFilter byType(String type) {
        return assignment -> assignment.assignmentType().equals(type);
    }

    public static AssignmentFilter assignedBy(String username) {
        return assignment -> assignment.metadata().assignedBy().equals(username);
    }

    public static AssignmentFilter assignedAfter(String date) {
        ZonedDateTime dateTimeForCompare = ZonedDateTime.parse(date, FORMATTER);

        return assignment -> {
            ZonedDateTime assignedDateTime = ZonedDateTime.parse(assignment.metadata().assignedAt(), FORMATTER);
            return assignedDateTime.isAfter(dateTimeForCompare);
        };
    }

    public static AssignmentFilter expiringBefore(String date) {
        ZonedDateTime dateTimeForCompare = ZonedDateTime.parse(date, FORMATTER);

        return assignment -> {
            TemporaryAssignment temporaryAssignment = (TemporaryAssignment)assignment;
            ZonedDateTime expiringDateTime = ZonedDateTime.parse(temporaryAssignment.getExpiresAt(), FORMATTER);

            return expiringDateTime.isBefore(dateTimeForCompare);
        };
    }
}