
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RBACSystem {
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private final AuditLog auditLog;
    private final ReportGenerator reportGenerator;
    private final BackgroundExecutor backgroundExecutor;
    private String currentUser; //имя тек. пользователя-админа системы

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager();
        this.auditLog = new AuditLog();
        this.reportGenerator = new ReportGenerator();
        this.backgroundExecutor = new BackgroundExecutor();
        this.currentUser = "system";
    }

    public UserManager getUserManager() {
        return userManager;
    }
    
    public RoleManager getRoleManager() {
        return roleManager;
    }
    
    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public AuditLog getAuditLog() {
        return auditLog;
    }

    public ReportGenerator getReportGenerator() {
        return reportGenerator;
    }

    public BackgroundExecutor getBackgroundExecutor() {
        return backgroundExecutor;
    }
    
    public void setCurrentUser(String username) {
        this.currentUser = username;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    //инициализация начальных данных системы
    public void initialize() {
        Permission readUsers = new Permission("READ", "users", "Просмотр пользователей");
        Permission writeUsers = new Permission("WRITE", "users", "Создание/редактирование пользователей");
        Permission deleteUsers = new Permission("DELETE", "users", "Удаление пользователей");
        
        Permission readRoles = new Permission("READ", "roles", "Просмотр ролей");
        Permission writeRoles = new Permission("WRITE", "roles", "Создание/редактирование ролей");
        Permission deleteRoles = new Permission("DELETE", "roles", "Удаление ролей");
        
        Permission readAssignments = new Permission("READ", "assignments", "Просмотр назначений");
        Permission writeAssignments = new Permission("WRITE", "assignments", "Создание назначений");
        Permission deleteAssignments = new Permission("DELETE", "assignments", "Удаление назначений");
        
        Permission readReports = new Permission("READ", "reports", "Просмотр отчетов");

        Role adminRole = new Role("Admin", "Полный доступ ко всем ресурсам");
        adminRole.addPermission(readUsers);
        adminRole.addPermission(writeUsers);
        adminRole.addPermission(deleteUsers);
        adminRole.addPermission(readRoles);
        adminRole.addPermission(writeRoles);
        adminRole.addPermission(deleteRoles);
        adminRole.addPermission(readAssignments);
        adminRole.addPermission(writeAssignments);
        adminRole.addPermission(deleteAssignments);
        adminRole.addPermission(readReports);

        Role managerRole = new Role("Manager", "Управление пользователями");
        managerRole.addPermission(readUsers);
        managerRole.addPermission(writeUsers);
        managerRole.addPermission(readRoles);
        managerRole.addPermission(readAssignments);
        managerRole.addPermission(readReports);
        
        Role viewerRole = new Role("Viewer", "Только просмотр");
        viewerRole.addPermission(readUsers);
        viewerRole.addPermission(readRoles);
        viewerRole.addPermission(readAssignments);
        viewerRole.addPermission(readReports);

        //добавляем роли в менеджер
        roleManager.add(adminRole);
        roleManager.add(managerRole);
        roleManager.add(viewerRole);
        
        User adminUser = User.validate("admin", "System Administrator", "admin@rbac.local");
        userManager.add(adminUser);
        
        //назначаем роль Admin администратору
        AssignmentMetadata meta = AssignmentMetadata.now(currentUser, "Инициализация системы");
        PermanentAssignment assignment = new PermanentAssignment(adminUser, adminRole, meta);
        assignmentManager.add(assignment);

        //устанавливаем текущего пользователя
        this.currentUser = "admin";
    }

    public String generateStatistics() {
        StringBuilder sb = new StringBuilder();
        
        int userCount = userManager.count();
        int roleCount = roleManager.count();
        int assignmentCount = assignmentManager.count();
        
        List<RoleAssignment> allAssignments = assignmentManager.findAll();
        long activeCount = allAssignments.stream().filter(RoleAssignment::isActive).count();
        long expiredCount = assignmentCount - activeCount;
            
        //подсчет ролей на пользователя
        double avgRolesPerUser = 0;
        if (userCount > 0) {
            int totalRolesAssigned = 0;
            for (User user : userManager.findAll()) {
                totalRolesAssigned += assignmentManager.findByUser(user).size();
            }
            avgRolesPerUser = (double) totalRolesAssigned / userCount;
        }

        //топ-3 самых популярных ролей
        Map<String, Integer> rolePopularity = new HashMap<>();
        
        //cчитаем, сколько раз каждая роль назначена
        for (RoleAssignment assignment : allAssignments) {
            String roleName = assignment.role().getName();
            rolePopularity.put(roleName, rolePopularity.getOrDefault(roleName, 0) + 1);
        }
        
        //cортируем роли по популярности и берем топ-3
        List<Map.Entry<String, Integer>> sortedRoles = rolePopularity.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(3)
            .toList();

        sb.append("Статистика системы: \n");
        sb.append(String.format("Пользователей: %d\n", userCount));
        sb.append(String.format("Ролей: %d\n", roleCount));
        sb.append(String.format("Назначений всего: %d\n", assignmentCount));
        sb.append(String.format("  Активных: %d\n", activeCount));
        sb.append(String.format("  Истекших: %d\n", expiredCount));
        sb.append(String.format("Среднее количество ролей на пользователя: %.2f\n", avgRolesPerUser));

        sb.append("\nТоп-3 самых популярных ролей: \n");
        if (sortedRoles.isEmpty()) {
            sb.append("  Нет назначенных ролей\n");
        } else {
            int place = 1;
            for (Map.Entry<String, Integer> entry : sortedRoles) {
                sb.append(String.format("  %d. %s - %d назначений\n", 
                    place++, entry.getKey(), entry.getValue()));
            }
        }

        return sb.toString();
    }

}
