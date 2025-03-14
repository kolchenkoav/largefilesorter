import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Класс для сортировки больших файлов с числами.
 */
public class LargeFileSorter {
    private static final String INPUT_FILE = "e:/large_random_numbers2.txt";
    private static final String OUTPUT_FILE = "e:/sorted_numbers-big2.txt";
    private static final String OUTPUT_DIR = "e:/temp_chunks/";
    private static final int CHUNK_SIZE = 10_000_000;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    private final AtomicLong progressCounter = new AtomicLong(0);
    private final AtomicLong realCountElements = new AtomicLong(0);

    /**
     * Основной метод для запуска сортировки файла.
     *
     * @param args аргументы командной строки (не используются).
     */
    public static void main(String[] args) {
        printStaticFinalInfo();
        new LargeFileSorter().run();
    }

    /**
     * Выводит информацию о статических константах.
     */
    public static void printStaticFinalInfo() {
        System.out.println();
        System.out.println("Исходный файл: " + INPUT_FILE);
        System.out.println("Итоговый отсортированный файл: " + OUTPUT_FILE);
        System.out.println("Временные файлы: " + OUTPUT_DIR);
        System.out.println("Размер чанка (в байтах): " + CHUNK_SIZE);
        System.out.println("Количество потоков: " + NUM_THREADS);
    }

    /**
     * Запускает процесс сортировки файла.
     */
    public void run() {
        long startTime = System.currentTimeMillis();

        long totalElements = countNumbers();
        System.out.println("Оценочное количество чисел в файле ~ " + totalElements);
        System.out.println();

        ProgressMonitor monitor = new ProgressMonitor(totalElements, progressCounter);
        Thread progressThread = new Thread(monitor);
        progressThread.start();

        try {
            System.out.println("Sorting blocks: ");
            List<File> chunkFiles = measureTime(() -> sortChunks());

            monitor.stop();
            progressThread.join();


            progressCounter.set(0);
            monitor = new ProgressMonitor(realCountElements.get() , progressCounter);
            progressThread = new Thread(monitor);
            progressThread.start();

            System.out.println("Merging files: ");
            measureTime(() -> {
                try {
                    mergeFiles(chunkFiles);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            monitor.stop();
            progressThread.join();


            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println();
            System.out.println("Total time: " + formatTime(totalTime));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Подсчитывает количество чисел в исходном файле.
     *
     * @return приблизительное количество чисел.
     */
    private long countNumbers() {
        File file = new File(INPUT_FILE);
        long fileSizeInBytes = file.length();

        double avgNumberSize = 10.4836;

        return (long) (fileSizeInBytes / avgNumberSize);
    }

    /**
     * Сортирует чанки файла.
     *
     * @return список временных файлов с отсортированными чанками.
     * @throws Exception если произошла ошибка ввода-вывода.
     */
    private List<File> sortChunks() throws Exception {
        List<File> chunkFiles = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE), 8192 * 1024)) {
            List<Integer> numbers = new ArrayList<>();
            StringBuilder numBuffer = new StringBuilder();
            int c;

            while ((c = reader.read()) != -1) {
                if (Character.isWhitespace(c)) {
                    if (numBuffer.length() > 0) {
                        numbers.add(Integer.parseInt(numBuffer.toString()));
                        progressCounter.incrementAndGet();
                        realCountElements.incrementAndGet();
                        numBuffer.setLength(0);
                    }
                    if (numbers.size() >= CHUNK_SIZE) {
                        submitChunk(executor, new ArrayList<>(numbers), chunkFiles);
                        numbers.clear();
                    }
                } else {
                    numBuffer.append((char) c);
                }
            }

            if (numBuffer.length() > 0) {
                numbers.add(Integer.parseInt(numBuffer.toString()));
                progressCounter.incrementAndGet();
                realCountElements.incrementAndGet();
            }

            if (!numbers.isEmpty()) {
                submitChunk(executor, numbers, chunkFiles);
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
        return chunkFiles;
    }

    /**
     * Отправляет чанк на сортировку в отдельном потоке.
     *
     * @param executor сервис для управления потоками.
     * @param chunk чанк чисел для сортировки.
     * @param chunkFiles список временных файлов.
     */
    private void submitChunk(ExecutorService executor, List<Integer> chunk, List<File> chunkFiles) {
        executor.submit(() -> {
            chunk.sort(Integer::compareTo);
            File tempFile = new File(OUTPUT_DIR + "chunk-" + UUID.randomUUID() + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile), 8 * 1024 * 1024)) {
                for (int i = 0; i < chunk.size(); i++) {
                    writer.write(chunk.get(i).toString());
                    if (i < chunk.size() - 1) writer.write(" ");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            chunkFiles.add(tempFile);
        });
    }

    /**
     * Объединяет временные файлы в один отсортированный файл.
     *
     * @param chunkFiles список временных файлов.
     * @throws Exception если произошла ошибка ввода-вывода.
     */
    private void mergeFiles(List<File> chunkFiles) throws Exception {
        PriorityQueue<FileReaderEntry> pq = new PriorityQueue<>();
        Queue<File> fileQueue = new LinkedList<>(chunkFiles);
        int maxOpenFiles = Math.min(500, chunkFiles.size());

        for (int i = 0; i < maxOpenFiles; i++) {
            File file = fileQueue.poll();
            if (file != null) {
                FileReaderEntry entry = new FileReaderEntry(file);
                if (entry.currentNumber != null) pq.add(entry);
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE), 8 * 1024 * 1024)) {
            while (!pq.isEmpty()) {
                FileReaderEntry entry = pq.poll();
                writer.write(entry.currentNumber.toString());
                progressCounter.incrementAndGet();
                writer.write(" ");

                if (entry.nextNumber()) {
                    pq.add(entry);
                } else {
                    entry.close();
                    if (!fileQueue.isEmpty()) {
                        File nextFile = fileQueue.poll();
                        FileReaderEntry newEntry = new FileReaderEntry(nextFile);
                        if (newEntry.currentNumber != null) pq.add(newEntry);
                    }
                }
            }
        }

        for (File file : chunkFiles) {
            if (!file.delete()) {
                System.err.println("Failed to delete: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Вспомогательный класс для чтения чисел из файла.
     */
    static class FileReaderEntry implements Comparable<FileReaderEntry> {
        private final BufferedReader reader;
        Integer currentNumber;
        private final StringBuilder numBuffer = new StringBuilder();
        private int nextChar = -1;

        /**
         * Конструктор.
         *
         * @param file файл для чтения.
         * @throws IOException если произошла ошибка ввода-вывода.
         */
        FileReaderEntry(File file) throws IOException {
            this.reader = new BufferedReader(new FileReader(file), 1024 * 1024);
            nextNumber();
        }

        /**
         * Читает следующее число из файла.
         *
         * @return true, если число успешно прочитано, иначе false.
         * @throws IOException если произошла ошибка ввода-вывода.
         */
        boolean nextNumber() throws IOException {
            numBuffer.setLength(0);
            while (true) {
                if (nextChar == -1) nextChar = reader.read();
                if (nextChar == -1) {
                    currentNumber = null;
                    return false;
                }
                if (Character.isWhitespace(nextChar)) {
                    nextChar = -1;
                    continue;
                }
                break;
            }

            while (nextChar != -1 && !Character.isWhitespace(nextChar)) {
                numBuffer.append((char) nextChar);
                nextChar = reader.read();
            }

            currentNumber = Integer.parseInt(numBuffer.toString());
            return true;
        }

        /**
         * Сравнивает текущее число с числом другого объекта.
         *
         * @param other другой объект для сравнения.
         * @return результат сравнения.
         */
        @Override
        public int compareTo(FileReaderEntry other) {
            return this.currentNumber.compareTo(other.currentNumber);
        }

        /**
         * Закрывает ридер.
         *
         * @throws IOException если произошла ошибка ввода-вывода.
         */
        void close() throws IOException {
            reader.close();
        }
    }

    /**
     * Измеряет время выполнения задачи.
     *
     * @param task задача для измерения времени.
     * @param <T> тип возвращаемого значения задачи.
     * @return результат выполнения задачи.
     * @throws Exception если произошла ошибка.
     */
    private <T> T measureTime(Callable<T> task) throws Exception {
        long start = System.currentTimeMillis();
        T result = task.call();
        System.out.printf("[%s]%n", formatTime(System.currentTimeMillis() - start));
        return result;
    }

    /**
     * Измеряет время выполнения задачи.
     *
     * @param task задача для измерения времени.
     */
    private void measureTime(Runnable task) {
        long start = System.currentTimeMillis();
        task.run();
        System.out.printf("[%s]%n", formatTime(System.currentTimeMillis() - start));
    }

    /**
     * Форматирует время в строку.
     *
     * @param millis время в миллисекундах.
     * @return форматированная строка времени.
     */
    private String formatTime(long millis) {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % 60,
                TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
    }

    /**
     * Класс для мониторинга прогресса выполнения задачи.
     */
    static class ProgressMonitor implements Runnable {
        private final long total;
        private final AtomicLong progress;
        private volatile boolean running = true;

        /**
         * Конструктор.
         *
         * @param total общее количество элементов.
         * @param progress счетчик прогресса.
         */
        ProgressMonitor(long total, AtomicLong progress) {
            this.total = total;
            this.progress = progress;
        }

        /**
         * Останавливает мониторинг.
         */
        void stop() {
            running = false;
        }

        /**
         * Запускает мониторинг прогресса.
         */
        @Override
        public void run() {
            if (total == 0) return;
            long startTime = System.currentTimeMillis();
            while (running && !Thread.currentThread().isInterrupted()) {
                long current = progress.get();
                double percent = (current * 100.0) / total;
                double logPercent = Math.log(percent + 1) / Math.log(101) * 100;
                System.out.printf("\rProgress: %-50s %.2f%% (%d/%d)",
                        progressBar((int)percent), logPercent, current, total);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println();
        }

        /**
         * Создает строку прогресс-бара с логарифмической шкалой.
         *
         * @param percent процент выполнения задачи.
         * @return строка прогресс-бара.
         */
        private String progressBar(int percent) {
            int totalBars = 50;
            double logPercent = Math.log(percent + 1) / Math.log(101) * 100;
            int completedBars = (int) (logPercent / 2);
            StringBuilder sb = new StringBuilder(totalBars + 2);

            sb.append('[');
            for (int i = 0; i < totalBars; i++) {
                if (i < completedBars) {
                    sb.append('#');
                } else {
                    sb.append(' ');
                }
            }
            sb.append(']');

            return sb.toString();
        }
    }
}