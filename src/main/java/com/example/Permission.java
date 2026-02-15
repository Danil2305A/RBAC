package com.example;

public record Permission(String name, String resource, String description) {
    public Permission(String name, String resource, String description) {
        name = name != null ? name.trim() : null;
        resource = resource != null ? resource.trim() : null;
        description = description != null ? description.trim() : null;

        validateName(name);
        validateResource(resource);
        validateDescription(description);

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

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("permission name must not be null or blank");
        }

        if (name.contains(" ")) {
            throw new IllegalArgumentException("permission name must not contain spaces");
        }
    }

    private void validateResource(String resource) {
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("permission resource must not be null or blank");
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("permission description must not be null or blank");
        }
    }
}