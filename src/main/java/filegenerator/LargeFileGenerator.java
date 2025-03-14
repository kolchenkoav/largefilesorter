package filegenerator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Класс `LargeFileGenerator` предназначен для генерации большого файла,
 * содержащего случайные целые числа, разделенные пробелами.
 * Файл создается с использованием многопоточности для ускорения процесса.
 */
public class LargeFileGenerator {

    private static final String FILE_PATH = "e:/_large_random_numbers_.txt";
    private static final long FILE_SIZE = 100L * 1024 * 1024 * 1024;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private static final int CHUNK_SIZE = 10 * 1024 * 1024; // 10 МБ
    private static final AtomicLong totalWritten = new AtomicLong(0);

    /**
     * Главный метод приложения. Запускает процесс генерации файла и
     * выводит время, затраченное на создание файла.
     * @param args аргументы командной строки (не используются).
     */
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        generateLargeFile();
        long endTime = System.currentTimeMillis();
        System.out.println("\nФайл создан за " + (endTime - startTime) / 1000 + " секунд");
    }

    /**
     * Генерирует большой файл, разбивая его на чанки и записывая их
     * параллельно в нескольких потоках.
     */
    private static void generateLargeFile() {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        // Используем блокирующую очередь, ограничивающую максимальное число задач в очереди
        Semaphore semaphore = new Semaphore(NUM_THREADS * 2);

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(FILE_PATH), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            while (totalWritten.get() < FILE_SIZE) {
                semaphore.acquire(); // Ограничиваем количество задач
                executor.submit(() -> {
                    try {
                        writeChunk(writer);
                    } finally {
                        semaphore.release();
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Записывает чанк данных в файл. Генерирует случайные числа,
     * добавляет их в строку, и затем записывает эту строку в файл.
     * Запись синхронизирована для предотвращения конфликтов.
     * @param writer объект BufferedWriter для записи в файл.
     */
    private static void writeChunk(BufferedWriter writer) {
        Random random = new Random();
        StringBuilder chunkData = new StringBuilder(CHUNK_SIZE);

        while (chunkData.length() < CHUNK_SIZE && totalWritten.get() < FILE_SIZE) {
            chunkData.append(random.nextInt(Integer.MAX_VALUE)).append(" ");
        }

        // Синхронизируем запись в файл
        synchronized (writer) {
            try {
                writer.write(chunkData.toString());
                writer.flush();
                long written = totalWritten.addAndGet(chunkData.length());
                ProgressBar.displayProgressBar(written, FILE_SIZE, 100);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
