import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

//Класс для выполнения фоновых задач с использованием ExecutorService
public class BackgroundExecutor {
    
    private final ExecutorService executor;
    private final List<Future<?>> activeTasks;
    
    public BackgroundExecutor() {
        //cоздаём пул потоков с фиксированным количеством 
        this.executor = Executors.newFixedThreadPool(5);
        this.activeTasks = new ArrayList<>();
    }
    
    public Future<?> submit(Runnable task) {
        Future<?> future = executor.submit(task);
        synchronized (activeTasks) {
            activeTasks.add(future);
            //очищаем завершённые задачи
            activeTasks.removeIf(Future::isDone);
        }
        return future;
    }
    
    public void submit(Runnable task, Runnable onComplete, Runnable onError) {
        executor.submit(() -> {
            try {
                task.run();
                if (onComplete != null) {
                    onComplete.run();
                }
            } catch (Exception e) {
                System.err.println("Ошибка в фоновой задаче: " + e.getMessage());
                if (onError != null) {
                    onError.run();
                }
            }
        });
    }
    
    public int getActiveTaskCount() {
        synchronized (activeTasks) {
            activeTasks.removeIf(Future::isDone);
            return activeTasks.size();
        }
    }
    
    //Ожидает завершения всех задач
    public boolean awaitTermination(long timeoutSeconds) {
        try {
            return executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public void shutdown() {
        executor.shutdown();
    }
    
    public void shutdownNow() {
        executor.shutdownNow();
    }

    public boolean isTerminated() {
        return executor.isTerminated();
    }

    public void shutdownAndAwaitTermination() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
}