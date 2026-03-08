package com.example.model;

import com.example.exception.DuplicatedResourceException;

import java.util.*;

import static com.example.util.ValidationUtils.*;

public class Role {
    private String id;
    private String name;
    private String description;
    private Set<Permission> permissions;

    private static final Set<String> usedNames = new HashSet<>();

    public Role(String name, String description, Set<Permission> permissions) {
        name = normalizeString(name);
        description = normalizeString(description);

        validateRoleName(name);

        if (usedNames.contains(name)) {
            throw new DuplicatedResourceException(String.format("role with name '%s' already exists", name));
        }
        usedNames.add(name);

        validateRoleDescription(description);
        validateRolePermissions(permissions);

        this.id = "role_" + UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.permissions = permissions;
    }

    public Set<String> getUsedNames() {
        return usedNames;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        name = normalizeString(name);
        validateRoleName(name);

        usedNames.remove(this.name);
        this.name = name;
    }

    public void setDescription(String description) {
        description = normalizeString(description);
        validateRoleDescription(description);

        this.description = description;
    }

    public void setPermissions(Set<Permission> permissions) {
        validateRolePermissions(permissions);

        this.permissions = permissions;
    }

    public void addPermission(Permission permission) {
        if (permission != null) {
            permissions.add(permission);
        }
    }

    public void removePermission(Permission permission) {
        if (permission != null) {
            permissions.remove(permission);
        }
    }

    public boolean hasPermission(Permission permission) {
        return permission != null && permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String permissionResource) {
        if (permissionName == null || permissionResource == null) {
            return false;
        }

        return permissions.stream()
                .anyMatch(permission -> permission.matches(permissionName, permissionResource));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Role otherRole = (Role) other;
        return Objects.equals(id, otherRole.id);
    }

    @Override
    public String toString() {
        return format();
    }

    public String format() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Role: %s [ID: %s]\n", name, id));
        sb.append(String.format("Description: %s\n", description));
        sb.append(String.format("Permissions (%d):\n", permissions.size()));

        for (Permission permission : permissions) {
            sb.append("  - ").append(permission.format()).append("\n");
        }

        return sb.toString().trim();
    }
}