package com.example.exception;

public class DuplicatedResourceException extends RuntimeException {
    public DuplicatedResourceException(String message) {
        super(message);
    }

    public DuplicatedResourceException(String resource, String field, String value) {
        super(String.format("%s already exists with %s: %s", resource, field, value));
    }
}