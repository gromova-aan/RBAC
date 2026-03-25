import java.util.ArrayList;
import java.util.List;

public class UtilTests {
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== ТЕСТИРОВАНИЕ УТИЛИТ ===\n");
        
        testValidationUtils();
        testDateUtils();
        testFormatUtils();
        testConsoleUtils();
        
        System.out.println("\n=== ИТОГИ ТЕСТИРОВАНИЯ ===");
        System.out.println("Пройдено: " + testsPassed);
        System.out.println("Провалено: " + testsFailed);
        System.out.println("Всего тестов: " + (testsPassed + testsFailed));

        System.exit(0);
    }
    
    private static void testValidationUtils() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ VALIDATION UTILS ---");
        
        // Тест isValidUsername
        test("isValidUsername - валидные", () -> {
            check(ValidationUtils.isValidUsername("john_doe"), true, "john_doe");
            check(ValidationUtils.isValidUsername("user123"), true, "user123");
            check(ValidationUtils.isValidUsername("abc"), true, "abc (мин)");
            check(ValidationUtils.isValidUsername("abcdefghijklmnopqrst"), true, "20 символов (макс)");
        });
        
        test("isValidUsername - невалидные", () -> {
            check(ValidationUtils.isValidUsername("ab"), false, "слишком короткий");
            check(ValidationUtils.isValidUsername("abcdefghijklmnopqrstu"), false, "слишком длинный");
            check(ValidationUtils.isValidUsername("user name"), false, "с пробелом");
            check(ValidationUtils.isValidUsername("user@name"), false, "с @");
            check(ValidationUtils.isValidUsername(""), false, "пустая строка");
            check(ValidationUtils.isValidUsername(null), false, "null");
        });
        
        // Тест isValidEmail
        test("isValidEmail - валидные", () -> {
            check(ValidationUtils.isValidEmail("user@example.com"), true, "user@example.com");
            check(ValidationUtils.isValidEmail("user.name@example.com"), true, "user.name@example.com");
            check(ValidationUtils.isValidEmail("user+filter@example.com"), true, "user+filter@example.com");
        });
        
        test("isValidEmail - невалидные", () -> {
            check(ValidationUtils.isValidEmail("user@.com"), false, "user@.com");
            check(ValidationUtils.isValidEmail("user@com"), false, "user@com");
            check(ValidationUtils.isValidEmail("user.example.com"), false, "нет @");
            check(ValidationUtils.isValidEmail(""), false, "пустая");
            check(ValidationUtils.isValidEmail(null), false, "null");
        });
        
        // Тест isValidDate
        test("isValidDate - валидные", () -> {
            check(ValidationUtils.isValidDate("2023-12-31"), true, "2023-12-31");
            check(ValidationUtils.isValidDate("2024-02-29"), true, "2024-02-29 (високосный)");
        });
        
        test("isValidDate - невалидные", () -> {
            check(ValidationUtils.isValidDate("2023-13-01"), false, "неверный месяц");
            check(ValidationUtils.isValidDate("2023-02-30"), false, "30 февраля");
            check(ValidationUtils.isValidDate("2023-02-29"), false, "29 февраля (не високосный)");
            check(ValidationUtils.isValidDate("31-12-2023"), false, "неверный формат");
        });
        
        // Тест normalizeString
        test("normalizeString", () -> {
            check(ValidationUtils.normalizeString("  hello  world  "), "hello world", "удаление пробелов");
            check(ValidationUtils.normalizeString("hello", true), "HELLO", "верхний регистр");
            check(ValidationUtils.normalizeString("HELLO", false), "hello", "нижний регистр");
            check(ValidationUtils.normalizeString(null), "", "null");
        });
    }
    
    private static void testDateUtils() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ DATE UTILS ---");
        
        // Тест getCurrentDate
        test("getCurrentDate", () -> {
            String date = DateUtils.getCurrentDate();
            check(date.matches("\\d{4}-\\d{2}-\\d{2}"), true, "формат YYYY-MM-DD");
        });
        
        // Тест isBefore / isAfter
        test("isBefore / isAfter", () -> {
            check(DateUtils.isBefore("2023-01-01", "2023-12-31"), true, "2023-01-01 раньше 2023-12-31");
            check(DateUtils.isBefore("2023-12-31", "2023-01-01"), false, "2023-12-31 не раньше 2023-01-01");
            check(DateUtils.isAfter("2023-12-31", "2023-01-01"), true, "2023-12-31 позже 2023-01-01");
        });
        
        // Тест addDays
        test("addDays", () -> {
            check(DateUtils.addDays("2023-01-01", 5), "2023-01-06", "+5 дней");
            check(DateUtils.addDays("2023-01-01", -1), "2022-12-31", "-1 день");
        });
        
        // Тест formatRelativeTime
        test("formatRelativeTime", () -> {
            // Сравниваем с текущей датой, поэтому используем addDays
            String today = DateUtils.getCurrentDate();
            String yesterday = DateUtils.addDays(today, -1);
            String tomorrow = DateUtils.addDays(today, 1);
            String nextWeek = DateUtils.addDays(today, 7);
            String lastWeek = DateUtils.addDays(today, -7);
            
            check(DateUtils.formatRelativeTime(today), "сегодня", "сегодня");
            
            // Для вчера проверяем, что строка содержит "назад" (а не точное совпадение)
            String yesterdayResult = DateUtils.formatRelativeTime(yesterday);
            check(yesterdayResult.contains("назад") || yesterdayResult.contains("вчера"), true, "вчера: " + yesterdayResult);
            
            // Для завтра проверяем, что строка содержит "завтра" или "через"
            String tomorrowResult = DateUtils.formatRelativeTime(tomorrow);
            check(tomorrowResult.contains("завтра") || tomorrowResult.contains("через"), true, "завтра: " + tomorrowResult);
            
            // Для следующей недели
            String nextWeekResult = DateUtils.formatRelativeTime(nextWeek);
            check(nextWeekResult.contains("через"), true, "через неделю: " + nextWeekResult);
            
            // Для прошлой недели
            String lastWeekResult = DateUtils.formatRelativeTime(lastWeek);
            check(lastWeekResult.contains("назад"), true, "неделю назад: " + lastWeekResult);
        });
    }
    
    private static void testFormatUtils() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ FORMAT UTILS ---");
        
        // Тест padRight / padLeft
        test("padRight / padLeft", () -> {
            check(FormatUtils.padRight("test", 8), "test    ", "padRight");
            check(FormatUtils.padLeft("test", 8), "    test", "padLeft");
        });
        
        // Тест truncate
        test("truncate", () -> {
            check(FormatUtils.truncate("Hello World", 8), "Hello...", "обрезание");
            check(FormatUtils.truncate("Hello", 10), "Hello", "не обрезается");
        });
        
        // Тест formatHeader
        test("formatHeader", () -> {
            String header = FormatUtils.formatHeader("TEST");
            check(header.contains("TEST"), true, "заголовок содержит текст");
            check(header.contains("+"), true, "заголовок имеет рамку");
        });
        
        // Тест formatTable
        test("formatTable", () -> {
            String[] headers = {"NAME", "AGE"};
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"John", "25"});
            rows.add(new String[]{"Jane", "30"});
            
            String table = FormatUtils.formatTable(headers, rows);
            check(table.contains("NAME"), true, "таблица содержит заголовки");
            check(table.contains("John"), true, "таблица содержит данные");
            check(table.contains("+"), true, "таблица имеет рамку");
        });
        
        // Тест formatBox
        test("formatBox", () -> {
            String box = FormatUtils.formatBox("Test");
            check(box.contains("Test"), true, "бокс содержит текст");
            check(box.contains("+"), true, "бокс имеет рамку");
        });
    }
    
    private static void testConsoleUtils() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ CONSOLE UTILS ---");
        
        // Тест requireNonNullEmpty
        test("requireNonNullEmpty", () -> {
            try {
                ValidationUtils.requireNonNullEmpty("test", "field");
                check(true, true, "валидная строка");
            } catch (Exception e) {
                check(false, true, "валидная строка (ошибка)");
            }
            
            try {
                ValidationUtils.requireNonNullEmpty("", "field");
                check(false, true, "пустая строка (должна быть ошибка)");
            } catch (IllegalArgumentException e) {
                check(true, true, "пустая строка (ошибка перехвачена)");
            }
        });
        
        // Тест requireInRange
        test("requireInRange", () -> {
            try {
                ValidationUtils.requireInRange(5, 1, 10, "field");
                check(true, true, "число в диапазоне");
            } catch (Exception e) {
                check(false, true, "число в диапазоне (ошибка)");
            }
            
            try {
                ValidationUtils.requireInRange(15, 1, 10, "field");
                check(false, true, "число вне диапазона (должна быть ошибка)");
            } catch (IllegalArgumentException e) {
                check(true, true, "число вне диапазона (ошибка перехвачена)");
            }
        });
    }
    
    // Вспомогательные методы для тестов
    private static void test(String testName, Runnable testLogic) {
        System.out.print("Тест " + testName + " ... \n");
        try {
            testLogic.run();
        } catch (Exception e) {
            System.out.println("ОШИБКА: " + e.getMessage());
            testsFailed++;
        }
    }
    
    private static void check(boolean actual, boolean expected, String message) {
        if (actual == expected) {
            System.out.println("  OK " + message);
            testsPassed++;
        } else {
            System.out.println("  FAIL " + message + " (ожидалось " + expected + ", получено " + actual + ")");
            testsFailed++;
        }
    }
    
    private static void check(String actual, String expected, String message) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  OK " + message);
            testsPassed++;
        } else {
            System.out.println("  FAIL " + message + " (ожидалось '" + expected + "', получено '" + actual + "')");
            testsFailed++;
        }
    }
    
    private static void check(boolean actual, String message) {
        if (actual) {
            System.out.println("  OK " + message);
            testsPassed++;
        } else {
            System.out.println("  FAIL " + message);
            testsFailed++;
        }
    }
}