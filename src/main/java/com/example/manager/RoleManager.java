package com.example.manager;

import com.example.exception.ResourceNotFoundException;
import com.example.model.Permission;
import com.example.model.Role;
import com.example.model.RoleAssignment;
import com.example.filter.RoleFilter;
import com.example.filter.RoleFilters;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RoleManager implements Repository<Role> {
    private final ConcurrentMap<String, Role> rolesWithIdKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Role> rolesWithNameKey = new ConcurrentHashMap<>();

    private AssignmentManager assignmentManager;

    public RoleManager() {
    }

    public void setAssignmentManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role role) {
        if (role == null) {
            return;
        }

        Role existing = rolesWithNameKey.putIfAbsent(role.getName(), role);
        if (existing != null) {
            return;
        }
        rolesWithIdKey.put(role.getId(), role);
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

        role.getUsedNames().remove(role.getName());

        boolean removedFromName = rolesWithNameKey.remove(role.getName(), role);
        boolean removedFromId = rolesWithIdKey.remove(role.getId(), role);

        return removedFromName && removedFromId;
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(rolesWithIdKey.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesWithIdKey.values());
    }

    @Override
    public int count() {
        return rolesWithIdKey.size();
    }

    @Override
    public void clear() {
        rolesWithIdKey.clear();
        rolesWithNameKey.clear();
    }

    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(rolesWithNameKey.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter) {
        return rolesWithIdKey.values().stream()
                .filter(filter::test)
                .toList();
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        return rolesWithIdKey.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .toList();
    }

    public boolean exists(String name) {
        return name != null && rolesWithNameKey.containsKey(name.trim());
    }

    public void update(String roleName, String newRoleName, String newDescription) {
        if (!exists(roleName)) {
            throw new ResourceNotFoundException("role", "name", roleName);
        }

        Role updatingRole = rolesWithNameKey.get(roleName);

        updatingRole.setName(newRoleName);
        updatingRole.setDescription(newDescription);

        rolesWithNameKey.remove(roleName);
        add(updatingRole);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        if (!exists(roleName)) {
            throw new ResourceNotFoundException("role", "name", roleName);
        }

        Role existedRole = rolesWithNameKey.get(roleName);
        existedRole.addPermission(permission);
        rolesWithNameKey.put(roleName, existedRole);
        rolesWithIdKey.put(existedRole.getId(), existedRole);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        if (!exists(roleName)) {
            throw new ResourceNotFoundException("role", "name", roleName);
        }

        Role existedRole = rolesWithNameKey.get(roleName);
        existedRole.removePermission(permission);
        rolesWithNameKey.put(roleName, existedRole);
        rolesWithIdKey.put(existedRole.getId(), existedRole);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resourceName) {
        return findByFilter(RoleFilters.hasPermission(permissionName, resourceName));
    }

    public List<Role> findByFilterParallel(RoleFilter filter) {
        return rolesWithIdKey.values().parallelStream().filter(filter::test).toList();
    }

    public List<Role> findAllParallel(RoleFilter filter, Comparator<Role> sorter) {
        return rolesWithIdKey.values().parallelStream().filter(filter::test).sorted(sorter).toList();
    }
}