package com.example;

public class CalculationTask implements Runnable {
    private final int threadIndex;
    private final int duration;
    private final int progressStepCount;
    private final ConsoleManager consoleManager;
    private final ProgressBar progressBar;

    public CalculationTask(int threadIndex, int duration, int progressStepCount, ConsoleManager consoleManager) {
        this.threadIndex = threadIndex;
        this.duration = duration;
        this.progressStepCount = progressStepCount;
        this.consoleManager = consoleManager;
        this.progressBar = new ProgressBar(progressStepCount);
    }

    @Override
    public void run() {
        long threadId = Thread.currentThread().threadId();

        int stepDuration = duration / progressStepCount; // Время на один шаг прогресса

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < progressStepCount; i++) {
            try {
                Thread.sleep(stepDuration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            progressBar.increment();

            consoleManager.updateLine(threadIndex, threadId, progressBar.render(), 0);
        }

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        consoleManager.updateLine(threadIndex, threadId, progressBar.render(), elapsedTime);
    }
}