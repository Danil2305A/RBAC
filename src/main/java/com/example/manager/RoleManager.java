package com.example.manager;

import com.example.Permission;
import com.example.Role;
import com.example.RoleAssignment;
import com.example.filter.RoleFilter;
import com.example.filter.RoleFilters;

import java.util.*;

public class RoleManager implements Repository<Role> {
    private final Map<String, Role> rolesWithIdKey = new HashMap<>();
    private final Map<String, Role> rolesWithNameKey = new HashMap<>();

    private AssignmentManager assignmentManager;

    public RoleManager() {
    }

    public void setAssignmentManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role role) {
        if (role != null) {
            rolesWithIdKey.put(role.getId(), role);
            rolesWithNameKey.put(role.getName(), role);
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

        return rolesWithIdKey.remove(role.getId(), role) &&
                rolesWithNameKey.remove(role.getName(), role);
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

    public void addPermissionToRole(String roleName, Permission permission) {
        if (!exists(roleName)) {
            throw new IllegalArgumentException("role with name '" + roleName + "' not found");
        }

        Role existedRole = rolesWithNameKey.get(roleName);

        existedRole.addPermission(permission);

        rolesWithNameKey.put(roleName, existedRole);
        rolesWithIdKey.put(existedRole.getId(), existedRole);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        if (!exists(roleName)) {
            throw new IllegalArgumentException("role with name '" + roleName + "' not found");
        }

        Role existedRole = rolesWithNameKey.get(roleName);

        existedRole.removePermission(permission);

        rolesWithNameKey.put(roleName, existedRole);
        rolesWithIdKey.put(existedRole.getId(), existedRole);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resourceName) {
        return findByFilter(RoleFilters.hasPermission(permissionName, resourceName));
    }
}