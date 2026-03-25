
import java.util.List;

public class CommandRegistry {
    public static void registerAllCommands(CommandParser parser, RBACSystem system) {
        
        //КОМАНДЫ УПРАВЛЕНИЯ ПОЛЬЗОВАТЕЛЯМИ
        parser.registerCommand("user-list", "Вывести список всех пользователей", (scanner, sys) -> {
            var users = sys.getUserManager().findAll();
            
            if (users.isEmpty()) {
                System.out.println("Пользователей нет");
                return;
            }
            
            String[] headers = {"USERNAME", "FULL NAME", "EMAIL"};
            List<String[]> rows = new java.util.ArrayList<>();

            for (User user : users) {
                rows.add(new String[]{
                    user.username(),
                    user.fullName(),
                    user.email()
                });
            }
            System.out.println(FormatUtils.formatHeader("СПИСОК ПОЛЬЗОВАТЕЛЕЙ"));
            System.out.println(FormatUtils.formatTable(headers, rows));
            System.out.println("Всего: " + users.size());
        });


        parser.registerCommand("user-create", "Создать нового пользователя", (scanner, sys) -> {
            try {
                String username = ConsoleUtils.promptString(scanner, "Введите username (латиница, цифры, _, от 3 до 20 символов)", true);
                String fullName = ConsoleUtils.promptString(scanner, "Введите полное имя", true);
                String email = ConsoleUtils.promptString(scanner, "Введите email", true);
                
                User user = User.validate(username, fullName, email);
                sys.getUserManager().add(user);

                sys.getAuditLog().log("CREATE_USER", sys.getCurrentUser(), username, "Создан пользователь");
                
                System.out.println("Пользователь успешно создан!");
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        
        parser.registerCommand("user-view", "Просмотр информации о пользователе", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Введите username", true);
            
            var userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("Пользователь не найден");
                return;
            }
            
            User user = userOpt.get();
            System.out.println("\n--- ИНФОРМАЦИЯ О ПОЛЬЗОВАТЕЛЕ ---");
            System.out.println("Username: " + user.username());
            System.out.println("Full name: " + user.fullName());
            System.out.println("Email: " + user.email());
            
            var assignments = sys.getAssignmentManager().findByUser(user);
            if (assignments.isEmpty()) {
                System.out.println("Роли: не назначены");
            } else {
                System.out.println("\nНазначенные роли:");
                for (var assignment : assignments) {
                    String status = assignment.isActive() ? "ACTIVE" : "INACTIVE";
                    System.out.println("  - " + assignment.role().getName() + 
                        " (" + assignment.assignmentType() + ", " + status + ")");
                }
            }
            
            var permissions = sys.getAssignmentManager().getUserPermissions(user);
            if (!permissions.isEmpty()) {
                System.out.println("\nВсе права:");
                for (var perm : permissions) {
                    System.out.println("  - " + perm.format());
                }
            }
        });


        parser.registerCommand("user-update", "Обновить данные пользователя", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Введите username", true);
            
            var userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("Пользователь не найден");
                return;
            }
            
            User user = userOpt.get();
            System.out.println("Текущие данные: " + user.format());
            
            String fullName = ConsoleUtils.promptString(scanner, "Новое полное имя (Enter - оставить текущее)", false);
            if (fullName.isEmpty()) {
                fullName = user.fullName();
            }
            
            String email = ConsoleUtils.promptString(scanner, "Новый email (Enter - оставить текущий)", false);
            if (email.isEmpty()) {
                email = user.email();
            }
            
            try {
                sys.getUserManager().update(username, fullName, email);
                System.out.println("Пользователь обновлен");
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });


        parser.registerCommand("user-delete", "Удалить пользователя", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Введите username", true);
            
            var userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("Пользователь не найден");
                return;
            }
            
            User user = userOpt.get();
            
            var assignments = sys.getAssignmentManager().findByUser(user);
            if (!assignments.isEmpty()) {
                System.out.println("У пользователя есть активные назначения:");
                for (var a : assignments) {
                    System.out.println("  - " + a.role().getName() + " (" + a.assignmentType() + ")");
                }
            }
            
            if (ConsoleUtils.promptYesNo(scanner, "Вы уверены")) {
                for (var a : assignments) {
                    sys.getAssignmentManager().remove(a);
                }
                
                sys.getUserManager().remove(user);
                sys.getAuditLog().log("DELETE_USER", sys.getCurrentUser(), username, "Удален пользователь");
                System.out.println("Пользователь удален");
            } else {
                System.out.println("Удаление отменено");
            }
        });
        

        parser.registerCommand("user-search", "Поиск пользователей по фильтрам", (scanner, sys) -> {
            System.out.println("\n--- ПОИСК ПОЛЬЗОВАТЕЛЕЙ ---");
            System.out.println("1. По username (содержит)");
            System.out.println("2. По email (содержит)");
            System.out.println("3. По домену email");
            System.out.println("4. По полному имени (содержит)");
            
            int choice = ConsoleUtils.promptInt(scanner, "Выберите фильтр", 1, 4);
            
            UserFilter filter = null;
            
            switch (choice) {
                case 1:
                    String usernamePart = ConsoleUtils.promptString(scanner, "Введите часть username", true);
                    filter = user -> user.username().toLowerCase().contains(usernamePart.toLowerCase());
                    break;
                case 2:
                    String emailPart = ConsoleUtils.promptString(scanner, "Введите часть email", true);
                    filter = user -> user.email().toLowerCase().contains(emailPart.toLowerCase());
                    break;
                case 3:
                    String domain = ConsoleUtils.promptString(scanner, "Введите домен (например @gmail.com)", true);
                    filter = user -> user.email().toLowerCase().endsWith(domain.toLowerCase());
                    break;
                case 4:
                    String namePart = ConsoleUtils.promptString(scanner, "Введите часть полного имени", true);
                    filter = user -> user.fullName().toLowerCase().contains(namePart.toLowerCase());
                    break;
            }
            
            var results = sys.getUserManager().findByFilter(filter);
            
            if (results.isEmpty()) {
                System.out.println("Ничего не найдено");
                return;
            }
            
            System.out.println("\nНайдено пользователей: " + results.size());
            for (User user : results) {
                System.out.println("  " + user.format());
            }
        });
        

        //КОМАНДЫ УПРАВЛЕНИЯ РОЛЯМИ
        parser.registerCommand("role-list", "Вывести список всех ролей", (scanner, sys) -> {
            var roles = sys.getRoleManager().findAll();
            
            if (roles.isEmpty()) {
                System.out.println("Ролей нет");
                return;
            }
            
            System.out.println("\n--- СПИСОК РОЛЕЙ ---");
            System.out.printf("%-20s %-30s %-10s %s\n", "NAME", "DESCRIPTION", "RIGHTS", "ID");
            System.out.println("-------------------------------------------------------------");
            
            for (Role role : roles) {
                System.out.printf("%-20s %-30s %-10d %s\n", 
                    role.getName(), 
                    role.getDescription(), 
                    role.getPermissions().size(),
                    role.getId());
            }
        });


        parser.registerCommand("role-create", "Создать новую роль", (scanner, sys) -> {
            try {
                String name = ConsoleUtils.promptString(scanner, "Введите название роли", true);
                String description = ConsoleUtils.promptString(scanner, "Введите описание роли", true);
                
                Role role = new Role(name, description);
                sys.getRoleManager().add(role);

                sys.getAuditLog().log("CREATE_ROLE", sys.getCurrentUser(), name, "Создана роль");
                
                System.out.println("Роль создана. ID: " + role.getId());
                
                if (ConsoleUtils.promptYesNo(scanner, "Добавить права сейчас")) {
                    boolean addMore = true;
                    while (addMore) {
                        String permName = ConsoleUtils.promptString(scanner, "Введите имя права (например READ)", true).toUpperCase();
                        String resource = ConsoleUtils.promptString(scanner, "Введите ресурс (например users)", true).toLowerCase();
                        String permDesc = ConsoleUtils.promptString(scanner, "Введите описание права", true);
                        
                        try {
                            Permission perm = new Permission(permName, resource, permDesc);
                            sys.getRoleManager().addPermissionToRole(name, perm);
                            System.out.println("Право добавлено");
                        } catch (IllegalArgumentException e) {
                            System.out.println("Ошибка: " + e.getMessage());
                        }
                        
                        addMore = ConsoleUtils.promptYesNo(scanner, "Добавить еще право");
                    }
                }
                
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });


        parser.registerCommand("role-view", "Просмотр информации о роли", (scanner, sys) -> {
            String name = ConsoleUtils.promptString(scanner, "Введите имя роли", true);
            
            var roleOpt = sys.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Роль не найдена");
                return;
            }
            
            Role role = roleOpt.get();
            System.out.println(role.format());
        });


        parser.registerCommand("role-update", "Обновить название и описание роли", (scanner, sys) -> {
            String oldName = ConsoleUtils.promptString(scanner, "Введите имя роли для обновления", true);
            
            var roleOpt = sys.getRoleManager().findByName(oldName);
            if (roleOpt.isEmpty()) {
                System.out.println("Роль не найдена");
                return;
            }
            
            Role role = roleOpt.get();
            System.out.println("\nТекущие данные роли:");
            System.out.println("  Название: " + role.getName());
            System.out.println("  Описание: " + role.getDescription());
            
            String newName = ConsoleUtils.promptString(scanner, "Введите новое название (Enter - оставить текущее)", false);
            if (newName.isEmpty()) {
                newName = role.getName();
            }
            
            String newDescription = ConsoleUtils.promptString(scanner, "Введите новое описание (Enter - оставить текущее)", false);
            if (newDescription.isEmpty()) {
                newDescription = role.getDescription();
            }
            
            if (!newName.equals(role.getName()) && sys.getRoleManager().exists(newName)) {
                System.out.println("Ошибка: роль с именем '" + newName + "' уже существует");
                return;
            }
            
            try {
                Role updatedRole = new Role(newName, newDescription);
                
                for (Permission perm : role.getPermissions()) {
                    updatedRole.addPermission(perm);
                }
                
                sys.getRoleManager().remove(role);
                sys.getRoleManager().add(updatedRole);
                
                System.out.println("Роль успешно обновлена");
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка при обновлении: " + e.getMessage());
            }
        });


        parser.registerCommand("role-delete", "Удалить роль", (scanner, sys) -> {
            String name = ConsoleUtils.promptString(scanner, "Введите имя роли", true);
            
            var roleOpt = sys.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Роль не найдена");
                return;
            }
            
            Role role = roleOpt.get();
            
            if (sys.getAssignmentManager().isRoleAssigned(role)) {
                System.out.println("Роль назначена следующим пользователям:");
                var assignments = sys.getAssignmentManager().findByRole(role);
                for (var a : assignments) {
                    System.out.println("  - " + a.user().username());
                }
                
                if (!ConsoleUtils.promptYesNo(scanner, "Все равно удалить")) {
                    System.out.println("Удаление отменено");
                    return;
                }
            }
            
            sys.getRoleManager().remove(role);
            sys.getAuditLog().log("DELETE_ROLE", sys.getCurrentUser(), name, "Удалена роль");
            System.out.println("Роль удалена");
        });


        parser.registerCommand("role-add-permission", "Добавить право к роли", (scanner, sys) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Введите имя роли", true);
            
            var roleOpt = sys.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                System.out.println("Роль не найдена");
                return;
            }
            
            try {
                String permName = ConsoleUtils.promptString(scanner, "Введите имя права (например READ)", true).toUpperCase();
                String resource = ConsoleUtils.promptString(scanner, "Введите ресурс (например users)", true).toLowerCase();
                String permDesc = ConsoleUtils.promptString(scanner, "Введите описание права", true);
                
                Permission perm = new Permission(permName, resource, permDesc);
                sys.getRoleManager().addPermissionToRole(roleName, perm);
                System.out.println("Право добавлено к роли");
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });


        parser.registerCommand("role-remove-permission", "Удалить право из роли", (scanner, sys) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Введите имя роли", true);
            
            var roleOpt = sys.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                System.out.println("Роль не найдена");
                return;
            }
            
            Role role = roleOpt.get();
            var permissions = role.getPermissions().stream().toList();
            
            if (permissions.isEmpty()) {
                System.out.println("У роли нет прав для удаления");
                return;
            }
            
            System.out.println("\nПрава роли " + roleName + ":");
            for (int i = 0; i < permissions.size(); i++) {
                Permission p = permissions.get(i);
                System.out.printf("  %d. %s\n", i + 1, p.format());
            }
            
            int choice = ConsoleUtils.promptInt(scanner, "\nВведите номер права для удаления (0 - отмена)", 0, permissions.size());
            if (choice == 0) {
                System.out.println("Удаление отменено");
                return;
            }
            
            Permission permToRemove = permissions.get(choice - 1);
            sys.getRoleManager().removePermissionFromRole(roleName, permToRemove);
            System.out.println("Право удалено из роли");
        });


        parser.registerCommand("role-search", "Поиск ролей по фильтрам", (scanner, sys) -> {
            System.out.println("\n--- ПОИСК РОЛЕЙ ---");
            System.out.println("1. По имени (содержит)");
            System.out.println("2. По наличию конкретного права");
            System.out.println("3. По минимальному количеству прав");
            
            int choice = ConsoleUtils.promptInt(scanner, "Выберите фильтр", 1, 3);
            
            RoleFilter filter = null;
            String filterDescription = "";
            
            switch (choice) {
                case 1:
                    String namePart = ConsoleUtils.promptString(scanner, "Введите часть имени роли", true);
                    filter = role -> role.getName().toLowerCase().contains(namePart.toLowerCase());
                    filterDescription = "имя содержит '" + namePart + "'";
                    break;
                    
                case 2:
                    String permName = ConsoleUtils.promptString(scanner, "Введите имя права (например READ)", true).toUpperCase();
                    String resource = ConsoleUtils.promptString(scanner, "Введите ресурс (например users)", true).toLowerCase();
                    
                    filter = role -> role.hasPermission(permName, resource);
                    filterDescription = "имеет право " + permName + " on " + resource;
                    break;
                    
                case 3:
                    int minCount = ConsoleUtils.promptInt(scanner, "Введите минимальное количество прав", 0, 100);
                    filter = role -> role.getPermissions().size() >= minCount;
                    filterDescription = "минимум " + minCount + " прав";
                    break;
            }
            
            var results = sys.getRoleManager().findByFilter(filter);
            
            if (results.isEmpty()) {
                System.out.println("Ролей, удовлетворяющих фильтру '" + filterDescription + "', не найдено");
                return;
            }
            
            System.out.println("\nНайдено ролей: " + results.size() + " (фильтр: " + filterDescription + ")");
            System.out.printf("%-20s %-30s %s\n", "NAME", "DESCRIPTION", "RIGHTS");
            System.out.println("-------------------------------------------------------------");
            
            for (Role role : results) {
                System.out.printf("%-20s %-30s %d\n", 
                    role.getName(), 
                    role.getDescription(), 
                    role.getPermissions().size());
            }
        });


        //КОМАНДЫ УПРАВЛЕНИЯ НАЗНАЧЕНИЯМИ
        parser.registerCommand("assign-role", "Назначить роль пользователю", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Введите username пользователя", true);
            
            var userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("Пользователь не найден");
                return;
            }
            
            User user = userOpt.get();
            
            var roles = sys.getRoleManager().findAll();
            if (roles.isEmpty()) {
                System.out.println("Нет доступных ролей");
                return;
            }
            
            System.out.println("\nДоступные роли:");
            for (int i = 0; i < roles.size(); i++) {
                Role role = roles.get(i);
                System.out.printf("  %d. %s - %s\n", i + 1, role.getName(), role.getDescription());
            }
            
            int choice = ConsoleUtils.promptInt(scanner, "Выберите номер роли", 1, roles.size());
            Role selectedRole = roles.get(choice - 1);
            
            int typeChoice = ConsoleUtils.promptInt(scanner, "Тип назначения (1 - постоянное, 2 - временное)", 1, 2);
            String reason = ConsoleUtils.promptString(scanner, "Причина назначения", false);
            
            AssignmentMetadata meta = AssignmentMetadata.now(sys.getCurrentUser(), reason);
            
            try {
                if (typeChoice == 1) {
                    PermanentAssignment assignment = new PermanentAssignment(user, selectedRole, meta);
                    sys.getAssignmentManager().add(assignment);
                    sys.getAuditLog().log("ASSIGN_ROLE", sys.getCurrentUser(), username, "Назначена постоянная роль " + selectedRole.getName());
                    System.out.println("Постоянное назначение создано. ID: " + assignment.assignmentId());
                } else {
                    String expiresAt = ConsoleUtils.promptString(scanner, "Введите дату истечения (ГГГГ-ММ-ДД)", true);
                    boolean autoRenew = ConsoleUtils.promptYesNo(scanner, "Автопродление");
                    
                    TemporaryAssignment assignment = new TemporaryAssignment(user, selectedRole, meta, expiresAt, autoRenew);
                    sys.getAssignmentManager().add(assignment);
                    System.out.println("Временное назначение создано. ID: " + assignment.assignmentId());
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });


        parser.registerCommand("revoke-role", "Отозвать роль у пользователя", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Введите username", true);
            
            var userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("Пользователь не найден");
                return;
            }
            
            User user = userOpt.get();
            var assignments = sys.getAssignmentManager().findByUser(user).stream()
                .filter(RoleAssignment::isActive)
                .toList();
            
            if (assignments.isEmpty()) {
                System.out.println("У пользователя нет активных назначений");
                return;
            }
            
            System.out.println("\nАктивные назначения пользователя " + username + ":");
            for (int i = 0; i < assignments.size(); i++) {
                RoleAssignment a = assignments.get(i);
                System.out.printf("  %d. %s (%s) - %s\n", 
                    i + 1, 
                    a.role().getName(), 
                    a.assignmentType(),
                    a.assignmentId());
            }
            
            int choice = ConsoleUtils.promptInt(scanner, "\nВведите номер назначения для отзыва (0 - отмена)", 0, assignments.size());
            if (choice == 0) {
                System.out.println("Отмена");
                return;
            }
            
            RoleAssignment toRevoke = assignments.get(choice - 1);
            
            if (toRevoke instanceof PermanentAssignment) {
                sys.getAssignmentManager().revokeAssignment(toRevoke.assignmentId());
                sys.getAuditLog().log("REVOKE_ROLE", sys.getCurrentUser(), username, "Отозвана роль " + toRevoke.role().getName());
                System.out.println("Назначение отозвано");
            } else if (toRevoke instanceof TemporaryAssignment) {
                sys.getAssignmentManager().remove(toRevoke);
                System.out.println("Временное назначение удалено");
            }
        });


        parser.registerCommand("assignment-list", "Список всех назначений", (scanner, sys) -> {
            var assignments = sys.getAssignmentManager().findAll();
            
            if (assignments.isEmpty()) {
                System.out.println("Назначений нет");
                return;
            }
            
            System.out.println("\n--- ВСЕ НАЗНАЧЕНИЯ ---");
            System.out.printf("%-15s %-20s %-10s %-10s %-20s %s\n", 
                "USERNAME", "ROLE", "TYPE", "STATUS", "ASSIGNED AT", "ID");
            System.out.println("-------------------------------------------------------------");
            
            for (var a : assignments) {
                String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("%-15s %-20s %-10s %-10s %-20s %s\n", 
                    a.user().username(),
                    a.role().getName(),
                    a.assignmentType(),
                    status,
                    a.metadata().assignedAt(),
                    a.assignmentId());
            }
            System.out.println("Всего: " + assignments.size());
        });


        parser.registerCommand("assignment-list-user", "Назначения пользователя", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Введите username", true);
            
            var userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("Пользователь не найден");
                return;
            }
            
            var assignments = sys.getAssignmentManager().findByUser(userOpt.get());
            
            if (assignments.isEmpty()) {
                System.out.println("У пользователя нет назначений");
                return;
            }
            
            System.out.println("\n--- НАЗНАЧЕНИЯ ПОЛЬЗОВАТЕЛЯ " + username + " ---");
            System.out.printf("%-20s %-15s %-10s %-10s %s\n", "ROLE", "TYPE", "STATUS", "ASSIGNED AT", "ID");
            System.out.println("-------------------------------------------------------------");
            
            for (var a : assignments) {
                String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                String assignedAt = a.metadata().assignedAt();
                System.out.printf("%-20s %-15s %-10s %-10s %s\n", 
                    a.role().getName(),
                    a.assignmentType(),
                    status,
                    assignedAt,
                    a.assignmentId());
            }
        });
        

        parser.registerCommand("assignment-list-role", "Список пользователей с конкретной ролью", (scanner, sys) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Введите имя роли", true);
            
            var roleOpt = sys.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                System.out.println("Роль не найдена");
                return;
            }
            
            Role role = roleOpt.get();
            var assignments = sys.getAssignmentManager().findByRole(role);
            
            if (assignments.isEmpty()) {
                System.out.println("Нет пользователей с ролью " + roleName);
                return;
            }
            
            System.out.println("\n--- ПОЛЬЗОВАТЕЛИ С РОЛЬЮ " + roleName + " ---");
            System.out.printf("%-15s %-10s %-20s %s\n", 
                "USERNAME", "STATUS", "ASSIGNED AT", "ID");
            System.out.println("-------------------------------------------------------------");
            
            for (var a : assignments) {
                String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("%-15s %-10s %-20s %s\n", 
                    a.user().username(),
                    status,
                    a.metadata().assignedAt(),
                    a.assignmentId());
            }
        });
        

        parser.registerCommand("assignment-active", "Только активные назначения", (scanner, sys) -> {
            var active = sys.getAssignmentManager().getActiveAssignments();
            
            if (active.isEmpty()) {
                System.out.println("Нет активных назначений");
                return;
            }
            
            System.out.println("\n--- АКТИВНЫЕ НАЗНАЧЕНИЯ ---");
            System.out.printf("%-15s %-20s %-15s %-15s %s\n", 
                "USER", "ROLE", "TYPE", "EXPIRES/RELATIVE", "ASSIGNED AT");
            System.out.println("-------------------------------------------------------------");
            
            for (var a : active) {
                if (a instanceof TemporaryAssignment temp) {
                    System.out.printf("%-15s %-20s %-15s %-15s %s\n", 
                        a.user().username(),
                        a.role().getName(),
                        a.assignmentType(),
                        temp.getExpiresAt() + " (" + DateUtils.formatRelativeTime(temp.getExpiresAt()) + ")",
                        a.metadata().assignedAt());
                } else {
                    System.out.printf("%-15s %-20s %-15s %-15s %s\n", 
                        a.user().username(),
                        a.role().getName(),
                        a.assignmentType(),
                        "PERMANENT",
                        a.metadata().assignedAt());
                }
            }
        });
        

        parser.registerCommand("assignment-expired", "Истекшие назначения", (scanner, sys) -> {
            var expired = sys.getAssignmentManager().getExpiredAssignments();
            
            if (expired.isEmpty()) {
                System.out.println("Нет истекших назначений");
                return;
            }
            
            System.out.println("\n--- ИСТЕКШИЕ НАЗНАЧЕНИЯ ---");
            System.out.printf("%-15s %-20s %-15s %-15s %s\n","USER", "ROLE", "EXPIRES", "RELATIVE", "ASSIGNED AT");
            System.out.println("-------------------------------------------------------------");
            
            for (var a : expired) {
                if (a instanceof TemporaryAssignment temp) {
                    System.out.printf("%-15s %-20s %-15s %-15s %s\n", 
                        a.user().username(),
                        a.role().getName(),
                        temp.getExpiresAt(),
                        DateUtils.formatRelativeTime(temp.getExpiresAt()),
                        a.metadata().assignedAt());
                }
            }
        });


        parser.registerCommand("assignment-extend", "Продлить временное назначение", (scanner, sys) -> {
            System.out.println("Выберите способ поиска назначения:");
            System.out.println("1. По ID назначения");
            System.out.println("2. По username + роль");
            
            int searchChoice = ConsoleUtils.promptInt(scanner, "Выбор", 1, 2);
            RoleAssignment assignment = null;
            
            try {
                if (searchChoice == 1) {
                    String id = ConsoleUtils.promptString(scanner, "Введите ID назначения", true);
                    var opt = sys.getAssignmentManager().findById(id);
                    if (opt.isEmpty()) {
                        System.out.println("Назначение не найдено");
                        return;
                    }
                    assignment = opt.get();
                } else {
                    String username = ConsoleUtils.promptString(scanner, "Введите username", true);
                    String roleName = ConsoleUtils.promptString(scanner, "Введите имя роли", true);
                    
                    var userOpt = sys.getUserManager().findByUsername(username);
                    var roleOpt = sys.getRoleManager().findByName(roleName);
                    
                    if (userOpt.isEmpty() || roleOpt.isEmpty()) {
                        System.out.println("Пользователь или роль не найдены");
                        return;
                    }
                    
                    var assignments = sys.getAssignmentManager().findByUser(userOpt.get()).stream()
                        .filter(a -> a.role().getId().equals(roleOpt.get().getId()))
                        .filter(RoleAssignment::isActive)
                        .toList();
                    
                    if (assignments.isEmpty()) {
                        System.out.println("Активное назначение не найдено");
                        return;
                    }
                    
                    if (assignments.size() > 1) {
                        System.out.println("Найдено несколько назначений. Используйте поиск по ID.");
                        return;
                    }
                    
                    assignment = assignments.get(0);
                }
                
                if (!(assignment instanceof TemporaryAssignment temp)) {
                    System.out.println("Можно продлить только временные назначения");
                    return;
                }
                
                System.out.println("Текущая дата истечения: " + temp.getExpiresAt());
                System.out.println("(Относительно сегодня: " + DateUtils.formatRelativeTime(temp.getExpiresAt()) + ")");
                
                String newDate = ConsoleUtils.promptString(scanner, "Введите новую дату истечения (ГГГГ-ММ-ДД)", true);
                
                // Проверяем, что новая дата позже текущей
                if (DateUtils.isBefore(newDate, DateUtils.getCurrentDate())) {
                    System.out.println("Предупреждение: новая дата раньше сегодняшнего дня!");
                    if (!ConsoleUtils.promptYesNo(scanner, "Всё равно продолжить")) {
                        return;
                    }
                }
                
                sys.getAssignmentManager().extendTemporaryAssignment(assignment.assignmentId(), newDate);
                System.out.println("Назначение продлено до " + newDate);
                System.out.println("(Относительно сегодня: " + DateUtils.formatRelativeTime(newDate) + ")");
                
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });


        parser.registerCommand("assignment-search", "Поиск назначений по фильтрам", (scanner, sys) -> {
            System.out.println("\n--- ПОИСК НАЗНАЧЕНИЙ ---");
            System.out.println("1. По пользователю");
            System.out.println("2. По роли");
            System.out.println("3. По типу (постоянное/временное)");
            System.out.println("4. По статусу (активное/неактивное)");
            System.out.println("5. Назначенные после даты");
            System.out.println("6. Истекающие до даты");
            
            int choice = ConsoleUtils.promptInt(scanner, "Выберите фильтр", 1, 6);
            
            AssignmentFilter filter = null;
            String filterDescription = "";
            
            switch (choice) {
                case 1:
                    String username = ConsoleUtils.promptString(scanner, "Введите username", true);
                    filter = a -> a.user().username().equals(username);
                    filterDescription = "пользователь = " + username;
                    break;
                    
                case 2:
                    String roleName = ConsoleUtils.promptString(scanner, "Введите имя роли", true);
                    filter = a -> a.role().getName().equalsIgnoreCase(roleName);
                    filterDescription = "роль = " + roleName;
                    break;
                    
                case 3:
                    int typeChoice = ConsoleUtils.promptInt(scanner, "Тип (1 - постоянное, 2 - временное)", 1, 2);
                    if (typeChoice == 1) {
                        filter = a -> "PERMANENT".equals(a.assignmentType());
                        filterDescription = "тип = PERMANENT";
                    } else {
                        filter = a -> "TEMPORARY".equals(a.assignmentType());
                        filterDescription = "тип = TEMPORARY";
                    }
                    break;
                    
                case 4:
                    int statusChoice = ConsoleUtils.promptInt(scanner, "Статус (1 - активные, 2 - неактивные)", 1, 2);
                    if (statusChoice == 1) {
                        filter = RoleAssignment::isActive;
                        filterDescription = "статус = ACTIVE";
                    } else {
                        filter = a -> !a.isActive();
                        filterDescription = "статус = INACTIVE";
                    }
                    break;
                    
                case 5:
                    String afterDate = ConsoleUtils.promptString(scanner, "Введите дату (ГГГГ-ММ-ДД)", true);
                    filter = AssignmentFilters.assignedAfter(afterDate);
                    filterDescription = "назначено после " + afterDate;
                    break;
                    
                case 6:
                    String beforeDate = ConsoleUtils.promptString(scanner, "Введите дату (ГГГГ-ММ-ДД)", true);
                    filter = AssignmentFilters.expiringBefore(beforeDate);
                    filterDescription = "истекает до " + beforeDate;
                    break;
            }
            
            var results = sys.getAssignmentManager().findByFilter(filter);
            
            if (results.isEmpty()) {
                System.out.println("Назначений, удовлетворяющих фильтру '" + filterDescription + "', не найдено");
                return;
            }
            
            System.out.println("\nНайдено назначений: " + results.size() + " (фильтр: " + filterDescription + ")");
            System.out.printf("%-15s %-20s %-10s %-10s %-20s\n", 
                "USERNAME", "ROLE", "TYPE", "STATUS", "ASSIGNED AT");
            System.out.println("-------------------------------------------------------------");
            
            for (var a : results) {
                String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("%-15s %-20s %-10s %-10s %-20s\n", 
                    a.user().username(),
                    a.role().getName(),
                    a.assignmentType(),
                    status,
                    a.metadata().assignedAt());
            }
        });


        //КОМАНДЫ ПРОСМОТРА ПРАВ
        parser.registerCommand("permissions-user", "Все права пользователя", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Введите username", true);
            
            var userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("Пользователь не найден");
                return;
            }
            
            var permissions = sys.getAssignmentManager().getUserPermissions(userOpt.get());
            
            if (permissions.isEmpty()) {
                System.out.println("У пользователя нет прав");
                return;
            }
            
            System.out.println("\n--- ПРАВА ПОЛЬЗОВАТЕЛЯ " + username + " ---");
            for (var perm : permissions) {
                System.out.println("  " + perm.format());
            }
        });
        

        parser.registerCommand("permissions-check", "Проверить наличие права", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Введите username", true);
            
            var userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("Пользователь не найден");
                return;
            }
            
            String permName = ConsoleUtils.promptString(scanner, "Введите имя права (например READ)", true).toUpperCase();
            String resource = ConsoleUtils.promptString(scanner, "Введите ресурс (например users)", true).toLowerCase();
            
            boolean hasPermission = sys.getAssignmentManager().userHasPermission(
                userOpt.get(), permName, resource);
            
            if (hasPermission) {
                System.out.println(" Пользователь имеет это право");
            } else {
                System.out.println(" Пользователь НЕ имеет это право");
            }
        });


        //СЛУЖЕБНЫЕ КОМАНДЫ
        parser.registerCommand("help", "Показать список всех команд", (scanner, sys) -> {
            parser.printHelp();
        });

        parser.registerCommand("stats", "Статистика системы", (scanner, sys) -> {
            System.out.println(sys.generateStatistics());
        });
        
        parser.registerCommand("clear", "Очистить экран", (scanner, sys) -> {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        });
        
        parser.registerCommand("exit", "Выход из программы", (scanner, sys) -> {
            if (ConsoleUtils.promptYesNo(scanner, "Вы уверены")) {
                System.out.println("До свидания!");
                System.exit(0);
            } else {
                System.out.println("Выход отменен");
            }
        });

        parser.registerCommand("audit-log", "Просмотр журнала аудита", (scanner, sys) -> {
            System.out.println("\n1. Показать все записи");
            System.out.println("2. Показать по исполнителю");
            System.out.println("3. Показать по действию");
            System.out.println("4. Сохранить в файл");
            
            int choice = ConsoleUtils.promptInt(scanner, "Выберите опцию", 1, 4);
            
            switch (choice) {
                case 1:
                    sys.getAuditLog().printLog();
                    break;
                    
                case 2:
                    String performer = ConsoleUtils.promptString(scanner, "Введите имя исполнителя", true);
                    var byPerformer = sys.getAuditLog().getByPerformer(performer);
                    if (byPerformer.isEmpty()) {
                        System.out.println("Записей не найдено");
                    } else {
                        System.out.println("\n=== ЗАПИСИ ДЛЯ " + performer + " ===\n");
                        byPerformer.forEach(e -> 
                            System.out.printf("[%s] %s -> %s: %s\n",
                                e.timestamp(), e.action(), e.target(), e.details()));
                    }
                    break;
                    
                case 3:
                    String action = ConsoleUtils.promptString(scanner, "Введите действие", true);
                    var byAction = sys.getAuditLog().getByAction(action);
                    if (byAction.isEmpty()) {
                        System.out.println("Записей не найдено");
                    } else {
                        System.out.println("\n=== ЗАПИСИ ДЛЯ ДЕЙСТВИЯ " + action + " ===\n");
                        byAction.forEach(e -> 
                            System.out.printf("[%s] %s -> %s: %s\n",
                                e.timestamp(), e.performer(), e.target(), e.details()));
                    }
                    break;
                    
                case 4:
                    String filename = ConsoleUtils.promptString(scanner, "Введите имя файла", true);
                    sys.getAuditLog().saveToFile(filename);
                    break;
            }
        });

        
        //КОМАНДЫ ОТЧЁТОВ
        parser.registerCommand("report-users", "Отчёт по пользователям", (scanner, sys) -> {
            System.out.println("\n1. Вывести в консоль");
            System.out.println("2. Сохранить в файл");
            
            int choice = ConsoleUtils.promptInt(scanner, "Выберите опцию", 1, 2);
            String report = sys.getReportGenerator().generateUserReport(sys.getUserManager(), sys.getAssignmentManager());
            
            if (choice == 1) {
                System.out.println(report);
            } else {
                String filename = ConsoleUtils.promptString(scanner, "Введите имя файла", true);
                sys.getReportGenerator().exportToFile(report, filename);
            }
        });

        parser.registerCommand("report-roles", "Отчёт по ролям", (scanner, sys) -> {
            System.out.println("\n1. Вывести в консоль");
            System.out.println("2. Сохранить в файл");
            
            int choice = ConsoleUtils.promptInt(scanner, "Выберите опцию", 1, 2);
            String report = sys.getReportGenerator().generateRoleReport(sys.getRoleManager(), sys.getAssignmentManager());
            
            if (choice == 1) {
                System.out.println(report);
            } else {
                String filename = ConsoleUtils.promptString(scanner, "Введите имя файла", true);
                sys.getReportGenerator().exportToFile(report, filename);
            }
        });

        parser.registerCommand("report-matrix", "Матрица прав доступа", (scanner, sys) -> {
            System.out.println("\n1. Вывести в консоль");
            System.out.println("2. Сохранить в файл");
            
            int choice = ConsoleUtils.promptInt(scanner, "Выберите опцию", 1, 2);
            String report = sys.getReportGenerator().generatePermissionMatrix(sys.getUserManager(), sys.getAssignmentManager());
            
            if (choice == 1) {
                System.out.println(report);
            } else {
                String filename = ConsoleUtils.promptString(scanner, "Введите имя файла", true);
                sys.getReportGenerator().exportToFile(report, filename);
            }
        });

        // АССИНХРОННЫЕ КОМАНДЫ ОТЧЕТОВ
        parser.registerCommand("report-users-async", "Асинхронный отчёт по пользователям", (scanner, sys) -> {
            System.out.print("Введите имя файла для сохранения: ");
            String filename = scanner.nextLine().trim();
            
            System.out.println("Генерация отчёта запущена в фоновом режиме...");
            
            sys.getReportGenerator().generateUserReportAsync(
                sys.getUserManager(), 
                sys.getAssignmentManager(),
                filename,
                () -> System.out.println(" Отчёт по пользователям сохранён в файл: " + filename)
            );
            
            System.out.println("Команда выполнена. Отчёт генерируется в фоне.");
        });

        parser.registerCommand("report-matrix-async", "Асинхронная матрица прав", (scanner, sys) -> {
            System.out.print("Введите имя файла для сохранения: ");
            String filename = scanner.nextLine().trim();
            
            System.out.println("Генерация матрицы запущена в фоновом режиме...");
            
            sys.getReportGenerator().generateMatrixAsync(
                sys.getUserManager(),
                sys.getAssignmentManager(),
                filename,
                () -> System.out.println(" Матрица прав сохранена в файл: " + filename)
            );
            
            System.out.println("Команда выполнена. Матрица генерируется в фоне.");
        });
    }
}