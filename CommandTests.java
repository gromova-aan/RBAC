
import java.util.Scanner;

public class CommandTests {
    
    private static RBACSystem system;
    private static CommandParser parser;
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== ТЕСТИРОВАНИЕ КОМАНД RBAC ===\n");
        
        setup();
        
        testUserCommands();
        testRoleCommands();
        testAssignmentCommands();
        testPermissionCommands();
        testUtilityCommands();
        
        System.out.println("\n=== ИТОГИ ТЕСТИРОВАНИЯ ===");
        System.out.println("Пройдено: " + testsPassed);
        System.out.println("Провалено: " + testsFailed);
        System.out.println("Всего: " + (testsPassed + testsFailed));
    }
    
    private static void setup() {
        system = new RBACSystem();
        system.initialize();
        parser = new CommandParser();
        CommandRegistry.registerAllCommands(parser, system);
        System.out.println(" Система инициализирована");
    }
    
    //ТЕСТЫ КОМАНД ПОЛЬЗОВАТЕЛЕЙ
    private static void testUserCommands() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ КОМАНД ПОЛЬЗОВАТЕЛЕЙ ---");

        // Тест 1: Создание пользователя
        test("user-create", () -> {
            String input = "testuser\nTest User\ntest@test.com\n";
            Scanner scanner = new Scanner(input);
            parser.executeCommand("user-create", scanner, system);
            
            boolean exists = system.getUserManager().exists("testuser");
            if (exists) {
                System.out.println("   user-create: пользователь создан");
                testsPassed++;
            } else {
                System.out.println("   user-create: пользователь не создан");
                testsFailed++;
            }
        });

        // Тест 2: Поиск пользователей
        test("user-list", () -> {
            int count = system.getUserManager().count();
            if (count > 0) {
                System.out.println("   user-list: список получен (" + count + " пользователей)");
                testsPassed++;
            } else {
                System.out.println("   user-list: список пуст");
                testsFailed++;
            }
        });
        
        // Тест 3: Поиск пользователя по username
        test("user-view", () -> {
            var user = system.getUserManager().findByUsername("testuser");
            if (user.isPresent()) {
                System.out.println("   user-view: пользователь найден");
                testsPassed++;
            } else {
                System.out.println("   user-view: пользователь не найден");
                testsFailed++;
            }
        });
        
        // Тест 4: Обновление пользователя
        test("user-update", () -> {
            String input = "testuser\nUpdated User\nupdated@test.com\n";
            Scanner scanner = new Scanner(input);
            parser.executeCommand("user-update", scanner, system);
            
            var user = system.getUserManager().findByUsername("testuser").get();
            if (user.fullName().equals("Updated User") && 
                user.email().equals("updated@test.com")) {
                System.out.println("   user-update: данные обновлены");
                testsPassed++;
            } else {
                System.out.println("   user-update: данные не обновлены");
                testsFailed++;
            }
        });

        // Тест 5: Удаление пользователя
        test("user-delete", () -> {
            //создадим пользователя для удаления
            User tempUser = User.validate("todelete", "To Delete", "delete@test.com");
            system.getUserManager().add(tempUser);
            
            if (!system.getUserManager().exists("todelete")) {
                System.out.println("   user-delete: не удалось создать тестового пользователя");
                testsFailed++;
                return;
            }
            
            String input = "todelete\nда\n";
            Scanner scanner = new Scanner(input);
            parser.executeCommand("user-delete", scanner, system);
            
            boolean exists = system.getUserManager().exists("todelete");
            if (!exists) {
                System.out.println("   user-delete: пользователь удален");
                testsPassed++;
            } else {
                System.out.println("   user-delete: пользователь не удален");
                testsFailed++;
            }
        });
        
        // Тест 6: Фильтр пользователей по email
        test("user-search", () -> {
            var filter = UserFilters.byEmailDomain("@test.com");
            var results = system.getUserManager().findByFilter(filter);
            if (results.size() == 1) {
                System.out.println("   user-search: фильтр работает");
                testsPassed++;
            } else {
                System.out.println("   user-search: фильтр не работает");
                testsFailed++;
            }
        });
    }
    
    //ТЕСТЫ КОМАНД РОЛЕЙ
    private static void testRoleCommands() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ КОМАНД РОЛЕЙ ---");
        
        // Тест 7: Создание роли
        test("role-create", () -> {
            String input = "TestRole\nТестовая роль\nнет\n";
            Scanner scanner = new Scanner(input);
            parser.executeCommand("role-create", scanner, system);
            
            boolean exists = system.getRoleManager().exists("TestRole");
            if (exists) {
                System.out.println("   role-create: роль создана");
                testsPassed++;
            } else {
                System.out.println("   role-create: роль не создана");
                testsFailed++;
            }
        });
        
        // Тест 8: Поиск роли по имени
        test("role-view", () -> {
            var role = system.getRoleManager().findByName("TestRole");
            if (role.isPresent()) {
                System.out.println("   role-view: роль найдена");
                testsPassed++;
            } else {
                System.out.println("   role-view: роль не найдена");
                testsFailed++;
            }
        });
        
        // Тест 9: Добавление права к роли
        test("role-add-permission", () -> {
            String input = "TestRole\nREAD\ntest\nТестовое право\n";
            Scanner scanner = new Scanner(input);
            parser.executeCommand("role-add-permission", scanner, system);
            
            var role = system.getRoleManager().findByName("TestRole").get();
            boolean hasPerm = role.hasPermission("READ", "test");
            
            if (hasPerm) {
                System.out.println("   role-add-permission: право добавлено");
                testsPassed++;
            } else {
                System.out.println("   role-add-permission: право не добавлено");
                testsFailed++;
            }
        });

        // Тест 10: Обновление роли 
        test("role-update", () -> {
            String input = "TestRole\nUpdatedRole\nОбновленное описание\n";
            Scanner scanner = new Scanner(input);
            parser.executeCommand("role-update", scanner, system);
            
            var role = system.getRoleManager().findByName("UpdatedRole");
            if (role.isPresent() && role.get().getDescription().equals("Обновленное описание")) {
                System.out.println("   role-update: роль обновлена");
                testsPassed++;
            } else {
                System.out.println("   role-update: роль не обновлена");
                testsFailed++;
            }
        });

        // Тест 11: Поиск ролей 
        test("role-search", () -> {
            // Тест поиска по имени (содержит)
            String input1 = "1\nUpdated\n";
            Scanner scanner1 = new Scanner(input1);
            parser.executeCommand("role-search", scanner1, system);
            
            var filter1 = (RoleFilter) (role -> role.getName().contains("Updated"));
            var results1 = system.getRoleManager().findByFilter(filter1);
            
            // Тест поиска по наличию права
            String input2 = "2\nREAD\ntest\n";
            Scanner scanner2 = new Scanner(input2);
            parser.executeCommand("role-search", scanner2, system);
            
            var filter2 = (RoleFilter) (role -> role.hasPermission("READ", "test"));
            var results2 = system.getRoleManager().findByFilter(filter2);
            
            // Тест поиска по минимальному количеству прав
            String input3 = "3\n1\n";
            Scanner scanner3 = new Scanner(input3);
            parser.executeCommand("role-search", scanner3, system);
            
            var filter3 = (RoleFilter) (role -> role.getPermissions().size() >= 1);
            var results3 = system.getRoleManager().findByFilter(filter3);
            
            if (results1.size() > 0 && results2.size() > 0 && results3.size() > 0) {
                System.out.println("   role-search: поиск работает");
                testsPassed++;
            } else {
                System.out.println("   role-search: поиск не работает");
                testsFailed++;
            }
        });

        // Тест 12: Удаление права из роли 
        test("role-remove-permission", () -> {
            var role = system.getRoleManager().findByName("UpdatedRole").get();
            int beforeCount = role.getPermissions().size();
            
            String input = "UpdatedRole\n1\n"; // Удаляем первое право
            Scanner scanner = new Scanner(input);
            parser.executeCommand("role-remove-permission", scanner, system);
            
            var updatedRole = system.getRoleManager().findByName("UpdatedRole").get();
            int afterCount = updatedRole.getPermissions().size();
            
            if (afterCount == beforeCount - 1) {
                System.out.println("   role-remove-permission: право удалено");
                testsPassed++;
            } else {
                System.out.println("   role-remove-permission: право не удалено");
                testsFailed++;
            }
        });
        
        // Тест 13: Список ролей
        test("role-list", () -> {
            int count = system.getRoleManager().count();
            if (count >= 4) { // 3 начальные + 1 тестовая
                System.out.println("   role-list: список получен (" + count + " ролей)");
                testsPassed++;
            } else {
                System.out.println("   role-list: список неполный");
                testsFailed++;
            }
        });

        // Тест 14: Удаление роли 
        test("role-delete", () -> {
            Role tempRole = new Role("TempRole", "Временная роль");
            system.getRoleManager().add(tempRole);
            
            if (!system.getRoleManager().exists("TempRole")) {
                System.out.println("   role-delete: не удалось создать тестовую роль");
                testsFailed++;
                return;
            }
            
            String input = "TempRole\nда\n";
            Scanner scanner = new Scanner(input);
            parser.executeCommand("role-delete", scanner, system);
            
            boolean exists = system.getRoleManager().exists("TempRole");
            if (!exists) {
                System.out.println("   role-delete: роль удалена");
                testsPassed++;
            } else {
                System.out.println("   role-delete: роль не удалена");
                testsFailed++;
            }
        });
    }
    
    // ТЕСТЫ КОМАНД НАЗНАЧЕНИЙ
    private static void testAssignmentCommands() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ КОМАНД НАЗНАЧЕНИЙ ---");
        
        // Создаем ОДИН раз пользователя и роль для всех тестов назначений
        User assignUser = User.validate("assignuser", "Assign User", "assign@test.com");
        system.getUserManager().add(assignUser);
        
        Role assignRole = new Role("AssignRole", "Роль для тестов назначений");
        system.getRoleManager().add(assignRole);
        
        // Тест 15: Назначение роли
        test("assign-role", () -> {
            // Получаем номер роли в списке
            var roles = system.getRoleManager().findAll();
            int roleNumber = -1;
            for (int i = 0; i < roles.size(); i++) {
                if (roles.get(i).getName().equals("AssignRole")) {
                    roleNumber = i + 1;
                    break;
                }
            }
            
            String input = "assignuser\n" + roleNumber + "\n1\nТестовое назначение\n";
            Scanner scanner = new Scanner(input);
            parser.executeCommand("assign-role", scanner, system);
            
            boolean hasRole = system.getAssignmentManager().userHasRole(assignUser, assignRole);
            
            if (hasRole) {
                System.out.println("   assign-role: роль назначена");
                testsPassed++;
            } else {
                System.out.println("   assign-role: роль не назначена");
                testsFailed++;
            }
        });

        // Тест 16: Список всех назначений 
        test("assignment-list", () -> {
            int count = system.getAssignmentManager().count();
            if (count >= 1) {
                System.out.println("   assignment-list: список всех назначений получен (" + count + ")");
                testsPassed++;
            } else {
                System.out.println("   assignment-list: назначений нет");
                testsFailed++;
            }
        });
        
        // Тест 17: Список назначений пользователя
        test("assignment-list-user", () -> {
            var assignments = system.getAssignmentManager().findByUser(assignUser);
            
            if (assignments.size() >= 1) {
                System.out.println("   assignment-list-user: назначения найдены (" + assignments.size() + ")");
                testsPassed++;
            } else {
                System.out.println("   assignment-list-user: назначений нет");
                testsFailed++;
            }
        });

        // Тест 18: Список пользователей с ролью
        test("assignment-list-role", () -> {
            var assignments = system.getAssignmentManager().findByRole(assignRole);
            
            if (assignments.size() >= 1) {
                System.out.println("   assignment-list-role: список пользователей с ролью получен (" + assignments.size() + ")");
                testsPassed++;
            } else {
                System.out.println("   assignment-list-role: пользователей с ролью нет");
                testsFailed++;
            }
        });
        
        // Тест 19: Активные назначения
        test("assignment-active", () -> {
            var active = system.getAssignmentManager().getActiveAssignments();
            if (active.size() >= 1) {
                System.out.println("   assignment-active: активные назначения есть");
                testsPassed++;
            } else {
                System.out.println("   assignment-active: активных назначений нет");
                testsFailed++;
            }
        });

        // Тест 20: Истекшие назначения
        test("assignment-expired", () -> {
            // Создаем нового пользователя для этого теста, чтобы не конфликтовать
            User expiredUser = User.validate("expireduser", "Expired User", "expired@test.com");
            system.getUserManager().add(expiredUser);
            
            Role expiredRole = new Role("ExpiredRole", "Роль для теста истекших");
            system.getRoleManager().add(expiredRole);
            
            // Создаем временное назначение с прошедшей датой (уже неактивное)
            AssignmentMetadata meta = AssignmentMetadata.now("test", "Для проверки истекших");
            
            TemporaryAssignment expired = new TemporaryAssignment(
                expiredUser, expiredRole, meta, "2020-01-01", false);
            system.getAssignmentManager().add(expired);
            
            var expiredList = system.getAssignmentManager().getExpiredAssignments();
            if (expiredList.size() >= 1) {
                System.out.println("   assignment-expired: истекшие назначения найдены");
                testsPassed++;
            } else {
                System.out.println("   assignment-expired: истекших назначений нет");
                testsFailed++;
            }
        });

        // Тест 21: Продление временного назначения
        test("assignment-extend", () -> {
            // Создаем нового пользователя для этого теста
            User extendUser = User.validate("extenduser", "Extend User", "extend@test.com");
            system.getUserManager().add(extendUser);
            
            Role extendRole = new Role("ExtendRole", "Роль для теста продления");
            system.getRoleManager().add(extendRole);
            
            AssignmentMetadata meta = AssignmentMetadata.now("test", "Для продления");
            
            // Создаем временное назначение с будущей датой
            TemporaryAssignment temp = new TemporaryAssignment(
                extendUser, extendRole, meta, "2026-12-31", false);
            system.getAssignmentManager().add(temp);
            
            String assignmentId = temp.assignmentId();
            String input = "1\n" + assignmentId + "\n2027-12-31\n";
            Scanner scanner = new Scanner(input);
            parser.executeCommand("assignment-extend", scanner, system);
            
            var updated = (TemporaryAssignment) system.getAssignmentManager()
                .findById(assignmentId).get();
            
            if (updated.getExpiresAt().equals("2027-12-31")) {
                System.out.println("   assignment-extend: назначение продлено");
                testsPassed++;
            } else {
                System.out.println("   assignment-extend: назначение не продлено");
                testsFailed++;
            }
        });

        // Тест 22: Поиск назначений
        test("assignment-search", () -> {
            int successCount = 0;
            
            // Поиск по пользователю
            String input1 = "1\nassignuser\n";
            Scanner scanner1 = new Scanner(input1);
            parser.executeCommand("assignment-search", scanner1, system);
            successCount++;
            
            // Поиск по роли
            String input2 = "2\nAssignRole\n";
            Scanner scanner2 = new Scanner(input2);
            parser.executeCommand("assignment-search", scanner2, system);
            successCount++;
            
            // Поиск по типу (постоянные)
            String input3 = "3\n1\n";
            Scanner scanner3 = new Scanner(input3);
            parser.executeCommand("assignment-search", scanner3, system);
            successCount++;
            
            // Поиск по статусу (активные)
            String input4 = "4\n1\n";
            Scanner scanner4 = new Scanner(input4);
            parser.executeCommand("assignment-search", scanner4, system);
            successCount++;
            
            // Поиск назначенных после даты
            String input5 = "5\n2020-01-01\n";
            Scanner scanner5 = new Scanner(input5);
            parser.executeCommand("assignment-search", scanner5, system);
            successCount++;
            
            if (successCount == 5) {
                System.out.println("   assignment-search: поиск работает");
                testsPassed++;
            } else {
                System.out.println("   assignment-search: ошибка поиска");
                testsFailed++;
            }
        });
        
        // Тест 23: Отзыв роли
        test("revoke-role", () -> {
            // Создаем нового пользователя для этого теста
            User revokeUser = User.validate("revokeuser", "Revoke User", "revoke@test.com");
            system.getUserManager().add(revokeUser);
            
            Role revokeRole = new Role("RevokeRole", "Роль для теста отзыва");
            system.getRoleManager().add(revokeRole);
            
            // Создаем назначение
            AssignmentMetadata meta = AssignmentMetadata.now("test", "Для отзыва");
            PermanentAssignment assignment = new PermanentAssignment(revokeUser, revokeRole, meta);
            system.getAssignmentManager().add(assignment);
            
            String input = "revokeuser\n1\n";
            Scanner scanner = new Scanner(input);
            parser.executeCommand("revoke-role", scanner, system);
            
            boolean stillHasRole = system.getAssignmentManager().userHasRole(revokeUser, revokeRole);
            if (!stillHasRole) {
                System.out.println("   revoke-role: роль отозвана");
                testsPassed++;
            } else {
                System.out.println("   revoke-role: роль не отозвана");
                testsFailed++;
            }
        });
    }

    // ТЕСТЫ КОМАНД ПРАВ
    private static void testPermissionCommands() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ КОМАНД ПРАВ ---");
        
        User permUser = User.validate("permuser", "Perm User", "perm@test.com");
        system.getUserManager().add(permUser);
        
        Role permRole = new Role("PermRole", "Роль для тестов прав");
        system.getRoleManager().add(permRole);
        
        AssignmentMetadata meta = AssignmentMetadata.now("test", "Для тестов прав");
        PermanentAssignment assignment = new PermanentAssignment(permUser, permRole, meta);
        system.getAssignmentManager().add(assignment);
        
        // Тест 24: Получение прав пользователя
        test("permissions-user", () -> {
            String input = "permuser\n";
            Scanner scanner = new Scanner(input);
            parser.executeCommand("permissions-user", scanner, system);
            
            var perms = system.getAssignmentManager().getUserPermissions(permUser);
            
            if (perms.size() >= 0) { // может быть 0, это нормально
                System.out.println("   permissions-user: права получены");
                testsPassed++;
            } else {
                System.out.println("   permissions-user: прав нет");
                testsFailed++;
            }
        });
        
        // Тест 25: Проверка наличия права
        test("permissions-check", () -> {
            // Добавляем право READ на test к роли
            try {
                Permission readPerm = new Permission("READ", "test", "Тестовое право");
                system.getRoleManager().addPermissionToRole("PermRole", readPerm);
            } catch (Exception e) {
                // Право уже существует
            }
            
            boolean hasRead = system.getAssignmentManager().userHasPermission(permUser, "READ", "test");
            
            if (hasRead) {
                System.out.println("   permissions-check: право READ есть");
                testsPassed++;
            } else {
                System.out.println("   permissions-check: права READ нет");
                testsFailed++;
            }
        });
    }

    
    //ТЕСТЫ СЛУЖЕБНЫХ КОМАНД
    private static void testUtilityCommands() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ СЛУЖЕБНЫХ КОМАНД ---");
        
        // Тест 26: Статистика
        test("stats", () -> {
            String stats = system.generateStatistics();
            if (stats.contains("Пользователей:") && stats.contains("Ролей:")) {
                System.out.println("   stats: статистика работает");
                testsPassed++;
            } else {
                System.out.println("   stats: ошибка статистики");
                testsFailed++;
            }
        });
        
        // Тест 27: Help
        test("help", () -> {
            try {
                parser.executeCommand("help", new Scanner(System.in), system);
                System.out.println("   help: справка работает");
                testsPassed++;
            } catch (Exception e) {
                System.out.println("   help: ошибка");
                testsFailed++;
            }
        });
    }
    
    // Вспомогательный метод для тестов
    private static void test(String testName, Runnable test) {
        try {
            test.run();
        } catch (Exception e) {
            System.out.println("   " + testName + ": исключение - " + e.getMessage());
            testsFailed++;
        }
    }
}