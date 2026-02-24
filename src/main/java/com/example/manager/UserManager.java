package com.example.manager;

import com.example.model.User;
import com.example.filter.UserFilter;
import com.example.filter.UserFilters;

import java.util.*;

public class UserManager implements Repository<User> {
    private final Map<String, User> users = new HashMap<>();

    @Override
    public void add(User user) {
        if (user != null && !users.containsKey(user.username())) {
            users.put(user.username(), user);
        }
    }

    @Override
    public boolean remove(User user) {
        if (user == null) {
            return false;
        }
        return users.remove(user.username(), user);
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id)); // id как ключ???
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        users.clear();
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public Optional<User> findByEmail(String email) {
        UserFilter userFilter = UserFilters.byEmail(email);
        return users.values().stream()
                .filter(userFilter::test)
                .findFirst();
    }

    public List<User> findByFilter(UserFilter filter) {
        return users.values().stream()
                .filter(filter::test)
                .toList();
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        return users.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .toList();
    }

    public boolean exists(String username) {
        return username != null && users.containsKey(username.trim());
    }

    public void update(String username, String newFullName, String newEmail) {
        if (!exists(username)) {
            throw new IllegalArgumentException("user with username '" + username + "' not found");
        }

        User updatedUser = User.validate(username, newFullName, newEmail);
        users.put(username, updatedUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(users);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        UserManager otherUserManager = (UserManager) other;
        return Objects.equals(users, otherUserManager.users);
    }
}