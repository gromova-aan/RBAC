import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

//Класс для логирования действий пользователей.
public class AuditLog {

    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>(); //записи аудита
    
    // Очередь для асинхронного логирования
    private final BlockingQueue<AuditEntry> logQueue = new LinkedBlockingQueue<>();
    
    // Флаг работы фонового логгера
    private volatile boolean running = true;
    
    // Исполнитель для фоновой записи
    private final ExecutorService loggerExecutor;
    
    public AuditLog() {
        // Создаём один поток для обработки очереди
        this.loggerExecutor = Executors.newSingleThreadExecutor();
        
        // Запускаем фоновый обработчик
        loggerExecutor.submit(this::processLogQueue);
    }
    
    public void log(String action, String performer, String target, String details) {
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        AuditEntry entry = new AuditEntry(timestamp, action, performer, target, details);
        
        // Добавляем в коллекцию для синхронного доступа
        entries.add(entry);
    }
    
    private void processLogQueue() {
        while (running) {
            try {
                AuditEntry entry = logQueue.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }
    
    public List<AuditEntry> getByPerformer(String performer) {
        return entries.parallelStream()
            .filter(e -> e.performer().equals(performer))
            .collect(Collectors.toList());
    }
    
    public List<AuditEntry> getByAction(String action) {
        return entries.parallelStream()
            .filter(e -> e.action().equals(action))
            .collect(Collectors.toList());
    }
    
    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("Лог пуст");
            return;
        }
        
        System.out.println("\n=== ЖУРНАЛ АУДИТА ===\n");
        System.out.printf("%-20s %-15s %-15s %-20s %s\n", 
            "TIMESTAMP", "ACTION", "PERFORMER", "TARGET", "DETAILS");
        System.out.println("------------------------------------------------------------");
        
        for (AuditEntry entry : entries) {
            System.out.printf("%-20s %-15s %-15s %-20s %s\n",
                entry.timestamp(),
                entry.action(),
                entry.performer(),
                entry.target(),
                entry.details());
        }
    }
    
    public void saveToFile(String filename) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("TIMESTAMP,ACTION,PERFORMER,TARGET,DETAILS");
            
            for (AuditEntry entry : entries) {
                lines.add(String.format("%s,%s,%s,%s,%s",
                    entry.timestamp(),
                    entry.action(),
                    entry.performer(),
                    entry.target(),
                    entry.details()));
            }
            
            Files.write(Paths.get(filename), lines);
            System.out.println("Лог сохранен в файл: " + filename);
            
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении лога: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        running = false;
        loggerExecutor.shutdown();
    }
}