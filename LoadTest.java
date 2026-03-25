import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class LoadTest {
    
    private static final int THREAD_COUNT = 10;          // количество потоков
    private static final int OPERATIONS_PER_THREAD = 50; // операций на поток
    private static final int TEST_DURATION_SECONDS = 30; // длительность теста
    
    private static RBACSystem system;
    private static AtomicInteger totalOperations = new AtomicInteger(0);
    private static AtomicInteger successOperations = new AtomicInteger(0);
    private static AtomicInteger failedOperations = new AtomicInteger(0);
    private static List<String> errors = new CopyOnWriteArrayList<>();
    
    public static void main(String[] args) {
        System.out.println("=== НАГРУЗОЧНОЕ ТЕСТИРОВАНИЕ RBAC ===\n");
        
        // Инициализация системы
        system = new RBACSystem();
        system.initialize();
        
        // Запуск тестов
        testConcurrentOperations();
        testConcurrentFilters();
        testConcurrentAssignments();
        
        // Итоги
        printResults();
    }
    
    private static void testConcurrentOperations() {
        System.out.println("\n--- ТЕСТ 1: Параллельное создание пользователей и ролей ---");
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        // Создаём пользователя с уникальным именем
                        String username = "user_" + threadId + "_" + j;
                        if (username.length() > 20) {
                            username = username.substring(0, 20);
                        }
                        try {
                            User user = User.validate(username, "Test User " + username, username + "@test.com");
                            system.getUserManager().add(user);
                            totalOperations.incrementAndGet();
                            successOperations.incrementAndGet();
                        } catch (IllegalArgumentException e) {
                            // Логируем только если ошибка не из-за дубликата
                            if (!e.getMessage().contains("уже существует")) {
                                failedOperations.incrementAndGet();
                                errors.add("Ошибка создания пользователя: " + e.getMessage());
                            }
                        }
                        
                        // Создаём роль с уникальным именем
                        String roleName = "role_" + threadId + "_" + j + "_" + System.currentTimeMillis();
                        try {
                            Role role = new Role(roleName, "Тестовая роль " + roleName);
                            system.getRoleManager().add(role);
                            totalOperations.incrementAndGet();
                            successOperations.incrementAndGet();
                        } catch (IllegalArgumentException e) {
                            failedOperations.incrementAndGet();
                            errors.add("Ошибка создания роли: " + e.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        executor.shutdown();
        
        System.out.println("   Создано пользователей: " + system.getUserManager().count());
        System.out.println("   Создано ролей: " + system.getRoleManager().count());
        System.out.println("   Всего операций: " + totalOperations.get());
        System.out.println("   Успешно: " + successOperations.get());
        System.out.println("   Провалено: " + failedOperations.get());
        System.out.println("   Время: " + duration + " мс");
    }
    
    private static void testConcurrentFilters() {
        System.out.println("\n--- ТЕСТ 2: Параллельные фильтры и поиски ---");
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        long startTime = System.currentTimeMillis();
        AtomicInteger filterResults = new AtomicInteger(0);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        // Фильтр по username
                        UserFilter usernameFilter = user -> user.username().contains("user");
                        List<User> users = system.getUserManager().findByFilterParallel(usernameFilter);
                        filterResults.addAndGet(users.size());
                        totalOperations.incrementAndGet();
                        successOperations.incrementAndGet();
                        
                        // Фильтр по роли
                        RoleFilter roleFilter = role -> role.getPermissions().size() > 0;
                        List<Role> roles = system.getRoleManager().findByFilterParallel(roleFilter);
                        filterResults.addAndGet(roles.size());
                        totalOperations.incrementAndGet();
                        successOperations.incrementAndGet();
                        
                        // Поиск по email
                        Optional<User> userByEmail = system.getUserManager().findByEmail("test@test.com");
                        if (userByEmail.isPresent()) {
                            filterResults.incrementAndGet();
                        }
                        totalOperations.incrementAndGet();
                        successOperations.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    errors.add("Ошибка фильтрации: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        executor.shutdown();
        
        System.out.println("   Операций фильтрации: " + totalOperations.get());
        System.out.println("   Результатов фильтрации: " + filterResults.get());
        System.out.println("   Время: " + duration + " мс");
    }
    
    private static void testConcurrentAssignments() {
        System.out.println("\n--- ТЕСТ 3: Параллельные назначения ролей ---");
        
        // Создаём список пользователей и ролей для назначений
        List<User> users = system.getUserManager().findAll();
        List<Role> roles = system.getRoleManager().findAll();
        
        if (users.isEmpty() || roles.isEmpty()) {
            System.out.println("  ✗ Нет пользователей или ролей для теста");
            return;
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        long startTime = System.currentTimeMillis();
        AtomicInteger assignmentSuccess = new AtomicInteger(0);
        AtomicInteger assignmentFailed = new AtomicInteger(0);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD / 2; j++) {
                        // Выбираем случайного пользователя и роль
                        User randomUser = users.get(new Random().nextInt(users.size()));
                        Role randomRole = roles.get(new Random().nextInt(roles.size()));
                        
                        try {
                            // Создаём назначение
                            AssignmentMetadata meta = AssignmentMetadata.now("loadtest", "Нагрузочный тест");
                            PermanentAssignment assignment = new PermanentAssignment(randomUser, randomRole, meta);
                            system.getAssignmentManager().add(assignment);
                            assignmentSuccess.incrementAndGet();
                            totalOperations.incrementAndGet();
                            successOperations.incrementAndGet();
                        } catch (IllegalArgumentException e) {
                            assignmentFailed.incrementAndGet();
                            failedOperations.incrementAndGet();
                            // Дубликаты — это нормально, не считаем ошибкой
                            if (!e.getMessage().contains("уже есть активное назначение")) {
                                errors.add("Ошибка назначения: " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    errors.add("Ошибка в потоке: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        executor.shutdown();
        
        System.out.println("   Успешных назначений: " + assignmentSuccess.get());
        System.out.println("   Проваленных (дубликаты): " + assignmentFailed.get());
        System.out.println("   Всего назначений в системе: " + system.getAssignmentManager().count());
        System.out.println("   Время: " + duration + " мс");
    }
    
    private static void checkDataIntegrity() {
        System.out.println("\n--- ПРОВЕРКА ЦЕЛОСТНОСТИ ДАННЫХ ---");
        
        // Проверка пользователей
        Set<String> usernames = new HashSet<>();
        boolean userDuplicate = false;
        for (User user : system.getUserManager().findAll()) {
            if (usernames.contains(user.username())) {
                userDuplicate = true;
                errors.add("Дубликат пользователя: " + user.username());
            }
            usernames.add(user.username());
        }
        
        // Проверка ролей
        Set<String> roleNames = new HashSet<>();
        boolean roleDuplicate = false;
        for (Role role : system.getRoleManager().findAll()) {
            if (roleNames.contains(role.getName())) {
                roleDuplicate = true;
                errors.add("Дубликат роли: " + role.getName());
            }
            roleNames.add(role.getName());
        }
        
        // Проверка назначений
        Set<String> assignmentIds = new HashSet<>();
        boolean assignmentDuplicate = false;
        for (RoleAssignment assignment : system.getAssignmentManager().findAll()) {
            if (assignmentIds.contains(assignment.assignmentId())) {
                assignmentDuplicate = true;
                errors.add("Дубликат назначения: " + assignment.assignmentId());
            }
            assignmentIds.add(assignment.assignmentId());
        }
        
        System.out.println("   Пользователей: " + usernames.size() + " (дубликатов: " + (userDuplicate ? "ЕСТЬ" : "нет") + ")");
        System.out.println("   Ролей: " + roleNames.size() + " (дубликатов: " + (roleDuplicate ? "ЕСТЬ" : "нет") + ")");
        System.out.println("   Назначений: " + assignmentIds.size() + " (дубликатов: " + (assignmentDuplicate ? "ЕСТЬ" : "нет") + ")");
        
        if (!userDuplicate && !roleDuplicate && !assignmentDuplicate) {
            System.out.println("  OK Целостность данных не нарушена!");
        } else {
            System.out.println("  NO Обнаружены нарушения целостности данных!");
        }
    }
    
    private static void printResults() {
        System.out.println("\n=== ИТОГИ НАГРУЗОЧНОГО ТЕСТИРОВАНИЯ ===\n");
        
        checkDataIntegrity();
        
        System.out.println("\n--- СТАТИСТИКА ---");
        System.out.println("Всего операций: " + totalOperations.get());
        System.out.println("Успешно: " + successOperations.get());
        System.out.println("Провалено: " + failedOperations.get());
        
        if (!errors.isEmpty()) {
            System.out.println("\n--- ОШИБКИ (" + errors.size() + ") ---");
            errors.stream().limit(10).forEach(e -> System.out.println("  - " + e));
            if (errors.size() > 10) {
                System.out.println("  ... и ещё " + (errors.size() - 10) + " ошибок");
            }
        }
        
        System.out.println("\n=== ТЕСТ ЗАВЕРШЁН ===");
    }
}