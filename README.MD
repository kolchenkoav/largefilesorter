# LargeFileSorter

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)


## 📌 Описание проекта
**LargeFileSorter** — это высокопроизводительный инструмент для сортировки больших файлов, содержащих числа.  
Программа использует многопоточный подход и алгоритм внешней сортировки, 
что позволяет эффективно обрабатывать файлы размером в десятки и сотни гигабайт.   
Для проверки работы можно сформировать файл 100 Гб с помощью программы `filegenerator.LargeFileGenerator.java`   
[Алгоритм формирования файла случайных чисел](https://github.com/kolchenkoav/largefilesorter/blob/master/%D0%90%D0%BB%D0%B3%D0%BE%D1%80%D0%B8%D1%82%D0%BC%20%D1%84%D0%BE%D1%80%D0%BC%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D1%8F%20%D1%84%D0%B0%D0%B9%D0%BB%D0%B0%20%D1%81%D0%BB%D1%83%D1%87%D0%B0%D0%B9%D0%BD%D1%8B%D1%85%20%D1%87%D0%B8%D1%81%D0%B5%D0%BB.md)   
[Алгоритм работы сортировки](https://github.com/kolchenkoav/largefilesorter/blob/master/%D0%90%D0%BB%D0%B3%D0%BE%D1%80%D0%B8%D1%82%D0%BC%20%D1%80%D0%B0%D0%B1%D0%BE%D1%82%D1%8B.md)

## 🚀 Возможности
- ⚡ **Многопоточная обработка** — ускоряет процесс сортировки.
- 📂 **Обработка больших файлов** — эффективно работает даже с файлами, превышающими объем оперативной памяти.
- 🔥 **Оптимизированное чтение и запись** — использование буферизированного ввода-вывода.
- 📊 **Прогресс-бар** — отображает ход выполнения сортировки.

---

## 📦 Структура проекта
```plaintext
LargeFileSorter
├── src/main/java
│   ├── LargeFileSorter.java  # Основной класс
│   │     ├─ ProgressMonitor.java               # Мониторинг прогресса
│   │     └─ FileReaderEntry.java               # Вспомогательный класс для чтения файлов   
│   └── filegenerator           
│               ├─ LargeFileGenerator.java      # Генератор большого файла
│               └─ ProgressBar.java             # Мониторинг прогресса
├── README.md                                   # Описание проекта
└── Алгоритм работы.md      
```

---

## 🛠 Требования проекта
- **Java 17+**
- **Maven** (для сборки проекта)
- **Операционная система**: Windows/Linux/MacOS

---

## 📖 Как запустить проект

### 1️⃣ Клонирование репозитория
```bash
git clone https://github.com/yourusername/LargeFileSorter.git
cd LargeFileSorter
```

### 2️⃣ Сборка проекта
```bash
mvn clean package
```

### 3️⃣ Запуск сортировки
```bash
java -jar target/LargeFileSorter.jar
```

---

## 📝 Конфигурация

Файл конфигурации задается в `LargeFileSorter.java`. Основные параметры:
```java
private static final String INPUT_FILE = "e:/large_random_numbers2.txt"; // Исходный файл
private static final String OUTPUT_FILE = "e:/sorted_numbers-big2.txt"; // Итоговый файл
private static final String OUTPUT_DIR = "e:/temp_chunks/"; // Папка для временных файлов
private static final int CHUNK_SIZE = 10_000_000; // Размер чанка (количество чисел в одном чанке)
private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors(); // Количество потоков
```

---

## 🔗 API Эндпоинты (если планируется REST API)

| Метод | URL | Описание |
|--------|----------------|-------------|
| `GET` | `/status` | Возвращает текущий статус сортировки |
| `POST` | `/sort` | Запускает процесс сортировки |
| `GET` | `/results` | Получает ссылку на отсортированный файл |

---

## 📜 Лицензия
Этот проект распространяется под лицензией MIT. Подробнее см. [LICENSE](LICENSE).

---

## 👨‍💻 Автор
**Ваше Имя**  
📧 Email: your.email@example.com  
🔗 GitHub: [yourusername](https://github.com/yourusername)

---

## 🤝 Поддержка проекта
Если вам понравился проект, поставьте ⭐ на [GitHub](https://github.com/yourusername/LargeFileSorter)!

