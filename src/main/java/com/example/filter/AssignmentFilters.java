package com.example.filter;

import com.example.model.Role;
import com.example.model.RoleAssignment;
import com.example.model.TemporaryAssignment;
import com.example.model.User;

import static com.example.util.DateTimeUtils.isAfter;
import static com.example.util.DateTimeUtils.isBefore;

public class AssignmentFilters {
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
        return assignment -> isAfter(assignment.metadata().assignedAt(), date);
    }

    public static AssignmentFilter expiringBefore(String date) {
        return assignment -> {
            TemporaryAssignment temporaryAssignment = (TemporaryAssignment)assignment;
            return isBefore(temporaryAssignment.getExpiresAt(), date);
        };
    }
}