package com.example.util;

import com.example.model.Permission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {
    @Test
    @DisplayName("Успешная валидация username")
    void validateUsername_ValidUsername_ShouldPass() {
        assertDoesNotThrow(() -> ValidationUtils.validateUsername("john_doe123"));
        assertDoesNotThrow(() -> ValidationUtils.validateUsername("abc"));
        assertDoesNotThrow(() -> ValidationUtils.validateUsername("a1234567890123456789"));
        assertDoesNotThrow(() -> ValidationUtils.validateUsername("USER_NAME"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Провальная валидация при null или пустом username")
    void validateUsername_NullOrBlank_ShouldThrowException(String username) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validateUsername(username));
        assertEquals("username must not be null or blank", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "a", "a very long username that exceeds twenty characters",
            "user@name", "user-name", "user.name"})
    @DisplayName("Провальная валидация username при несоответствии с шаблоном")
    void validateUsername_InvalidPattern_ShouldThrowException(String username) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validateUsername(username));
        assertEquals("username must contain only latin letters, digits, underscores and his length must be between 3 and 20 symbols",
                exception.getMessage());
    }

    @Test
    @DisplayName("Успешная валидация fullName")
    void validateFullName_ValidFullName_ShouldPass() {
        assertDoesNotThrow(() -> ValidationUtils.validateFullName("John Doe"));
        assertDoesNotThrow(() -> ValidationUtils.validateFullName("Alice"));
        assertDoesNotThrow(() -> ValidationUtils.validateFullName("Bob Smith Jr."));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Провальная валидация при null или пустом fullName")
    void validateFullName_NullOrBlank_ShouldThrowException(String fullName) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validateFullName(fullName));
        assertEquals("user full name must not be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Успешная валидация email")
    void validateEmail_ValidEmail_ShouldPass() {
        assertDoesNotThrow(() -> ValidationUtils.validateEmail("user@example.com"));
        assertDoesNotThrow(() -> ValidationUtils.validateEmail("john.doe@company.co.uk"));
        assertDoesNotThrow(() -> ValidationUtils.validateEmail("user+label@gmail.com"));
        assertDoesNotThrow(() -> ValidationUtils.validateEmail("user-name@domain.org"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Провальная валидация при null или пустом email")
    void validateEmail_NullOrBlank_ShouldThrowException(String email) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validateEmail(email));
        assertEquals("user email must not be null or blank", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "user@", "@domain.com", "user@domain",
            "user@domain.c", "user name@domain.com", "user@.com"})
    @DisplayName("Провальная валидация email при несоответствии с шаблоном")
    void validateEmail_InvalidPattern_ShouldThrowException(String email) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validateEmail(email));
        assertEquals("user email must match the basic format", exception.getMessage());
    }

    @Test
    @DisplayName("Успешная валидация permission name")
    void validatePermissionName_ValidName_ShouldPass() {
        assertDoesNotThrow(() -> ValidationUtils.validatePermissionName("READ"));
        assertDoesNotThrow(() -> ValidationUtils.validatePermissionName("WRITE_PRIVATE"));
        assertDoesNotThrow(() -> ValidationUtils.validatePermissionName("USER:CREATE"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Провальная валидация при null или пустом permission name")
    void validatePermissionName_NullOrBlank_ShouldThrowException(String name) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validatePermissionName(name));
        assertEquals("permission name must not be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Должно выброситься исключение при наличии внутренних пробелов в permission name")
    void validatePermissionName_WithSpaces_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validatePermissionName("READ WRITE"));
        assertEquals("permission name must not contain spaces", exception.getMessage());
    }

    @Test
    @DisplayName("Успешная валидация permission resource")
    void validatePermissionResource_ValidResource_ShouldPass() {
        assertDoesNotThrow(() -> ValidationUtils.validatePermissionResource("/api/users"));
        assertDoesNotThrow(() -> ValidationUtils.validatePermissionResource("file:document.pdf"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Провальная валидация при null или пустом permission resource")
    void validatePermissionResource_NullOrBlank_ShouldThrowException(String resource) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validatePermissionResource(resource));
        assertEquals("permission resource must not be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Успешная валидация permission description")
    void validatePermissionDescription_ValidDescription_ShouldPass() {
        assertDoesNotThrow(() -> ValidationUtils.validatePermissionDescription("Read access to user data"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Провальная валидация при null или пустом permission description")
    void validatePermissionDescription_NullOrBlank_ShouldThrowException(String description) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validatePermissionDescription(description));
        assertEquals("permission description must not be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Успешная валидация role name")
    void validateRoleName_ValidName_ShouldPass() {
        assertDoesNotThrow(() -> ValidationUtils.validateRoleName("ADMIN"));
        assertDoesNotThrow(() -> ValidationUtils.validateRoleName("USER_MANAGER"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Провальная валидация при null или пустом role name")
    void validateRoleName_NullOrBlank_ShouldThrowException(String name) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validateRoleName(name));
        assertEquals("role name must not be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Успешная валидация role description")
    void validateRoleDescription_ValidDescription_ShouldPass() {
        assertDoesNotThrow(() -> ValidationUtils.validateRoleDescription("Administrator role with full access"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Провальная валидация при null или пустом role description")
    void validateRoleDescription_NullOrBlank_ShouldThrowException(String description) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validateRoleDescription(description));
        assertEquals("role description must not be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Успешная валидация non-null permissions")
    void validateRolePermissions_NonNullSet_ShouldPass() {
        Set<Permission> emptyPermissions = new HashSet<>();
        assertDoesNotThrow(() -> ValidationUtils.validateRolePermissions(emptyPermissions));

        Set<Permission> permissionsWithData = new HashSet<>();
        permissionsWithData.add(new Permission("READ", "users", "test"));
        assertDoesNotThrow(() -> ValidationUtils.validateRolePermissions(permissionsWithData));
    }

    @Test
    @DisplayName("Должно выброситься исключение при null permissions")
    void validateRolePermissions_NullSet_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validateRolePermissions(null));
        assertEquals("role permissions must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Успешная валидация assigned by")
    void validateAssignedBy_ValidValue_ShouldPass() {
        assertDoesNotThrow(() -> ValidationUtils.validateAssignedBy("admin"));
        assertDoesNotThrow(() -> ValidationUtils.validateAssignedBy("user123"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Провальная валидация при null или пустом assigned by")
    void validateAssignedBy_NullOrBlank_ShouldThrowException(String assignedBy) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validateAssignedBy(assignedBy));
        assertEquals("assigned by must not be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Успешная валидация expiration date")
    void validateExpirationDate_ValidDate_ShouldPass() {
        assertDoesNotThrow(() -> ValidationUtils.validateExpirationDate("2024-12-31 23:59:59 +02:00"));
        assertDoesNotThrow(() -> ValidationUtils.validateExpirationDate("2024-01-01 00:00:00 Z"));
        assertDoesNotThrow(() -> ValidationUtils.validateExpirationDate("2024-06-15 14:30:00 -04:00"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Провальная валидация при null или пустом expiration date")
    void validateExpirationDate_NullOrBlank_ShouldThrowException(String date) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validateExpirationDate(date));
        assertEquals("expiration date must not be null or blank", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-12-31", "2024/12/31 23:59:59", "31-12-2024",
            "2024-12-31 23:59:59", "invalid date", "2024-13-31 23:59:59 +02:00"})
    @DisplayName("Должно выброситься исключение при невалидном по формату expiration date")
    void validateExpirationDate_InvalidFormat_ShouldThrowException(String date) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validateExpirationDate(date));
        assertEquals("expiration date must be in format: yyyy-MM-dd HH:mm:ss XXX",
                exception.getMessage());
        assertInstanceOf(DateTimeParseException.class, exception.getCause());
    }

    @Test
    @DisplayName("Успешная нормализация строки с удалением whitespace")
    void normalizeString_WithWhitespace_ShouldTrim() {
        assertEquals("hello", ValidationUtils.normalizeString("  hello  "));
        assertEquals("hello world", ValidationUtils.normalizeString("  hello world  "));
        assertEquals("test", ValidationUtils.normalizeString("\t\ntest\n\t"));
    }

    @Test
    @DisplayName("Должен вернуть null при null input")
    void normalizeString_NullInput_ShouldReturnNull() {
        assertNull(ValidationUtils.normalizeString(null));
    }

    @Test
    @DisplayName("Должен вернуть пустую строку, если она состоит только из пробелов")
    void normalizeString_WhitespaceOnly_ShouldReturnEmpty() {
        assertEquals("", ValidationUtils.normalizeString("   "));
        assertEquals("", ValidationUtils.normalizeString("\t\n"));
    }

    @Test
    @DisplayName("Успешная проверка requireNonBlank")
    void requireNonBlank_ValidValues_ShouldPass() {
        assertDoesNotThrow(() -> ValidationUtils.requireNonBlank("valid", "field"));
        assertDoesNotThrow(() -> ValidationUtils.requireNonBlank("  valid with spaces  ", "field"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Должно выброситься исключение при неуспешной проверке requireNonBlank")
    void requireNonBlank_InvalidValues_ShouldThrowException(String value) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonBlank(value, "testField"));
        assertEquals("testField must not be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("DATE_TIME_FORMATTER should be properly configured")
    void dateTimeFormatter_ShouldParseAndFormatCorrectly() {
        ZonedDateTime now = ZonedDateTime.now();
        String formatted = now.format(ValidationUtils.DATE_TIME_FORMATTER);
        assertDoesNotThrow(() -> ZonedDateTime.parse(formatted, ValidationUtils.DATE_TIME_FORMATTER));
    }
}