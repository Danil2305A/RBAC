package com.example.command;

import java.util.Scanner;

@FunctionalInterface
public interface Command {
    void execute(String[] args, Scanner scanner, RBACSystem system);
}