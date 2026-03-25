import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
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
        System.out.println("Всего тестов: " + (testsPassed + testsFailed));

        if (system != null) {
            system.shutdown();
        }

        System.out.println("\n=== ТЕСТИРОВАНИЕ ЗАВЕРШЕНО ===");
        System.exit(0);
    }

    private static void setup() {
        system = new RBACSystem();
        system.initialize();

        parser = new CommandParser();
        CommandRegistry.registerAllCommands(parser, system);

        System.out.println("Система инициализирована. Текущий пользователь: " + system.getCurrentUser());
    }

    private static void testUserCommands() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ КОМАНД ПОЛЬЗОВАТЕЛЕЙ ---");

        test("user-create", () -> {
            int countBefore = system.getUserManager().count();
            String input = "testuser\nTest User\ntest@test.com\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("user-create", sc, system);

            boolean exists = system.getUserManager().exists("testuser");
            if (exists && system.getUserManager().count() == countBefore + 1) {
                System.out.println("OK: пользователь создан");
                testsPassed++;
            } else {
                System.out.println("FAIL: пользователь не создан");
                testsFailed++;
            }
        });

        test("user-list", () -> {
            simulateCommand("user-list");
            System.out.println("OK: user-list выполнился");
            testsPassed++;
        });

        test("user-view", () -> {
            String input = "testuser\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("user-view", sc, system);
            System.out.println("OK: user-view выполнился");
            testsPassed++;
        });

        test("user-update", () -> {
            String input = "testuser\nUpdated Name\nupdated@test.com\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("user-update", sc, system);

            User u = system.getUserManager().findByUsername("testuser").orElse(null);
            if (u != null && "Updated Name".equals(u.fullName()) && "updated@test.com".equals(u.email())) {
                System.out.println("OK: данные обновлены");
                testsPassed++;
            } else {
                System.out.println("FAIL: данные не обновлены");
                testsFailed++;
            }
        });

        test("user-delete", () -> {
            User temp = User.validate("todelete", "To Delete", "delete@test.com");
            system.getUserManager().add(temp);

            String input = "todelete\nда\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("user-delete", sc, system);

            boolean exists = system.getUserManager().exists("todelete");
            if (!exists) {
                System.out.println("OK: пользователь удалён");
                testsPassed++;
            } else {
                System.out.println("FAIL: пользователь не удалён");
                testsFailed++;
            }
        });

        test("user-search", () -> {
            String input = "3\n@test.com\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("user-search", sc, system);
            System.out.println("OK: user-search выполнился");
            testsPassed++;
        });
    }

    
    private static void testRoleCommands() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ КОМАНД РОЛЕЙ ---");

        test("role-list", () -> {
            int count = system.getRoleManager().count();
            simulateCommand("role-list");
            System.out.println("OK: выведено " + count + " ролей");
            testsPassed++;
        });

        test("role-create", () -> {
            int countBefore = system.getRoleManager().count();
            String input = "TestRole\nТестовая роль\nнет\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("role-create", sc, system);

            boolean exists = system.getRoleManager().exists("TestRole");
            if (exists && system.getRoleManager().count() == countBefore + 1) {
                System.out.println("OK: роль создана");
                testsPassed++;
            } else {
                System.out.println("FAIL: роль не создана");
                testsFailed++;
            }
        });

        test("role-update", () -> {
            Role tempRole = new Role("TempRole", "Временная");
            system.getRoleManager().add(tempRole);

            String input = "TempRole\nUpdatedRole\nНовое описание\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("role-update", sc, system);

            Optional<Role> updated = system.getRoleManager().findByName("UpdatedRole");
            if (updated.isPresent() && "Новое описание".equals(updated.get().getDescription())) {
                System.out.println("OK: роль обновлена (UpdatedRole)");
                testsPassed++;
            } else {
                System.out.println("FAIL: роль не обновлена");
                testsFailed++;
            }
        });

        test("role-view", () -> {
            String input = "TestRole\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("role-view", sc, system);
            System.out.println("OK: role-view выполнился");
            testsPassed++;
        });

        test("role-add-permission", () -> {
            String input = "TestRole\nMANAGE\nsettings\nУправление\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("role-add-permission", sc, system);

            Role role = system.getRoleManager().findByName("TestRole").orElse(null);
            if (role != null && role.hasPermission("MANAGE", "settings")) {
                System.out.println("OK: право добавлено");
                testsPassed++;
            } else {
                System.out.println("FAIL: право не добавлено");
                testsFailed++;
            }
        });

        test("role-remove-permission", () -> {
            String input = "TestRole\n1\n"; 
            Scanner sc = new Scanner(input);
            parser.executeCommand("role-remove-permission", sc, system);
            System.out.println("OK: role-remove-permission выполнился");
            testsPassed++;
        });

        test("role-search", () -> {
            String input = "1\nTest\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("role-search", sc, system);
            System.out.println("OK: role-search выполнился");
            testsPassed++;
        });

        test("role-delete", () -> {
            Role temp = new Role("TempToDelete", "Временная");
            system.getRoleManager().add(temp);

            String input = "TempToDelete\nда\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("role-delete", sc, system);

            boolean exists = system.getRoleManager().exists("TempToDelete");
            if (!exists) {
                System.out.println("OK: роль удалена");
                testsPassed++;
            } else {
                System.out.println("FAIL: роль не удалена");
                testsFailed++;
            }
        });
    }


    private static void testAssignmentCommands() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ КОМАНД НАЗНАЧЕНИЙ ---");

        User testUser = User.validate("testassign", "Test Assign", "testassign@test.com");
        system.getUserManager().add(testUser);

        Role testRole = new Role("TestAssignRole", "Роль для тестов");
        system.getRoleManager().add(testRole);

        test("assign-role", () -> {
            int countBefore = system.getAssignmentManager().count();
            
            List<Role> roles = system.getRoleManager().findAll();
            int roleNumber = -1;
            for (int i = 0; i < roles.size(); i++) {
                if (roles.get(i).getName().equals("TestAssignRole")) {
                    roleNumber = i + 1;
                    break;
                }
            }
            
            if (roleNumber == -1) {
                System.out.println("FAIL: роль не найдена в списке");
                testsFailed++;
                return;
            }
            
            String input = "testassign\n" + roleNumber + "\n1\nТестовое назначение\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("assign-role", sc, system);

            boolean hasRole = system.getAssignmentManager().userHasRole(testUser, testRole);
            if (hasRole && system.getAssignmentManager().count() == countBefore + 1) {
                System.out.println("OK: роль назначена");
                testsPassed++;
            } else {
                System.out.println("FAIL: роль не назначена");
                testsFailed++;
            }
        });

        test("assignment-list", () -> {
            simulateCommand("assignment-list");
            System.out.println("OK: assignment-list выполнился");
            testsPassed++;
        });

        test("assignment-list-user", () -> {
            String input = "testassign\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("assignment-list-user", sc, system);
            System.out.println("OK: assignment-list-user выполнился");
            testsPassed++;
        });

        test("assignment-list-role", () -> {
            String input = "TestAssignRole\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("assignment-list-role", sc, system);
            System.out.println("OK: assignment-list-role выполнился");
            testsPassed++;
        });

        test("assignment-active", () -> {
            int activeBefore = system.getAssignmentManager().getActiveAssignments().size();
            simulateCommand("assignment-active");
            int activeAfter = system.getAssignmentManager().getActiveAssignments().size();
            if (activeAfter >= activeBefore) {
                System.out.println("OK: active assignments выведены (" + activeAfter + ")");
                testsPassed++;
            } else {
                System.out.println("FAIL: active assignments не выведены");
                testsFailed++;
            }
        });

        test("assignment-expired", () -> {
            User expiredUser = User.validate("expireduser", "Expired", "expired@test.com");
            system.getUserManager().add(expiredUser);

            Role expiredRole = new Role("ExpiredRole", "Для теста");
            system.getRoleManager().add(expiredRole);

            AssignmentMetadata meta = AssignmentMetadata.now("test", "Для теста");
            TemporaryAssignment expired = new TemporaryAssignment(
                    expiredUser, expiredRole, meta, "2020-01-01", false);
            system.getAssignmentManager().add(expired);

            simulateCommand("assignment-expired");

            boolean hasExpired = system.getAssignmentManager().getExpiredAssignments().size() > 0;
            if (hasExpired) {
                System.out.println("OK: истёкшие назначения найдены");
                testsPassed++;
            } else {
                System.out.println("FAIL: истёкших назначений не найдено");
                testsFailed++;
            }
        });

        test("assignment-extend", () -> {
            User extendUser = User.validate("extenduser", "Extend User", "extend@test.com");
            if (!system.getUserManager().exists("extenduser")) {
                system.getUserManager().add(extendUser);
            }

            Role extendRole = new Role("ExtendRole", "Роль для теста продления");
            if (!system.getRoleManager().exists("ExtendRole")) {
                system.getRoleManager().add(extendRole);
            }

            String initialDate = LocalDate.now().minusDays(10).format(DateTimeFormatter.ISO_LOCAL_DATE);
            TemporaryAssignment temp = new TemporaryAssignment(
                    extendUser, extendRole,
                    AssignmentMetadata.now("test", "Для теста продления"),
                    initialDate, false);
            system.getAssignmentManager().add(temp);

            String assignId = temp.assignmentId();

            String newDate = LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String input = "1\n" + assignId + "\n" + newDate + "\n";  

            Scanner sc = new Scanner(input);
            parser.executeCommand("assignment-extend", sc, system);

            TemporaryAssignment updated = (TemporaryAssignment) system.getAssignmentManager()
                    .findById(assignId).orElse(null);

            if (updated != null && newDate.equals(updated.getExpiresAt())) {
                System.out.println("OK: назначение продлено до " + newDate);
                testsPassed++;
            } else {
                System.out.println("FAIL: назначение не продлено (или не найдено)");
                testsFailed++;
            }
        });

        test("revoke-role", () -> {
            String input = "testassign\n1\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("revoke-role", sc, system);

            boolean hasRole = system.getAssignmentManager().userHasRole(testUser, testRole);
            if (!hasRole) {
                System.out.println("OK: роль отозвана");
                testsPassed++;
            } else {
                System.out.println("FAIL: роль не отозвана");
                testsFailed++;
            }
        });

        test("assignment-search", () -> {
            String input = "1\ntestassign\n"; 
            Scanner sc = new Scanner(input);
            parser.executeCommand("assignment-search", sc, system);
            System.out.println("OK: assignment-search выполнился");
            testsPassed++;
        });
    }


    private static void testPermissionCommands() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ КОМАНД ПРАВ ---");

        test("permissions-user", () -> {
            String input = "admin\n"; 
            Scanner sc = new Scanner(input);
            parser.executeCommand("permissions-user", sc, system);
            System.out.println("OK: permissions-user выполнился для admin");
            testsPassed++;
        });

        test("permissions-check", () -> {
            String input = "admin\nREAD\nusers\n";
            Scanner sc = new Scanner(input);
            parser.executeCommand("permissions-check", sc, system);
            System.out.println("OK: permissions-check выполнился");
            testsPassed++;
        });
    }

    private static void testUtilityCommands() {
        System.out.println("\n--- ТЕСТИРОВАНИЕ СЛУЖЕБНЫХ КОМАНД ---");

        test("stats", () -> {
            simulateCommand("stats");
            System.out.println("OK: stats выполнился");
            testsPassed++;
        });

        test("help", () -> {
            simulateCommand("help");
            System.out.println("OK: help выполнился");
            testsPassed++;
        });
    }


    private static void test(String commandName, Runnable testLogic) {
        System.out.print("Тест " + commandName + " ... ");
        try {
            testLogic.run();
        } catch (Exception e) {
            System.out.println("ОШИБКА: " + e.getMessage());
            testsFailed++;
        }
    }

    private static void simulateCommand(String commandName) {
        parser.parseAndExecute(commandName, new Scanner(""), system);
    }
}