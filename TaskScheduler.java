import java.util.concurrent.*;
import java.time.LocalDate;
import java.util.List;

//Класс для выполнения периодических задач с использованием ScheduledExecutorService
public class TaskScheduler {
    
    private final ScheduledExecutorService scheduler;
    private final RBACSystem system;
    private volatile boolean running = true;
    
    public TaskScheduler(RBACSystem system) {
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.system = system;
    }
    
    public void startExpiredAssignmentsChecker(long intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            
            try {
                checkAndMarkExpiredAssignments();
            } catch (Exception e) {
                System.err.println("Ошибка при проверке истёкших назначений: " + e.getMessage());
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }
    
    public void startStatisticsReporter(long intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            
            try {
                printStatisticsReport();
            } catch (Exception e) {
                System.err.println("Ошибка при формировании отчёта статистики: " + e.getMessage());
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }
    
    private void checkAndMarkExpiredAssignments() {
        List<RoleAssignment> allAssignments = system.getAssignmentManager().findAll();
        int expiredCount = 0;

        for (RoleAssignment assignment : allAssignments) {
            if (assignment instanceof TemporaryAssignment) {
                TemporaryAssignment temp = (TemporaryAssignment) assignment;
                
                boolean isExpired = temp.isExpired();
                System.out.println("[DEBUG] Назначение: " + temp.assignmentId() + 
                    ", дата: " + temp.getExpiresAt() + 
                    ", isExpired: " + isExpired +
                    ", isActive: " + temp.isActive());
                
                if (isExpired) {
                    system.getAssignmentManager().remove(temp);
                    expiredCount++;
                    
                    system.getAuditLog().log(
                        "AUTO_EXPIRE",
                        "system",
                        temp.user().username(),
                        "Автоматически истекла роль: " + temp.role().getName()
                    );
                }
            }
        }
        
        if (expiredCount > 0) {
            System.out.println("[Периодическая задача] Истекло назначений: " + expiredCount);
        }
    }
    
    private void printStatisticsReport() {
        int userCount = system.getUserManager().count();
        int roleCount = system.getRoleManager().count();
        int assignmentCount = system.getAssignmentManager().count();
        int activeCount = system.getAssignmentManager().getActiveAssignments().size();
        int expiredCount = assignmentCount - activeCount;
        
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        System.out.println("\n=== ПЕРИОДИЧЕСКИЙ ОТЧЁТ СТАТИСТИКИ [" + timestamp + "] ===");
        System.out.println("Пользователей: " + userCount);
        System.out.println("Ролей: " + roleCount);
        System.out.println("Назначений: " + assignmentCount);
        System.out.println("  - Активных: " + activeCount);
        System.out.println("  - Истекших: " + expiredCount);
        
        // Топ-3 популярных ролей
        var rolePopularity = new java.util.HashMap<String, Integer>();
        for (RoleAssignment a : system.getAssignmentManager().findAll()) {
            String roleName = a.role().getName();
            rolePopularity.put(roleName, rolePopularity.getOrDefault(roleName, 0) + 1);
        }
        
        var sortedRoles = rolePopularity.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(3)
            .toList();
        
        if (!sortedRoles.isEmpty()) {
            System.out.println("Топ-3 популярных ролей:");
            int rank = 1;
            for (var entry : sortedRoles) {
                System.out.println("  " + rank + ". " + entry.getKey() + " (" + entry.getValue() + " назначений)");
                rank++;
            }
        }
        
        System.out.println("==================================================\n");
        
        system.getAuditLog().log(
            "STATS_REPORT",
            "system",
            "all",
            "Пользователей: " + userCount + ", Ролей: " + roleCount + ", Назначений: " + assignmentCount
        );
    }
    
    public void shutdown() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public boolean isRunning() {
        return running && !scheduler.isShutdown();
    }

    public void forceCheckExpired() {
        checkAndMarkExpiredAssignments();
    }
    
    public void forceStatisticsReport() {
        printStatisticsReport();
    }
}