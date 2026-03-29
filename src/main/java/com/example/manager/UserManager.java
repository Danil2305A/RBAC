package com.example.manager;

import com.example.exception.DuplicatedResourceException;
import com.example.exception.ResourceNotFoundException;
import com.example.model.User;
import com.example.filter.UserFilter;
import com.example.filter.UserFilters;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UserManager implements Repository<User> {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void add(User user) {
        lock.writeLock().lock();
        try {
            if (users.containsKey(user.username())) {
                throw new DuplicatedResourceException("User", "username", user.username());
            }
            users.put(user.username(), user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(User user) {
        if (user == null) {
            return false;
        }
        lock.writeLock().lock();
        try {
            return users.remove(user.username(), user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<User> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(users.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<User> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(users.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return users.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            users.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<User> findByUsername(String username) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(users.get(username));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<User> findByEmail(String email) {
        lock.readLock().lock();
        try {
            UserFilter userFilter = UserFilters.byEmail(email);
            return users.values().stream()
                    .filter(userFilter::test)
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findByFilter(UserFilter filter) {
        lock.readLock().lock();
        try {
            return users.values().stream()
                    .filter(filter::test)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        lock.readLock().lock();
        try {
            return users.values().stream()
                    .filter(filter::test)
                    .sorted(sorter)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean exists(String username) {
        lock.readLock().lock();
        try {
            return username != null && users.containsKey(username.trim());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void update(String username, String newFullName, String newEmail) {
        lock.writeLock().lock();
        try {
            if (!exists(username)) {
                throw new ResourceNotFoundException("user", "username", username);
            }

            User updatedUser = User.validate(username, newFullName, newEmail);
            users.put(username, updatedUser);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<User> findByFilterParallel(UserFilter filter) {
        lock.readLock().lock();
        try {
            return users.values().parallelStream()
                    .filter(filter::test)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findAllParallel(UserFilter filter, Comparator<User> sorter) {
        lock.readLock().lock();
        try {
            return users.values().parallelStream()
                    .filter(filter::test)
                    .sorted(sorter)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
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