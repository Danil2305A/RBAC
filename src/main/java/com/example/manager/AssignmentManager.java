package com.example.manager;

import com.example.exception.DuplicatedResourceException;
import com.example.exception.ResourceNotFoundException;
import com.example.filter.AssignmentFilter;
import com.example.filter.AssignmentFilters;
import com.example.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {
    private final Map<String, RoleAssignment> assignments = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

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
            throw new ResourceNotFoundException("user '" + assignment.user().username() + "' does not exist");
        }

        if (!roleManager.exists(assignment.role().getName())) {
            throw new ResourceNotFoundException("role '" + assignment.role().getName() + "' does not exist");
        }

        AssignmentFilter assignmentFilterByUser = AssignmentFilters.byUser(assignment.user());
        AssignmentFilter assignmentFilterByRole = AssignmentFilters.byRole(assignment.role());
        AssignmentFilter assignmentFilterByActiveOnly = AssignmentFilters.activeOnly();

        lock.writeLock().lock();
        try {
            boolean hasActiveDuplicate = assignments.values().stream()
                    .filter(assignmentFilterByUser.and(assignmentFilterByRole)::test)
                    .anyMatch(assignmentFilterByActiveOnly::test);

            if (hasActiveDuplicate) {
                throw new DuplicatedResourceException(
                        String.format("user '%s' already has active assignment for role '%s'",
                                assignment.user().username(), assignment.role().getName())
                );
            }

            assignments.put(assignment.assignmentId(), assignment);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) {
            return false;
        }
        lock.writeLock().lock();
        try {
            return assignments.remove(assignment.assignmentId(), assignment);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(assignments.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<RoleAssignment> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(assignments.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return assignments.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            assignments.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        AssignmentFilter assignmentFilterByUser = AssignmentFilters.byUser(user);
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(assignmentFilterByUser::test)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByRole(Role role) {
        if (role == null) {
            return Collections.emptyList();
        }
        AssignmentFilter assignmentFilterByRole = AssignmentFilters.byRole(role);
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(assignmentFilterByRole::test)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }


    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(filter::test)
                    .sorted(sorter)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(RoleAssignment::isActive)
                    .filter(assignmentFilterByUser::test)
                    .anyMatch(assignmentFilterByRole::test);
        } finally {
            lock.readLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(RoleAssignment::isActive)
                    .filter(assignmentFilterByUser::test)
                    .flatMap(assignment -> assignment.role().getPermissions().stream())
                    .collect(Collectors.toSet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void revokeAssignment(String assignmentId) {
        lock.writeLock().lock();
        try {
            RoleAssignment assignment = findById(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("assignment", "id", assignmentId));

            if (assignment instanceof PermanentAssignment) {
                ((PermanentAssignment) assignment).revoke();
            } else if (assignment instanceof TemporaryAssignment) {
                ((TemporaryAssignment) assignment).revoke();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        lock.writeLock().lock();
        try {
            RoleAssignment assignment = findById(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("assignment", "id", assignmentId));

            if (assignment instanceof TemporaryAssignment temporaryAssignment) {
                temporaryAssignment.extend(newExpirationDate);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<RoleAssignment> findByFilterParallel(AssignmentFilter filter) {
        lock.readLock().lock();
        try {
            return assignments.values().parallelStream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findAllParallel(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        lock.readLock().lock();
        try {
            return assignments.values().parallelStream()
                    .filter(filter::test)
                    .sorted(sorter)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }
}