package com.example.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "assignmentType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PermanentAssignment.class, name = "PERMANENT"),
        @JsonSubTypes.Type(value = TemporaryAssignment.class, name = "TEMPORARY")
})
public abstract class AbstractRoleAssignment implements RoleAssignment {
    private final String assignmentId;
    private final User user;
    private final Role role;
    private final AssignmentMetadata metadata;

    public AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {
        validateUser(user);
        validateRole(role);
        validateMetadata(metadata);

        this.assignmentId = "assignment_" + UUID.randomUUID();
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }

    // Пустой конструктор для Jackson
    protected AbstractRoleAssignment() {
        this.assignmentId = null;
        this.user = null;
        this.role = null;
        this.metadata = null;
    }

    // Геттеры (нужны для сериализации)
    public String getAssignmentId() {
        return assignmentId;
    }

    public User getUser() {
        return user;
    }

    public Role getRole() {
        return role;
    }

    public AssignmentMetadata getMetadata() {
        return metadata;
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
    }

    private void validateRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
    }

    private void validateMetadata(AssignmentMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
    }

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }

    @Override
    public abstract boolean isActive();

    @Override
    public abstract String assignmentType();

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        AbstractRoleAssignment otherRoleAssignment = (AbstractRoleAssignment) other;
        return Objects.equals(assignmentId, otherRoleAssignment.assignmentId());
    }

    public String summary() {
        return String.format("Assignment ID: %s\n", assignmentId) +
                String.format("[%s] %s assigned to %s by %s at %s\n",
                        assignmentType(), role.getName(), user.username(),
                        metadata.assignedBy(), metadata.assignedAt()) +
                String.format("Reason: %s\n", metadata.reason()) +
                String.format("Status: %s", isActive() ? "ACTIVE" : "NOT ACTIVE");
    }
}