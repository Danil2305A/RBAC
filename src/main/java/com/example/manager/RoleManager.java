package com.example.manager;

import com.example.exception.ResourceNotFoundException;
import com.example.model.Permission;
import com.example.model.Role;
import com.example.model.RoleAssignment;
import com.example.filter.RoleFilter;
import com.example.filter.RoleFilters;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RoleManager implements Repository<Role> {
    private final Map<String, Role> rolesWithIdKey = new ConcurrentHashMap<>();
    private final Map<String, Role> rolesWithNameKey = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private AssignmentManager assignmentManager;

    public RoleManager() {
    }

    public void setAssignmentManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role role) {
        if (role != null) {
            lock.writeLock().lock();
            try {
                rolesWithIdKey.put(role.getId(), role);
                rolesWithNameKey.put(role.getName(), role);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public boolean remove(Role role) {
        if (role == null) {
            return false;
        }

        if (assignmentManager == null) {
            throw new NullPointerException("RoleManager needs AssignmentManager as dependency");
        }

        List<RoleAssignment> assignments = assignmentManager.findByRole(role);
        if (!assignments.isEmpty()) {
            return false;
        }

        lock.writeLock().lock();
        try {
            role.getUsedNames().remove(role.getName());
            return rolesWithIdKey.remove(role.getId(), role) && rolesWithNameKey.remove(role.getName(), role);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Role> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(rolesWithIdKey.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Role> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(rolesWithIdKey.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return rolesWithIdKey.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            rolesWithIdKey.clear();
            rolesWithNameKey.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<Role> findByName(String name) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(rolesWithNameKey.get(name));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findByFilter(RoleFilter filter) {
        lock.readLock().lock();
        try {
            return rolesWithIdKey.values().stream().filter(filter::test).toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        lock.readLock().lock();
        try {
            return rolesWithIdKey.values().stream().filter(filter::test).sorted(sorter).toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean exists(String name) {
        lock.readLock().lock();
        try {
            return name != null && rolesWithNameKey.containsKey(name.trim());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void update(String roleName, String newRoleName, String newDescription) {
        lock.writeLock().lock();
        try {
            if (!exists(roleName)) {
                throw new ResourceNotFoundException("role", "name", roleName);
            }

            Role updatingRole = rolesWithNameKey.get(roleName);

            updatingRole.setName(newRoleName);
            updatingRole.setDescription(newDescription);

            rolesWithNameKey.remove(roleName);
            add(updatingRole);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        lock.writeLock().lock();
        try {
            if (!exists(roleName)) {
                throw new ResourceNotFoundException("role", "name", roleName);
            }

            Role existedRole = rolesWithNameKey.get(roleName);
            existedRole.addPermission(permission);
            rolesWithNameKey.put(roleName, existedRole);
            rolesWithIdKey.put(existedRole.getId(), existedRole);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        lock.writeLock().lock();
        try {
            if (!exists(roleName)) {
                throw new ResourceNotFoundException("role", "name", roleName);
            }

            Role existedRole = rolesWithNameKey.get(roleName);
            existedRole.removePermission(permission);
            rolesWithNameKey.put(roleName, existedRole);
            rolesWithIdKey.put(existedRole.getId(), existedRole);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Role> findRolesWithPermission(String permissionName, String resourceName) {
        return findByFilter(RoleFilters.hasPermission(permissionName, resourceName));
    }

    public List<Role> findByFilterParallel(RoleFilter filter) {
        lock.readLock().lock();
        try {
            return rolesWithIdKey.values().parallelStream().filter(filter::test).toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findAllParallel(RoleFilter filter, Comparator<Role> sorter) {
        lock.readLock().lock();
        try {
            return rolesWithIdKey.values().parallelStream().filter(filter::test).sorted(sorter).toList();
        } finally {
            lock.readLock().unlock();
        }
    }
}