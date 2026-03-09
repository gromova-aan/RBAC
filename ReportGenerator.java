import java.io.*;
import java.nio.file.*;
import java.util.*;

//Класс для генерации отчётов по системе RBAC
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
        
        for (User user : users) {
            sb.append("┌───────────────────────────────────────┐\n");
            sb.append(String.format("│ %-37s │\n", "Пользователь: " + user.username()));
            sb.append("├───────────────────────────────────────┤\n");
            sb.append(String.format("│ Полное имя: %-28s │\n", user.fullName()));
            sb.append(String.format("│ Email: %-33s │\n", user.email()));
            sb.append("├───────────────────────────────────────┤\n");
            
            List<RoleAssignment> assignments = assignmentManager.findByUser(user);
            
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
        
        for (Role role : roles) {
            List<RoleAssignment> assignments = assignmentManager.findByRole(role);
            long userCount = assignments.stream()
                .map(a -> a.user().username())
                .distinct()
                .count();
            
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
        
        // Собираем все уникальные ресурсы
        Set<String> allResources = new TreeSet<>();
        for (User user : users) {
            Set<Permission> perms = assignmentManager.getUserPermissions(user);
            for (Permission p : perms) {
                allResources.add(p.resource());
            }
        }
        
        List<String> resources = new ArrayList<>(allResources);
        
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
        
        // Строки для каждого пользователя
        for (User user : users) {
            sb.append(String.format("%-15s", user.username()));
            
            Set<Permission> userPerms = assignmentManager.getUserPermissions(user);
            
            for (String resource : resources) {
                String permStr = "";
                for (Permission p : userPerms) {
                    if (p.resource().equals(resource)) {
                        permStr += p.name().charAt(0);
                    }
                }
                if (permStr.isEmpty()) {
                    permStr = "-";
                }
                sb.append(String.format(" %-12s", permStr));
            }
            sb.append("\n");
        }
        
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

    //Обрезает длинную строку
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}