package com.example.model;

import static com.example.util.ValidationUtils.*;

public record User(String username, String fullName, String email) {
    public static User validate(String username, String fullName, String email) {
        username = normalizeString(username);
        fullName = normalizeString(fullName);
        email = normalizeString(email);

        validateUsername(username);
        validateFullName(fullName);
        validateEmail(email);

        return new User(username, fullName, email);
    }

    public String format() {
        return String.format("%s (%s) <%s>", username, fullName, email);
    }
}