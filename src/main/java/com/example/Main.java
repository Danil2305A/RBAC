package com.example;

import com.example.command.CommandParser;
import com.example.command.CommandRegistry;
import com.example.command.RBACSystem;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        RBACSystem system = new RBACSystem();
        system.initialize();

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAllCommands(parser);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\nEnter the command: ");
            try {
                String input = scanner.nextLine();

                parser.parseAndExecute(input, scanner, system);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}