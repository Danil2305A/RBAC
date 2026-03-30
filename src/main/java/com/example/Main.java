package com.example;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        int threadCount = 10;
        int progressStepCount = 100;  // грубо говоря, частота обновления шкалы прогресс-бара

        List<Integer> durations = List.of(15000, 5000, 6500, 1000, 9000, 9000, 5000, 6500, 1000, 9000);

        ConsoleManager consoleManager = new ConsoleManager(threadCount);

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            CalculationTask task = new CalculationTask(i, durations.get(i), progressStepCount, consoleManager);
            Thread thread = new Thread(task);
            threads.add(thread);

            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}