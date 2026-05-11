package com.example.command;

import com.example.model.AbstractRoleAssignment;
import com.example.model.Role;
import com.example.model.User;

import java.util.List;

public record RBACSystemData(
        List<User> users,
        List<Role> roles,
        List<AbstractRoleAssignment> assignments) {
}