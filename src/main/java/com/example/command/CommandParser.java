package com.example.command;

import com.example.exception.ResourceNotFoundException;
import com.example.util.FormatUtils;

import java.util.*;

public class CommandParser {
    private final Map<String, Command> commands = new HashMap<>();
    private final Map<String, String> commandDescriptions = new HashMap<>();

    public void registerCommand(String name, String description, Command command) {
        commands.put(name, command);
        commandDescriptions.put(name, description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        if (!commands.containsKey(commandName)) {
            throw new ResourceNotFoundException(String.format("command '%s' not found in command registry", commandName));
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
        String[] headers = {"Command", "Description"};
        List<String[]> rows = new ArrayList<>();
        new TreeMap<>(commandDescriptions)
                .forEach((name, description) -> {
                    rows.add(new String[]{name, description});
                });
        System.out.println(FormatUtils.formatTable(headers, rows));
    }
}
