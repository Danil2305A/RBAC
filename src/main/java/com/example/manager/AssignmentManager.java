package com.example.manager;

import com.example.filter.AssignmentFilter;
import com.example.filter.AssignmentFilters;
import com.example.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {
    private final Map<String, RoleAssignment> assignments = new HashMap<>();

    private UserManager userManager;
    private RoleManager roleManager;

    public AssignmentManager() {
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment assignment) {
        if (assignment == null) {
            return;
        }

        if (userManager == null || roleManager == null) {
            throw new NullPointerException("AssignmentManager needs UserManager and RoleManager as dependencies");
        }

        if (!userManager.exists(assignment.user().username())) {
            throw new IllegalArgumentException("user '" + assignment.user().username() + "' does not exist");
        }

        if (!roleManager.exists(assignment.role().getName())) {
            throw new IllegalArgumentException("role '" + assignment.role().getName() + "' does not exist");
        }

        AssignmentFilter assignmentFilterByUser = AssignmentFilters.byUser(assignment.user());
        AssignmentFilter assignmentFilterByRole = AssignmentFilters.byRole(assignment.role());
        AssignmentFilter assignmentFilterByActiveOnly = AssignmentFilters.activeOnly();

        boolean hasActiveDuplicate = assignments.values().stream()
                .filter(assignmentFilterByUser.and(assignmentFilterByRole)::test)
                .anyMatch(assignmentFilterByActiveOnly::test);

        if (hasActiveDuplicate) {
            throw new IllegalStateException(
                    String.format("user '%s' already has active assignment for role '%s'",
                            assignment.user().username(), assignment.role().getName())
            );
        }

        assignments.put(assignment.assignmentId(), assignment);
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        return assignment != null && assignments.remove(assignment.assignmentId(), assignment);
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        return Optional.ofNullable(assignments.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignments.values());
    }

    @Override
    public int count() {
        return assignments.size();
    }

    @Override
    public void clear() {
        assignments.clear();
    }

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        AssignmentFilter assignmentFilterByUser = AssignmentFilters.byUser(user);
        return assignments.values().stream()
                .filter(assignmentFilterByUser::test)
                .toList();
    }

    public List<RoleAssignment> findByRole(Role role) {
        if (role == null) {
            return Collections.emptyList();
        }
        AssignmentFilter assignmentFilterByRole = AssignmentFilters.byRole(role);
        return assignments.values().stream()
                .filter(assignmentFilterByRole::test)
                .toList();
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        return assignments.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        return assignments.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .toList();
    }

    public List<RoleAssignment> getActiveAssignments() {
        return findByFilter(AssignmentFilters.activeOnly());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return findByFilter(AssignmentFilters.inactiveOnly());
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }
        AssignmentFilter assignmentFilterByUser = AssignmentFilters.byUser(user);
        AssignmentFilter assignmentFilterByRole = AssignmentFilters.byRole(role);
        return assignments.values().stream()
                .filter(RoleAssignment::isActive)
                .filter(assignmentFilterByUser::test)
                .anyMatch(assignmentFilterByRole::test);
    }

    public boolean userHasPermission(User user, String permissionName, String resourceName) {
        if (user == null || permissionName == null || resourceName == null) {
            return false;
        }

        return getUserPermissions(user).stream()
                .anyMatch(permission -> permission.matches(permissionName, resourceName));
    }

    public Set<Permission> getUserPermissions(User user) {
        if (user == null) {
            return Collections.emptySet();
        }
        AssignmentFilter assignmentFilterByUser = AssignmentFilters.byUser(user);
        return assignments.values().stream()
                .filter(RoleAssignment::isActive)
                .filter(assignmentFilterByUser::test)
                .flatMap(assignment -> assignment.role().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public void revokeAssignment(String assignmentId) {
        RoleAssignment assignment = findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "assignment with id '" + assignmentId + "' not found"));

        if (assignment instanceof PermanentAssignment) {
            ((PermanentAssignment) assignment).revoke();
        } else if (assignment instanceof TemporaryAssignment) {
            remove(assignment);
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        RoleAssignment assignment = findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "assignment with id '" + assignmentId + "' not found"));

        if ((assignment instanceof TemporaryAssignment temporaryAssignment)) {
            temporaryAssignment.extend(newExpirationDate);
        }
    }
}