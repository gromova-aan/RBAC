
public class CommandRegistry {
    public static void registerAllCommands(CommandParser parser, RBACSystem system) {
        
        //КОМАНДЫ УПРАВЛЕНИЯ ПОЛЬЗОВАТЕЛЯМИ
        parser.registerCommand("user-list", "Вывести список всех пользователей", (scanner, sys) -> {
            var users = sys.getUserManager().findAll();
            
            if (users.isEmpty()) {
                System.out.println("Пользователей нет");
                return;
            }
            
            System.out.println("\n--- СПИСОК ПОЛЬЗОВАТЕЛЕЙ ---");
            System.out.printf("%-15s %-25s %-30s\n", "USERNAME", "FULL NAME", "EMAIL");
            System.out.println("-------------------------------------------------------------");

            for (User user : users) {
                System.out.printf("%-15s %-25s %-30s\n", 
                    user.username(), 
                    user.fullName(), 
                    user.email());
            }
            System.out.println("Всего: " + users.size());
        });


        parser.registerCommand("user-create", "Создать нового пользователя", (scanner, sys) -> {
            try {
                System.out.print("Введите username (латиница, цифры, _, от 3 до 20 символов): ");
                String username = scanner.nextLine().trim();
                
                System.out.print("Введите полное имя: ");
                String fullName = scanner.nextLine().trim();
                
                System.out.print("Введите email: ");
                String email = scanner.nextLine().trim();
                
                User user = User.validate(username, fullName, email);
                sys.getUserManager().add(user);

                sys.getAuditLog().log("CREATE_USER", sys.getCurrentUser(), username, "Создан пользователь");
                
                System.out.println("Пользователь успешно создан!");
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });
        
        
        parser.registerCommand("user-view", "Просмотр информации о пользователе", (scanner, sys) -> {
            System.out.print("Введите username: ");
            String username = scanner.nextLine().trim();
            
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
            System.out.print("Введите username: ");
            String username = scanner.nextLine().trim();
            
            var userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("Пользователь не найден");
                return;
            }
            
            User user = userOpt.get();
            System.out.println("Текущие данные: " + user.format());
            
            System.out.print("Новое полное имя (Enter - оставить текущее): ");
            String fullName = scanner.nextLine().trim();
            if (fullName.isEmpty()) {
                fullName = user.fullName();
            }
            
            System.out.print("Новый email (Enter - оставить текущий): ");
            String email = scanner.nextLine().trim();
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
            System.out.print("Введите username: ");
            String username = scanner.nextLine().trim();
            
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
            
            System.out.print("Вы уверены? (да/нет): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            
            if (confirm.equals("да")) {
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
            System.out.print("Выберите фильтр (1-4): ");
            
            String choice = scanner.nextLine().trim();
            
            UserFilter filter = null;
            
            switch (choice) {
                case "1":
                    System.out.print("Введите часть username: ");
                    String usernamePart = scanner.nextLine().trim();
                    filter = user -> user.username().toLowerCase().contains(usernamePart.toLowerCase());
                    break;
                case "2":
                    System.out.print("Введите часть email: ");
                    String emailPart = scanner.nextLine().trim();
                    filter = user -> user.email().toLowerCase().contains(emailPart.toLowerCase());
                    break;
                case "3":
                    System.out.print("Введите домен (например @gmail.com): ");
                    String domain = scanner.nextLine().trim();
                    filter = user -> user.email().toLowerCase().endsWith(domain.toLowerCase());
                    break;
                case "4":
                    System.out.print("Введите часть полного имени: ");
                    String namePart = scanner.nextLine().trim();
                    filter = user -> user.fullName().toLowerCase().contains(namePart.toLowerCase());
                    break;
                default:
                    System.out.println("Неверный выбор");
                    return;
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
                System.out.print("Введите название роли: ");
                String name = scanner.nextLine().trim();
                
                System.out.print("Введите описание роли: ");
                String description = scanner.nextLine().trim();
                
                Role role = new Role(name, description);
                sys.getRoleManager().add(role);

                sys.getAuditLog().log("CREATE_ROLE", sys.getCurrentUser(), name, "Создана роль");
                
                System.out.println("Роль создана. ID: " + role.getId());
                
                System.out.print("Добавить права сейчас? (да/нет): ");
                String addNow = scanner.nextLine().trim().toLowerCase();
                
                while (addNow.equals("да")) {
                    System.out.print("Введите имя права (например READ): ");
                    String permName = scanner.nextLine().trim().toUpperCase();
                    
                    System.out.print("Введите ресурс (например users): ");
                    String resource = scanner.nextLine().trim().toLowerCase();
                    
                    System.out.print("Введите описание права: ");
                    String permDesc = scanner.nextLine().trim();
                    
                    try {
                        Permission perm = new Permission(permName, resource, permDesc);
                        sys.getRoleManager().addPermissionToRole(name, perm);
                        System.out.println("Право добавлено");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                    
                    System.out.print("Добавить еще право? (да/нет): ");
                    addNow = scanner.nextLine().trim().toLowerCase();
                }
                
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });


        parser.registerCommand("role-view", "Просмотр информации о роли", (scanner, sys) -> {
            System.out.print("Введите имя роли: ");
            String name = scanner.nextLine().trim();
            
            var roleOpt = sys.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Роль не найдена");
                return;
            }
            
            Role role = roleOpt.get();
            System.out.println(role.format());
        });


        parser.registerCommand("role-update", "Обновить название и описание роли", (scanner, sys) -> {
            System.out.print("Введите имя роли для обновления: ");
            String oldName = scanner.nextLine().trim();
            
            var roleOpt = sys.getRoleManager().findByName(oldName);
            if (roleOpt.isEmpty()) {
                System.out.println("Роль не найдена");
                return;
            }
            
            Role role = roleOpt.get();
            System.out.println("\nТекущие данные роли:");
            System.out.println("  Название: " + role.getName());
            System.out.println("  Описание: " + role.getDescription());
            
            System.out.print("\nВведите новое название (Enter - оставить текущее): ");
            String newName = scanner.nextLine().trim();
            if (newName.isEmpty()) {
                newName = role.getName();
            }
            
            System.out.print("Введите новое описание (Enter - оставить текущее): ");
            String newDescription = scanner.nextLine().trim();
            if (newDescription.isEmpty()) {
                newDescription = role.getDescription();
            }
            
            if (!newName.equals(role.getName()) && sys.getRoleManager().exists(newName)) {
                System.out.println("Ошибка: роль с именем '" + newName + "' уже существует");
                return;
            }
            
            try {
                Role updatedRole = new Role(newName, newDescription);
                
                //копируем все права из старой роли
                for (Permission perm : role.getPermissions()) {
                    updatedRole.addPermission(perm);
                }
                
                // Удаляем старую роль и добавляем новую
                sys.getRoleManager().remove(role);
                sys.getRoleManager().add(updatedRole);
                
                System.out.println("Роль успешно обновлена");
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка при обновлении: " + e.getMessage());
            }
        });


        parser.registerCommand("role-delete", "Удалить роль", (scanner, sys) -> {
            System.out.print("Введите имя роли: ");
            String name = scanner.nextLine().trim();
            
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
                System.out.print("Все равно удалить? (да/нет): ");
                String confirm = scanner.nextLine().trim().toLowerCase();
                if (!confirm.equals("да")) {
                    System.out.println("Удаление отменено");
                    return;
                }
            }
            
            sys.getRoleManager().remove(role);
            sys.getAuditLog().log("DELETE_ROLE", sys.getCurrentUser(), name, "Удалена роль");
            System.out.println("Роль удалена");
        });


        parser.registerCommand("role-add-permission", "Добавить право к роли", (scanner, sys) -> {
            System.out.print("Введите имя роли: ");
            String roleName = scanner.nextLine().trim();
            
            var roleOpt = sys.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                System.out.println("Роль не найдена");
                return;
            }
            
            try {
                System.out.print("Введите имя права (например READ): ");
                String permName = scanner.nextLine().trim().toUpperCase();
                
                System.out.print("Введите ресурс (например users): ");
                String resource = scanner.nextLine().trim().toLowerCase();
                
                System.out.print("Введите описание права: ");
                String permDesc = scanner.nextLine().trim();
                
                Permission perm = new Permission(permName, resource, permDesc);
                sys.getRoleManager().addPermissionToRole(roleName, perm);
                System.out.println("Право добавлено к роли");
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });


        parser.registerCommand("role-remove-permission", "Удалить право из роли", (scanner, sys) -> {
            System.out.print("Введите имя роли: ");
            String roleName = scanner.nextLine().trim();
            
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
            
            System.out.print("\nВведите номер права для удаления (0 - отмена): ");
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice == 0) {
                    System.out.println("Удаление отменено");
                    return;
                }
                
                if (choice < 1 || choice > permissions.size()) {
                    System.out.println("Неверный номер");
                    return;
                }
                
                Permission permToRemove = permissions.get(choice - 1);
                sys.getRoleManager().removePermissionFromRole(roleName, permToRemove);
                System.out.println("Право удалено из роли");
                
            } catch (NumberFormatException e) {
                System.out.println("Ошибка ввода");
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });


        parser.registerCommand("role-search", "Поиск ролей по фильтрам", (scanner, sys) -> {
            System.out.println("\n--- ПОИСК РОЛЕЙ ---");
            System.out.println("1. По имени (содержит)");
            System.out.println("2. По наличию конкретного права");
            System.out.println("3. По минимальному количеству прав");
            System.out.print("Выберите фильтр (1-3): ");
            
            String choice = scanner.nextLine().trim();
            RoleFilter filter = null;
            String filterDescription = "";
            
            switch (choice) {
                case "1":
                    System.out.print("Введите часть имени роли: ");
                    String namePart = scanner.nextLine().trim();
                    filter = role -> role.getName().toLowerCase().contains(namePart.toLowerCase());
                    filterDescription = "имя содержит '" + namePart + "'";
                    break;
                    
                case "2":
                    System.out.print("Введите имя права (например READ): ");
                    String permName = scanner.nextLine().trim().toUpperCase();
                    
                    System.out.print("Введите ресурс (например users): ");
                    String resource = scanner.nextLine().trim().toLowerCase();
                    
                    filter = role -> role.hasPermission(permName, resource);
                    filterDescription = "имеет право " + permName + " on " + resource;
                    break;
                    
                case "3":
                    System.out.print("Введите минимальное количество прав: ");
                    try {
                        int minCount = Integer.parseInt(scanner.nextLine().trim());
                        filter = role -> role.getPermissions().size() >= minCount;
                        filterDescription = "минимум " + minCount + " прав";
                    } catch (NumberFormatException e) {
                        System.out.println("Ошибка: введите число");
                        return;
                    }
                    break;
                    
                default:
                    System.out.println("Неверный выбор");
                    return;
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
            System.out.print("Введите username пользователя: ");
            String username = scanner.nextLine().trim();
            
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
            
            System.out.print("Выберите номер роли: ");
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice < 1 || choice > roles.size()) {
                    System.out.println("Неверный выбор");
                    return;
                }
                
                Role selectedRole = roles.get(choice - 1);
                
                System.out.print("Тип назначения (1 - постоянное, 2 - временное): ");
                String typeChoice = scanner.nextLine().trim();
                
                System.out.print("Причина назначения: ");
                String reason = scanner.nextLine().trim();
                
                AssignmentMetadata meta = AssignmentMetadata.now(sys.getCurrentUser(), reason);
                
                if (typeChoice.equals("1")) {
                    PermanentAssignment assignment = new PermanentAssignment(user, selectedRole, meta);
                    sys.getAssignmentManager().add(assignment);
                    sys.getAuditLog().log("ASSIGN_ROLE", sys.getCurrentUser(), username, "Назначена постоянная роль " + selectedRole.getName());
                    System.out.println("Постоянное назначение создано. ID: " + assignment.assignmentId());
                } else if (typeChoice.equals("2")) {
                    System.out.print("Введите дату истечения (ГГГГ-ММ-ДД): ");
                    String expiresAt = scanner.nextLine().trim();
                    
                    System.out.print("Автопродление? (да/нет): ");
                    String autoRenewStr = scanner.nextLine().trim().toLowerCase();
                    boolean autoRenew = autoRenewStr.equals("да");
                    
                    TemporaryAssignment assignment = new TemporaryAssignment(user, selectedRole, meta, expiresAt, autoRenew);
                    sys.getAssignmentManager().add(assignment);
                    System.out.println("Временное назначение создано. ID: " + assignment.assignmentId());
                } else {
                    System.out.println("Неверный тип назначения");
                }
                
            } catch (NumberFormatException e) {
                System.out.println("Ошибка ввода");
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });


        parser.registerCommand("revoke-role", "Отозвать роль у пользователя", (scanner, sys) -> {
            System.out.print("Введите username: ");
            String username = scanner.nextLine().trim();
            
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
            
            System.out.print("\nВведите номер назначения для отзыва (0 - отмена): ");
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice == 0) {
                    System.out.println("Отмена");
                    return;
                }
                
                if (choice < 1 || choice > assignments.size()) {
                    System.out.println("Неверный номер");
                    return;
                }
                
                RoleAssignment toRevoke = assignments.get(choice - 1);
                
                if (toRevoke instanceof PermanentAssignment) {
                    //для постоянных - отзываем
                    sys.getAssignmentManager().revokeAssignment(toRevoke.assignmentId());
                    sys.getAuditLog().log("REVOKE_ROLE", sys.getCurrentUser(), username, "Отозвана роль " + toRevoke.role().getName());
                    System.out.println("Назначение отозвано");
                } else if (toRevoke instanceof TemporaryAssignment) {
                    //для временных - помечаем как истекшие
                    // Временно удаляем назначение
                    sys.getAssignmentManager().remove(toRevoke);
                    System.out.println("Временное назначение удалено");
                }
                
            } catch (NumberFormatException e) {
                System.out.println("Ошибка ввода");
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
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
            System.out.print("Введите username: ");
            String username = scanner.nextLine().trim();
            
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
            System.out.print("Введите имя роли: ");
            String roleName = scanner.nextLine().trim();
            
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
            System.out.printf("%-15s %-20s %-15s %s\n", "USER", "ROLE", "TYPE", "ASSIGNED AT");
            System.out.println("-------------------------------------------------------------");
            
            for (var a : active) {
                System.out.printf("%-15s %-20s %-15s %s\n", 
                    a.user().username(),
                    a.role().getName(),
                    a.assignmentType(),
                    a.metadata().assignedAt());
            }
        });
        

        parser.registerCommand("assignment-expired", "Истекшие назначения", (scanner, sys) -> {
            var expired = sys.getAssignmentManager().getExpiredAssignments();
            
            if (expired.isEmpty()) {
                System.out.println("Нет истекших назначений");
                return;
            }
            
            System.out.println("\n--- ИСТЕКШИЕ НАЗНАЧЕНИЯ ---");
            System.out.printf("%-15s %-20s %-15s %s\n", "USER", "ROLE", "EXPIRES", "ASSIGNED AT");
            System.out.println("-------------------------------------------------------------");
            
            for (var a : expired) {
                if (a instanceof TemporaryAssignment) {
                    TemporaryAssignment temp = (TemporaryAssignment) a;
                    System.out.printf("%-15s %-20s %-15s %s\n", 
                        a.user().username(),
                        a.role().getName(),
                        temp.getExpiresAt(),
                        a.metadata().assignedAt());
                } else {
                    System.out.printf("%-15s %-20s %-15s %s\n", 
                        a.user().username(),
                        a.role().getName(),
                        "NEVER",
                        a.metadata().assignedAt());
                }
            }
        });


        parser.registerCommand("assignment-extend", "Продлить временное назначение", (scanner, sys) -> {
            System.out.println("Выберите способ поиска назначения:");
            System.out.println("1. По ID назначения");
            System.out.println("2. По username + роль");
            System.out.print("Выбор (1-2): ");
            
            String choice = scanner.nextLine().trim();
            RoleAssignment assignment = null;
            
            try {
                if (choice.equals("1")) {
                    System.out.print("Введите ID назначения: ");
                    String id = scanner.nextLine().trim();
                    var opt = sys.getAssignmentManager().findById(id);
                    if (opt.isEmpty()) {
                        System.out.println("Назначение не найдено");
                        return;
                    }
                    assignment = opt.get();
                } else if (choice.equals("2")) {
                    System.out.print("Введите username: ");
                    String username = scanner.nextLine().trim();
                    
                    var userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        System.out.println("Пользователь не найден");
                        return;
                    }
                    
                    System.out.print("Введите имя роли: ");
                    String roleName = scanner.nextLine().trim();
                    
                    var roleOpt = sys.getRoleManager().findByName(roleName);
                    if (roleOpt.isEmpty()) {
                        System.out.println("Роль не найдена");
                        return;
                    }
                    
                    User user = userOpt.get();
                    Role role = roleOpt.get();
                    
                    var assignments = sys.getAssignmentManager().findByUser(user).stream()
                        .filter(a -> a.role().getId().equals(role.getId()))
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
                } else {
                    System.out.println("Неверный выбор");
                    return;
                }
                
                if (!(assignment instanceof TemporaryAssignment)) {
                    System.out.println("Можно продлить только временные назначения");
                    return;
                }
                
                TemporaryAssignment temp = (TemporaryAssignment) assignment;
                System.out.println("Текущая дата истечения: " + temp.getExpiresAt());
                
                System.out.print("Введите новую дату истечения (ГГГГ-ММ-ДД): ");
                String newDate = scanner.nextLine().trim();
                
                sys.getAssignmentManager().extendTemporaryAssignment(assignment.assignmentId(), newDate);
                System.out.println("Назначение продлено до " + newDate);
                
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
            System.out.print("Выберите фильтр (1-6): ");
            
            String choice = scanner.nextLine().trim();
            AssignmentFilter filter = null;
            String filterDescription = "";
            
            switch (choice) {
                case "1": // По пользователю
                    System.out.print("Введите username: ");
                    String username = scanner.nextLine().trim();
                    filter = a -> a.user().username().equals(username);
                    filterDescription = "пользователь = " + username;
                    break;
                    
                case "2": // По роли
                    System.out.print("Введите имя роли: ");
                    String roleName = scanner.nextLine().trim();
                    filter = a -> a.role().getName().equalsIgnoreCase(roleName);
                    filterDescription = "роль = " + roleName;
                    break;
                    
                case "3": // По типу
                    System.out.println("Тип: 1 - постоянное, 2 - временное");
                    System.out.print("Выбор: ");
                    String typeChoice = scanner.nextLine().trim();
                    if (typeChoice.equals("1")) {
                        filter = a -> "PERMANENT".equals(a.assignmentType());
                        filterDescription = "тип = PERMANENT";
                    } else if (typeChoice.equals("2")) {
                        filter = a -> "TEMPORARY".equals(a.assignmentType());
                        filterDescription = "тип = TEMPORARY";
                    } else {
                        System.out.println("Неверный выбор");
                        return;
                    }
                    break;
                    
                case "4": // По статусу
                    System.out.println("Статус: 1 - активные, 2 - неактивные");
                    System.out.print("Выбор: ");
                    String statusChoice = scanner.nextLine().trim();
                    if (statusChoice.equals("1")) {
                        filter = RoleAssignment::isActive;
                        filterDescription = "статус = ACTIVE";
                    } else if (statusChoice.equals("2")) {
                        filter = a -> !a.isActive();
                        filterDescription = "статус = INACTIVE";
                    } else {
                        System.out.println("Неверный выбор");
                        return;
                    }
                    break;
                    
                case "5": // Назначенные после даты
                    System.out.print("Введите дату (ГГГГ-ММ-ДД): ");
                    String afterDate = scanner.nextLine().trim();
                    filter = AssignmentFilters.assignedAfter(afterDate);
                    filterDescription = "назначено после " + afterDate;
                    break;
                    
                case "6": // Истекающие до даты
                    System.out.print("Введите дату (ГГГГ-ММ-ДД): ");
                    String beforeDate = scanner.nextLine().trim();
                    filter = AssignmentFilters.expiringBefore(beforeDate);
                    filterDescription = "истекает до " + beforeDate;
                    break;
                    
                default:
                    System.out.println("Неверный выбор");
                    return;
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


        //кОМАНДЫ ПРОСМОТРА ПРАВ
        parser.registerCommand("permissions-user", "Все права пользователя", (scanner, sys) -> {
            System.out.print("Введите username: ");
            String username = scanner.nextLine().trim();
            
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
            System.out.print("Введите username: ");
            String username = scanner.nextLine().trim();
            
            var userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("Пользователь не найден");
                return;
            }
            
            System.out.print("Введите имя права (например READ): ");
            String permName = scanner.nextLine().trim().toUpperCase();
            
            System.out.print("Введите ресурс (например users): ");
            String resource = scanner.nextLine().trim().toLowerCase();
            
            boolean hasPermission = sys.getAssignmentManager().userHasPermission(
                userOpt.get(), permName, resource);
            
            if (hasPermission) {
                System.out.println(" Пользователь имеет это право");
            } else {
                System.out.println(" Пользователь НЕ имеет это право");
            }
        });


        //СЛУЖЕБНЫЕ КОМАНДЫ
        parser.registerCommand("help", "Показать список всех команд", (Scanner, sys) -> {
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
            System.out.print("Вы уверены? (да/нет): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (confirm.equals("да")) {
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
            System.out.print("Выберите опцию (1-4): ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    sys.getAuditLog().printLog();
                    break;
                    
                case "2":
                    System.out.print("Введите имя исполнителя: ");
                    String performer = scanner.nextLine().trim();
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
                    
                case "3":
                    System.out.print("Введите действие: ");
                    String action = scanner.nextLine().trim();
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
                    
                case "4":
                    System.out.print("Введите имя файла: ");
                    String filename = scanner.nextLine().trim();
                    sys.getAuditLog().saveToFile(filename);
                    break;
                    
                default:
                    System.out.println("Неверный выбор");
            }
        });
    }
}
