package com.example.util;

import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {
    public static String promptString(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String result = scanner.nextLine().trim();

            if (result.isBlank()) {
                System.out.println(("input must not be null or empty"));
                continue;
            }

            return result;
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message);

            if (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid number");
                scanner.nextLine();
                continue;
            }

            int result = scanner.nextInt();
            scanner.nextLine();

            if (result < min || result > max) {
                System.out.printf("Number must be between %d and %d\n", min, max);
                continue;
            }

            return result;
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        System.out.print(message);
        String answer = scanner.nextLine().trim().toLowerCase();

        return answer.equals("y");
    }

    public static int promptChoice(Scanner scanner, String message, List<String> options) {
        int k = 1;
        for (String option : options) {
            System.out.println("\t" + k + ". " + option);
            ++k;
        }

        int i = promptInt(scanner, message, 1, options.size());
        return i - 1;
    }
}
