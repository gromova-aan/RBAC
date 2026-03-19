import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MultiThreadProgressBar {
    
    private static final int THREAD_COUNT = 5;
    private static final int PROGRESS_BAR_LENGTH = 30;
    private static final List<ThreadInfo> threads = new ArrayList<>();
    private static boolean firstDisplay = true;
    
    static class ThreadInfo {
        int number;
        long id;
        int position = 0;
        boolean finished = false;
        long startTime;
        long endTime;
        
        ThreadInfo(int number) {
            this.number = number;
        }
    }
    
    public static void main(String[] args) {
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads.add(new ThreadInfo(i + 1));
        }
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            int index = i;
            new Thread(() -> runCalculation(index)).start();
        }
        
        new Thread(() -> {
            while (!allFinished()) {
                displayProgress();
                sleep(100);
            }
            displayProgress();
            System.out.println("\nВсе потоки завершили работу!");
        }).start();
    }
    
    private static void runCalculation(int index) {
        ThreadInfo info = threads.get(index);
        info.id = Thread.currentThread().getId();
        info.startTime = System.currentTimeMillis();
        
        Random random = new Random();
        for (int i = 1; i <= PROGRESS_BAR_LENGTH; i++) {
            info.position = i;
            sleep(100 + random.nextInt(200));
        }
        
        info.endTime = System.currentTimeMillis();
        info.finished = true;
    }
    
    private static void displayProgress() {
        if (!firstDisplay) {
            clearConsole();
        } else {
            firstDisplay = false;
        }
        
        System.out.println("ПРОГРЕСС ВЫПОЛНЕНИЯ:\n");
        
        for (ThreadInfo info : threads) {
            System.out.printf("Поток #%d [ID: %d] [", 
                info.number, 
                info.id > 0 ? info.id : 0);
            
            for (int j = 1; j <= PROGRESS_BAR_LENGTH; j++) {
                System.out.print(j <= info.position ? "#" : "-");
            }
            
            int percent = (info.position * 100) / PROGRESS_BAR_LENGTH;
            System.out.printf("] %d%%", percent);
            
            if (info.finished) {
                System.out.printf(" (завершён за %d мс)", 
                    info.endTime - info.startTime);
            }
            
            System.out.println();
        }
        System.out.println("\n# - выполнено, - - ожидание");
    }
    
    private static boolean allFinished() {
        for (ThreadInfo info : threads) {
            if (!info.finished) return false;
        }
        return true;
    }
    
    private static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
    
    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}