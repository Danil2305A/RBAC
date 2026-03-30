package com.example;

public class ProgressBar {
    private static final char PROGRESS_CHAR = '█';
    private static final char EMPTY_CHAR = ' ';
    private static final int BAR_LENGTH = 100;

    private int currentStep;
    private final int totalSteps;

    public ProgressBar(int totalSteps) {
        this.totalSteps = totalSteps;
        this.currentStep = 0;
    }

    public void increment() {
        if (currentStep < totalSteps) {
            currentStep++;
        }
    }

    public String render() {
        int progressLength = (int) ((double) currentStep / totalSteps * BAR_LENGTH);
        int percentage = (int) ((double) currentStep / totalSteps * 100);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < BAR_LENGTH; i++) {
            bar.append(i < progressLength ? PROGRESS_CHAR : EMPTY_CHAR);
        }
        bar.append(String.format("] %3d%%", percentage));

        return bar.toString();
    }
}