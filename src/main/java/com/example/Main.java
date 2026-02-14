package com.example;

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
    }
}