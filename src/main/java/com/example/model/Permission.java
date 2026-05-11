package com.example.model;

import static com.example.util.ValidationUtils.*;

public record Permission(String name, String resource, String description) {
    public Permission(String name, String resource, String description) {
        name = normalizeString(name);
        resource = normalizeString(resource);
        description = normalizeString(description);

        validatePermissionName(name);
        validatePermissionResource(resource);
        validatePermissionDescription(description);

        this.name = name.toUpperCase();
        this.resource = resource.toLowerCase();
        this.description = description;
    }

    public String format() {
        return String.format("%s on %s: %s", name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        namePattern = namePattern != null ? namePattern.toUpperCase() : null;
        resourcePattern = resourcePattern != null ? resourcePattern.toLowerCase() : null;

        boolean np = namePattern == null || name.contains(namePattern) || name.matches(namePattern);
        boolean rp = resourcePattern == null || resource.contains(resourcePattern) || resource.matches(resourcePattern);

        return np && rp;
    }
}