
public class ManagerTest {
    public static void main(String[] args) {
        testUserManager();
        testRoleManager();
        testAssignmentManager();
        System.exit(0);
    }
    
    static void testUserManager() {
        System.out.println("ТЕСТИРОВАНИЕ UserManager:");
        
        //создаем менеджер
        UserManager userManager = new UserManager();
        
        User john = User.validate("Nastya", "Nastya Gromova", "nastya@example.com");
        User jane = User.validate("Kirill", "Kirill Shirokov", "kirill@ya.ru");
   
        userManager.add(john);
        userManager.add(jane);
        System.out.println("    Добавлено пользователей: " + userManager.count());
        
        //поиск по username
        java.util.Optional<User> found = userManager.findByUsername("Nastya");
        if (found.isPresent()) {
            System.out.println("    Найден пользователь: " + found.get().fullName());
        } else {
            System.out.println("    Ошибка: пользователь не найден");
        }
        
        //проверка exists
        System.out.println("    exists('Nastya'): " + userManager.exists("Nastya"));
        System.out.println("    exists('unknown'): " + userManager.exists("unknown"));
        
        //поиск по email
        java.util.Optional<User> byEmail = userManager.findByEmail("kirill@ya.ru");
        if (byEmail.isPresent()) {
            System.out.println("    Найден по email: " + byEmail.get().username());
        }
        
        //обновление
        userManager.update("Nastya", "Nastya Updated", "nastya.new@mail.com");
        java.util.Optional<User> updated = userManager.findByUsername("Nastya");
        if (updated.isPresent()) {
            System.out.println("    Обновлен: " + updated.get().fullName() + 
                ", " + updated.get().email());
        }
        
        //фильтрация
        UserFilter filter = user -> user.email().endsWith("ya.ru");
        java.util.List<User> filtered = userManager.findByFilter(filter);
        System.out.println("    Фильтр (email заканчивается на ya.ru): " + filtered.size());
        
        //Удаление
        userManager.remove(jane);
        System.out.println("    После удаления: " + userManager.count());
        
        //очистка
        userManager.clear();
        System.out.println("    После очистки: " + userManager.count());
        
        System.out.println();
    }
    
    static void testRoleManager() {
        System.out.println("ТЕСТИРОВАНИЕ RoleManager:");
        
        RoleManager roleManager = new RoleManager();
      
        Permission read = new Permission("READ", "users", "Чтение");
        Permission write = new Permission("WRITE", "users", "Запись");
        
        Role admin = new Role("Administrator", "Админ");
        admin.addPermission(read);
        admin.addPermission(write);
        
        Role viewer = new Role("Viewer", "Читатель");
        viewer.addPermission(read);
        
        //добавление
        roleManager.add(admin);
        roleManager.add(viewer);
        System.out.println("    Добавлено ролей: " + roleManager.count());
        
        //поиск по имени
        java.util.Optional<Role> found = roleManager.findByName("Administrator");
        if (found.isPresent()) {
            System.out.println("    Найдена роль: " + found.get().getName() + 
                ", прав: " + found.get().getPermissions().size());
        }
        
        //проверка exists
        System.out.println("    exists('Administrator'): " + roleManager.exists("Administrator"));
        System.out.println("    exists('Unknown'): " + roleManager.exists("Unknown"));
        
        //добавление права
        Permission delete = new Permission("DELETE", "users", "Удаление");
        roleManager.addPermissionToRole("Administrator", delete);
        java.util.Optional<Role> updated = roleManager.findByName("Administrator");
        if (updated.isPresent()) {
            System.out.println("    После добавления права: " + 
                updated.get().getPermissions().size() + " прав");
        }
        
        //поиск ролей с правом
        java.util.List<Role> withRead = roleManager.findRolesWithPermission("READ", "users");
        System.out.println("    Ролей с READ: " + withRead.size());
        
        //фильтрация
        RoleFilter filter = r -> r.getPermissions().size() >= 2;
        java.util.List<Role> filtered = roleManager.findByFilter(filter);
        System.out.println("    Ролей с >=2 правами: " + filtered.size());
        
        //удаление
        roleManager.remove(viewer);
        System.out.println("    После удаления: " + roleManager.count());
        
        //очистка
        roleManager.clear();
        System.out.println("    После очистки: " + roleManager.count());
        
        System.out.println();
    }
    
    static void testAssignmentManager() {
        System.out.println("ТЕСТИРОВАНИЕ AssignmentManager:");
        
        AssignmentManager assignmentManager = new AssignmentManager();
        
        User john = User.validate("Nastya", "Nastya Gromova", "nastya@example.com");
        
        Permission read = new Permission("READ", "users", "Чтение");
        Role admin = new Role("Administrator", "Админ");
        admin.addPermission(read);
        
        //создаем метаданные
        AssignmentMetadata meta = AssignmentMetadata.now("system", "Тест");
        
        //добавление постоянного назначения
        PermanentAssignment perm = new PermanentAssignment(john, admin, meta);
        assignmentManager.add(perm);
        System.out.println("    Добавлено назначений: " + assignmentManager.count());
        
        //поиск по ID
        String id = perm.assignmentId();
        java.util.Optional<RoleAssignment> found = assignmentManager.findById(id);
        if (found.isPresent()) {
            System.out.println("    Найдено назначение для: " + 
                found.get().user().username());
        }
        
        //проверка наличия роли
        boolean hasRole = assignmentManager.userHasRole(john, admin);
        System.out.println("    Nastya имеет роль Admin? " + hasRole);
        
        //права пользователя
        java.util.Set<Permission> perms = assignmentManager.getUserPermissions(john);
        System.out.println("    У Nastya прав: " + perms.size());
        
        //проверка права
        boolean hasRead = assignmentManager.userHasPermission(john, "READ", "users");
        System.out.println("    Nstya имеет READ? " + hasRead);
        
        //отзыв назначения
        assignmentManager.revokeAssignment(id);
        System.out.println("    Назначение отозвано");
        
        //удаление
        assignmentManager.remove(perm);
        System.out.println("    После удаления: " + assignmentManager.count());
        
        //очистка
        assignmentManager.clear();
        System.out.println("    После очистки: " + assignmentManager.count());
        
        System.out.println();
    }
}