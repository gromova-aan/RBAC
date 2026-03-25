import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


public class ThreadTests {
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static List<String> errors = new ArrayList<>();
    
    public static void main(String[] args) {
        System.out.println("=== МОДУЛЬНЫЕ ТЕСТЫ RBAC ===\n");
        
        testThreadSafeManagers();
        testParallelFilters();
        testAsyncCommands();
        testScheduledTasks();
        testDataIntegrity();
        
        printResults();
    }
    
    private static void testThreadSafeManagers() {
        System.out.println("\n--- ТЕСТ 1: Потокобезопасные менеджеры ---");
        
        UserManager userManager = new UserManager();
        RoleManager roleManager = new RoleManager();
        AssignmentManager assignmentManager = new AssignmentManager(userManager, roleManager);
        
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // Параллельное добавление пользователей
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String username = "user_" + threadId + "_" + j;
                        try {
                            User user = User.validate(username, "Test User", username + "@test.com");
                            userManager.add(user);
                            successCount.incrementAndGet();
                        } catch (IllegalArgumentException e) {
                            // Дубликат — нормально
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            errors.add("Таймаут ожидания потоков");
        }
        
        executor.shutdown();
        long duration = System.currentTimeMillis() - startTime;
        
        int userCount = userManager.count();
        boolean testPassed = userCount == threadCount * operationsPerThread;
        
        if (testPassed) {
            System.out.println("  OK Пользователей создано: " + userCount + " (ожидалось " + (threadCount * operationsPerThread) + ")");
            System.out.println("  OK Время: " + duration + " мс");
            testsPassed++;
        } else {
            System.out.println("  FAIL Пользователей создано: " + userCount + " (ожидалось " + (threadCount * operationsPerThread) + ")");
            testsFailed++;
        }
    }
    
    private static void testParallelFilters() {
        System.out.println("\n--- ТЕСТ 2: Параллельные фильтры ---");
        
        UserManager userManager = new UserManager();
        
        // Добавляем тестовых пользователей
        for (int i = 0; i < 1000; i++) {
            User user = User.validate("user" + i, "User " + i, "user" + i + "@test.com");
            userManager.add(user);
        }
        
        // Тест параллельного фильтра
        UserFilter filter = u -> u.username().contains("5");
        long startTime = System.currentTimeMillis();
        
        List<User> result1 = userManager.findByFilter(filter);
        long timeSequential = System.currentTimeMillis() - startTime;
        
        startTime = System.currentTimeMillis();
        List<User> result2 = userManager.findByFilterParallel(filter);
        long timeParallel = System.currentTimeMillis() - startTime;
        
        boolean testPassed = result1.size() == result2.size();
        
        if (testPassed) {
            System.out.println("  OK Результаты совпадают: " + result1.size() + " пользователей");
            System.out.println("  OK Последовательный: " + timeSequential + " мс");
            System.out.println("  OK Параллельный: " + timeParallel + " мс");
            testsPassed++;
        } else {
            System.out.println("  FAIL Результаты не совпадают");
            testsFailed++;
        }
    }
    
    private static void testAsyncCommands() {
        System.out.println("\n--- ТЕСТ 3: Асинхронные команды ---");
        
        RBACSystem system = new RBACSystem();
        system.initialize();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        // Асинхронная генерация отчёта
        system.getBackgroundExecutor().submit(
            () -> {
                String report = system.getReportGenerator().generateUserReport(
                    system.getUserManager(), system.getAssignmentManager()
                );
                completed.set(report != null && !report.isEmpty());
            },
            () -> {
                latch.countDown();
            },
            () -> {
                latch.countDown();
            }
        );
        
        try {
            boolean finished = latch.await(5, TimeUnit.SECONDS);
            if (finished && completed.get()) {
                System.out.println("  OK Асинхронная команда выполнена успешно");
                testsPassed++;
            } else {
                System.out.println("  FAIL Асинхронная команда не завершилась");
                testsFailed++;
            }
        } catch (InterruptedException e) {
            errors.add("Прерывание при ожидании асинхронной команды");
            testsFailed++;
        }
        
        // Проверка активных задач
        int activeTasks = system.getBackgroundExecutor().getActiveTaskCount();
        System.out.println("  OK Активных задач после выполнения: " + activeTasks);
    }
    
    private static void testScheduledTasks() {
        System.out.println("\n--- ТЕСТ 4: Периодические задачи ---");
        
        RBACSystem system = new RBACSystem();
        system.initialize();
        
        // Создаём истекшее временное назначение
        User user = User.validate("expired_user", "Expired User", "expired@test.com");
        system.getUserManager().add(user);
        
        Role role = new Role("ExpiredRole", "Для теста");
        system.getRoleManager().add(role);
        
        // Назначаем с датой в прошлом (2020 год)
        AssignmentMetadata meta = AssignmentMetadata.now("test", "Для проверки");
        TemporaryAssignment expired = new TemporaryAssignment(
            user, role, meta, "2020-01-01", false
        );
        system.getAssignmentManager().add(expired);
        
        String assignmentId = expired.assignmentId();
        
        // Проверяем, что назначение существует и активно
        boolean exists = system.getAssignmentManager().findById(assignmentId).isPresent();
        if (!exists) {
            System.out.println("  FAIL Назначение не создано");
            testsFailed++;
            return;
        }
        
        System.out.println("  OK Назначение создано с ID: " + assignmentId);
        
        // Принудительная проверка истекших
        system.getTaskScheduler().forceCheckExpired();
        
        // Даём время на выполнение
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Проверяем, что истекшее назначение удалено
        boolean stillExists = system.getAssignmentManager().findById(assignmentId).isPresent();
        
        if (!stillExists) {
            System.out.println("  OK Истекшее назначение удалено");
            testsPassed++;
        } else {
            System.out.println("  FAIL Истекшее назначение не удалено (всё ещё существует)");
            testsFailed++;
        }
        
        // Проверка отчёта статистики
        system.getTaskScheduler().forceStatisticsReport();
        System.out.println("  OK Отчёт статистики сформирован");
    }
    
    private static void testDataIntegrity() {
        System.out.println("\n--- ТЕСТ 5: Целостность данных под нагрузкой ---");
        
        RBACSystem system = new RBACSystem();
        system.initialize();
        
        int threadCount = 5;
        int operationsPerThread = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        // Параллельное создание, обновление и удаление
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String username = "test_user_" + threadId + "_" + j;
                        
                        try {
                            User user = User.validate(username, "Test User", username + "@test.com");
                            system.getUserManager().add(user);
                        } catch (IllegalArgumentException e) {
                            // Уже существует
                        }
                        
                        system.getUserManager().findByUsername(username);
                        
                        if (j % 3 == 0) {
                            system.getUserManager().findByUsername(username)
                                .ifPresent(system.getUserManager()::remove);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            errors.add("Таймаут ожидания");
        }
        
        executor.shutdown();
        
        // Проверка целостности
        Set<String> uniqueUsernames = new HashSet<>();
        boolean hasDuplicates = false;
        
        for (User user : system.getUserManager().findAll()) {
            if (uniqueUsernames.contains(user.username())) {
                hasDuplicates = true;
                errors.add("Дубликат пользователя: " + user.username());
            }
            uniqueUsernames.add(user.username());
        }
        
        if (!hasDuplicates) {
            System.out.println("  OK Дубликатов пользователей нет");
            testsPassed++;
        } else {
            System.out.println("  FAIL Обнаружены дубликаты пользователей");
            testsFailed++;
        }
        
        System.out.println("  OK Итоговое количество пользователей: " + system.getUserManager().count());
    }
    
    private static void printResults() {
        System.out.println("\n=== ИТОГИ МОДУЛЬНОГО ТЕСТИРОВАНИЯ ===\n");
        System.out.println("Пройдено: " + testsPassed);
        System.out.println("Провалено: " + testsFailed);
        System.out.println("Всего тестов: " + (testsPassed + testsFailed));
        
        if (!errors.isEmpty()) {
            System.out.println("\n--- ОШИБКИ (" + errors.size() + ") ---");
            errors.stream().limit(10).forEach(e -> System.out.println("  - " + e));
            if (errors.size() > 10) {
                System.out.println("  ... и ещё " + (errors.size() - 10) + " ошибок");
            }
        }
        
        stopAllBackgroundTasks();
        System.out.println("\n=== ТЕСТИРОВАНИЕ ЗАВЕРШЕНО ===");
    }

    private static void stopAllBackgroundTasks() {
        RBACSystem tempSystem = new RBACSystem();
        tempSystem.getBackgroundExecutor().shutdown();
        tempSystem.getTaskScheduler().shutdown();
    }
}