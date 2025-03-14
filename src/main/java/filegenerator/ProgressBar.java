package filegenerator;

public class ProgressBar {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public static void displayProgressBar(long progress, long maxProgress, int maxLength) {
        if (progress > maxProgress) {
            progress = maxProgress;
        }

        double percentage = (double) progress / maxProgress * 100;
        String color = percentage < 30 ? ANSI_RED : percentage < 70 ? ANSI_YELLOW : ANSI_GREEN;

        int filledLength = (int) ((percentage / 100) * maxLength);
        int emptyLength = maxLength - filledLength;

        StringBuilder progressBar = new StringBuilder("[");
        for (int i = 0; i < filledLength; i++) {
            progressBar.append("█");
        }
        for (int i = 0; i < emptyLength; i++) {
            progressBar.append(" ");
        }
        progressBar.append("]");

        // Выводим прогресс-бар в той же строке
        System.out.printf(
                "\r%s%s%s %.2f%% (%d/%d Гб)",
                color, progressBar, ANSI_RESET,
                percentage,
                progress / (1024 * 1024 * 1024),
                maxProgress / (1024 * 1024 * 1024)
        );
        System.out.flush(); // Принудительно обновляем консоль
    }
}
