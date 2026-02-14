package com.example;

import java.util.Set;

public class Main {
    public static void main(String[] args) {
        User user1 = User.validate("  akdante", "Akusko Danil Dmitrievich", "akusko.dan@yandex.ru");
        System.out.println(user1.format());

        User user2 = User.validate(" akdante ", "Akusko Danil Dmitrievich  ", " akusko.dan@dev123.sibsutis.ru");
        System.out.println(user2.format());

        try {
            User.validate("ak", "Akusko Danil Dmitrievich", "akusko.dan@yandex.ru");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        try {
            User.validate("матушка земля", "Akusko Danil Dmitrievich", "akusko.dan@yandex.ru");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        try {
            User.validate("ak_dante23", "  ", "akusko.dan@yandex.ru");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        try {
            User.validate("akdante", null, "akusko.dan@yandex.ru");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        try {
            User.validate("akdante", "Akusko Danil Dmitrievich", "akusko.danyandex.ru");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        try {
            User.validate("akdante", "Akusko Danil Dmitrievich", "akusko.dan@yandexru");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        System.out.println();

        Permission permission = new Permission(" Read", "Users ", " Can view user list");
        System.out.println(permission.format());
        System.out.println(permission.matches("RE.*", "us.*"));

        System.out.println();

        Set<Permission> permissions = Set.of(
                permission,
                new Permission("Write", "Users", "Can add user to list"),
                new Permission("Delete", "Users", "Can delete user from list")
        );

        Role admin = new Role(" Administrator", "Full access ", permissions);
        System.out.println(admin);
        System.out.println(admin.hasPermission("READ", "us.*"));
        try {
            new Role("Administrator", "Full access", permissions);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        System.out.println();

        AssignmentMetadata metadata = AssignmentMetadata.now(" Ivanov Ivan", "I don't know ");
        System.out.println(metadata.format());

        System.out.println();

        PermanentAssignment permanentRole = new PermanentAssignment(user1, admin, metadata);
        System.out.println(permanentRole.summary());
        permanentRole.revoke();
        System.out.println(permanentRole.summary());
    }
}