package com.example;

public class ConsoleManager {
    private final String[] lines;
    private final Object lock = new Object();

    public ConsoleManager(int threadCount) {
        this.lines = new String[threadCount];

        clearConsole();
        for (int i = 0; i < threadCount; i++) {
            System.out.println();
        }
        System.out.flush();
    }

    private void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void updateLine(int threadIndex, long threadId, String progressBar, long elapsedTime) {
        String timeInfo = elapsedTime > 0 ? String.format("(%d ms)", elapsedTime) : "";
        String newLine = String.format("Thread %2d (ID: %d) %s %s",
                threadIndex + 1, threadId, progressBar, timeInfo);

        synchronized (lock) {
            if (!newLine.equals(lines[threadIndex])) {
                lines[threadIndex] = newLine;
                updateSpecificLine(threadIndex);
            }
        }
    }

    private void updateSpecificLine(int threadIndex) {
        // Сохраняем текущую позицию курсора
        System.out.print("\033[s");

        // Перемещаем курсор на нужную строку
        System.out.printf("\033[%d;1H", threadIndex + 1);

        // Очищаем строку от текущей позиции до конца
        System.out.print("\033[K");

        // Выводим новое содержимое строки
        System.out.print(lines[threadIndex]);

        // Возвращаем курсор на сохранённую позицию
        System.out.print("\033[u");
        System.out.flush();
    }
}