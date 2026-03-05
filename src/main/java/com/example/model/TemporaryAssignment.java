package com.example.model;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TemporaryAssignment extends AbstractRoleAssignment {
    private String expiresAt;
    private boolean autoRenew;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               String expiresAt, boolean autoRenew) {
        super(user, role, metadata);

        validateExpirationDate(expiresAt);
        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    protected TemporaryAssignment() {
        super();
    }

    private void validateExpirationDate(String expirationDate) {
        if (expirationDate == null || expirationDate.isBlank()) {
            throw new IllegalArgumentException("expiration date must not be null or blank");
        }

        try {
            ZonedDateTime.parse(expirationDate, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("expiration date must be in format: yyyy-MM-dd HH:mm:ss XXX", e);
        }
    }

    public void revoke() {
        expiresAt = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX"));
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean getAutoRenew() {
        return autoRenew;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void extend(String newExpirationDate) {
        validateExpirationDate(newExpirationDate);
        expiresAt = newExpirationDate;
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    @Override
    public boolean isActive() {
        return !isExpired();
    }

    public boolean isActive(String dateTimeToCompare) {
        validateExpirationDate(dateTimeToCompare);
        return !isExpired(dateTimeToCompare);
    }

    public boolean isExpired() {
        return isExpired(ZonedDateTime.now().format(FORMATTER));
    }

    public boolean isExpired(String dateTimeToCompare) {
        validateExpirationDate(dateTimeToCompare);

        ZonedDateTime zonedDateTimeToCompare = ZonedDateTime.parse(dateTimeToCompare, FORMATTER);
        ZonedDateTime expiryDateTime = ZonedDateTime.parse(expiresAt, FORMATTER);

        return !zonedDateTimeToCompare.isBefore(expiryDateTime);
    }

    public String getTimeRemaining() {
        if (isExpired()) {
            return "0 days 0 hours 0 minutes 0 seconds";
        }

        ZonedDateTime nowDateTime = ZonedDateTime.now();
        ZonedDateTime expiryDateTime = ZonedDateTime.parse(expiresAt, FORMATTER);

        Duration duration = Duration.between(nowDateTime, expiryDateTime);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder remaining = new StringBuilder();

        if (days > 0) {
            remaining.append(days).append(" days ");
        }
        if (hours > 0 || days > 0) {
            remaining.append(hours).append(" hours ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            remaining.append(minutes).append(" minutes ");
        }
        remaining.append(seconds).append(" seconds");

        return remaining.toString();
    }

    @Override
    public String summary() {
        return super.summary() + String.format("\nExpires at: %s", expiresAt);
    }
}