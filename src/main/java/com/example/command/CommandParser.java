package com.example.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {
    private final Map<String, Command> commands = new HashMap<>();
    private final Map<String, String> commandDescriptions = new HashMap<>();

    public void registerCommand(String name, String description, Command command) {
        commands.put(name, command);
        commandDescriptions.put(name, description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        if (!commands.containsKey(commandName)) {
            throw new IllegalArgumentException(String.format("command '%s' not found in command registry", commandName));
        }
        commands.get(commandName).execute(scanner, system);
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("input must not be null or empty");
        }

        String[] tokens = input.split("\\s+");
        String commandName = tokens[0];

        executeCommand(commandName, scanner, system);
    }

    public void printHelp() {
        commandDescriptions.forEach(
                (name, description) -> System.out.printf("%s: %s\n", name, description));
    }
}
