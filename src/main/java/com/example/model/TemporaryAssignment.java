package com.example.model;

import java.time.Duration;
import java.time.ZonedDateTime;

import static com.example.util.DateTimeUtils.getCurrentDateTime;
import static com.example.util.DateTimeUtils.isBefore;
import static com.example.util.ValidationUtils.DATE_TIME_FORMATTER;
import static com.example.util.ValidationUtils.validateExpirationDate;

public class TemporaryAssignment extends AbstractRoleAssignment {
    private String expiresAt;
    private boolean autoRenew;

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

    public void revoke() {
        expiresAt = getCurrentDateTime();
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean getAutoRenew() {
        return autoRenew;
    }

    public void setExpiresAt(String expiresAt) {
        validateExpirationDate(expiresAt);
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
        return isExpired(getCurrentDateTime());
    }

    public boolean isExpired(String dateTimeToCompare) {
        validateExpirationDate(dateTimeToCompare);

        return !isBefore(dateTimeToCompare, expiresAt);
    }

    public String getTimeRemaining() {
        if (isExpired()) {
            return "0 days 0 hours 0 minutes 0 seconds";
        }

        ZonedDateTime nowDateTime = ZonedDateTime.now();
        ZonedDateTime expiryDateTime = ZonedDateTime.parse(expiresAt, DATE_TIME_FORMATTER);

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