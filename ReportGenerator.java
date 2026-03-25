import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReportGenerator {
    
    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("========================================\n");
        sb.append("         ОТЧЁТ ПО ПОЛЬЗОВАТЕЛЯМ        \n");
        sb.append("========================================\n\n");
        
        List<User> users = userManager.findAll();
        
        if (users.isEmpty()) {
            sb.append("Пользователей нет\n");
            return sb.toString();
        }
        
        // Параллельная обработка пользователей
        users.parallelStream().forEach(user -> {
            // Синхронизируем доступ к StringBuilder через synchronized блок
            synchronized (sb) {
                sb.append("┌───────────────────────────────────────┐\n");
                sb.append(String.format("│ %-37s │\n", "Пользователь: " + user.username()));
                sb.append("├───────────────────────────────────────┤\n");
                sb.append(String.format("│ Полное имя: %-28s │\n", user.fullName()));
                sb.append(String.format("│ Email: %-33s │\n", user.email()));
                sb.append("├───────────────────────────────────────┤\n");
            }
            
            List<RoleAssignment> assignments = assignmentManager.findByUser(user);
            
            synchronized (sb) {
                if (assignments.isEmpty()) {
                    sb.append("│ Роли: не назначены                    │\n");
                } else {
                    sb.append("│ Роли:                                  │\n");
                    for (RoleAssignment ra : assignments) {
                        String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                        sb.append(String.format("│   - %-20s [%-8s] │\n", 
                            ra.role().getName(), status));
                    }
                }
                sb.append("└───────────────────────────────────────┘\n\n");
            }
        });
        
        sb.append(String.format("Всего пользователей: %d\n", users.size()));
        
        return sb.toString();
    }
    
    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("========================================\n");
        sb.append("           ОТЧЁТ ПО РОЛЯМ              \n");
        sb.append("========================================\n\n");
        
        List<Role> roles = roleManager.findAll();
        
        if (roles.isEmpty()) {
            sb.append("Ролей нет\n");
            return sb.toString();
        }
        
        sb.append(String.format("%-20s %-30s %-15s %-10s\n", 
            "РОЛЬ", "ОПИСАНИЕ", "КОЛ-ВО ПРАВ", "ПОЛЬЗОВАТЕЛЕЙ"));
        sb.append("----------------------------------------------------------------\n");
        
        // Параллельная обработка ролей
        Map<Role, Long> roleUserCount = roles.parallelStream()
            .collect(Collectors.toConcurrentMap(
                role -> role,
                role -> assignmentManager.findByRole(role).stream()
                    .map(a -> a.user().username())
                    .distinct()
                    .count()
            ));
        
        for (Role role : roles) {
            long userCount = roleUserCount.getOrDefault(role, 0L);
            
            sb.append(String.format("%-20s %-30s %-15d %-10d\n",
                role.getName(),
                truncate(role.getDescription(), 28),
                role.getPermissions().size(),
                userCount));
        }
        
        sb.append("\n");
        sb.append(String.format("Всего ролей: %d\n", roles.size()));
        
        return sb.toString();
    }
    
    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("========================================\n");
        sb.append("         МАТРИЦА ПРАВ ДОСТУПА          \n");
        sb.append("========================================\n\n");
        
        List<User> users = userManager.findAll();
        
        if (users.isEmpty()) {
            sb.append("Пользователей нет\n");
            return sb.toString();
        }
        
        // Параллельный сбор всех ресурсов
        Set<String> allResources = Collections.synchronizedSet(new HashSet<>());
    
        users.parallelStream().forEach(user -> {
            Set<Permission> userPerms = assignmentManager.getUserPermissions(user);
            for (Permission p : userPerms) {
                allResources.add(p.resource());
            }
        });
        
        List<String> resources = new ArrayList<>(allResources);
        Collections.sort(resources);
        
        // Заголовок таблицы
        sb.append(String.format("%-15s", "ПОЛЬЗОВАТЕЛЬ"));
        for (String resource : resources) {
            sb.append(String.format(" %-12s", resource));
        }
        sb.append("\n");
        
        // Разделитель
        sb.append(String.format("%-15s", "-------------"));
        for (int i = 0; i < resources.size(); i++) {
            sb.append(" -------------");
        }
        sb.append("\n");
        
        // Параллельная обработка пользователей для матрицы
        // Используем Map для безопасного сбора результатов
        Map<String, String> userPermissionsMatrix = new ConcurrentHashMap<>();
        
        users.parallelStream().forEach(user -> {
            Set<Permission> userPerms = assignmentManager.getUserPermissions(user);
            StringBuilder rowBuilder = new StringBuilder();
            
            for (String resource : resources) {
                StringBuilder permStr = new StringBuilder();
                for (Permission p : userPerms) {
                    if (p.resource().equals(resource)) {
                        permStr.append(p.name().charAt(0));
                    }
                }
                if (permStr.length() == 0) {
                    permStr.append("-");
                }
                rowBuilder.append(String.format(" %-12s", permStr.toString()));
            }
            
            userPermissionsMatrix.put(user.username(), rowBuilder.toString());
        });
        
        // Выводим результаты в правильном порядке
        for (User user : users) {
            sb.append(String.format("%-15s", user.username()));
            sb.append(userPermissionsMatrix.getOrDefault(user.username(), ""));
            sb.append("\n");
        }
        
        // Легенда
        sb.append("\nЛегенда: R=READ, W=WRITE, D=DELETE, и т.д.\n");
        
        return sb.toString();
    }
    
    public void exportToFile(String report, String filename) {
        try {
            Files.write(Paths.get(filename), report.getBytes());
            System.out.println("Отчёт сохранён в файл: " + filename);
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении отчёта: " + e.getMessage());
        }
    }
    
    //Генерирует отчёт асинхронно (для использования в отдельном потоке)
    public void generateUserReportAsync(UserManager userManager, AssignmentManager assignmentManager, 
                                        String filename, Runnable onComplete) {
        new Thread(() -> {
            try {
                String report = generateUserReport(userManager, assignmentManager);
                exportToFile(report, filename);
                if (onComplete != null) {
                    onComplete.run();
                }
            } catch (Exception e) {
                System.err.println("Ошибка при асинхронной генерации отчёта: " + e.getMessage());
            }
        }).start();
    }
    
    //Генерирует матрицу прав асинхронно
    public void generateMatrixAsync(UserManager userManager, AssignmentManager assignmentManager,
                                    String filename, Runnable onComplete) {
        new Thread(() -> {
            try {
                String report = generatePermissionMatrix(userManager, assignmentManager);
                exportToFile(report, filename);
                if (onComplete != null) {
                    onComplete.run();
                }
            } catch (Exception e) {
                System.err.println("Ошибка при асинхронной генерации матрицы: " + e.getMessage());
            }
        }).start();
    }

    //Обрезает длинную строку
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}