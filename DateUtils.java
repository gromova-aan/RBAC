import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private static final DateTimeFormatter DATETIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }
    
    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }
    
    //true если date1 < date2
    public static boolean isBefore(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) < 0;
    }
    
    public static boolean isAfter(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) > 0;
    }
    
    //Добавляет дни к дате
    public static String addDays(String date, int days) {
        if (date == null) return getCurrentDate();
        
        try {
            LocalDate ld = LocalDate.parse(date, DATE_FORMATTER);
            return ld.plusDays(days).format(DATE_FORMATTER);
        } catch (Exception e) {
            return getCurrentDate();
        }
    }
    
    public static String formatRelativeTime(String date) {
        if (date == null) return "неизвестно";
        
        try {
            LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);
            LocalDate today = LocalDate.now();
            
            long days = ChronoUnit.DAYS.between(today, targetDate);
            
            if (days < 0) {
                long absDays = Math.abs(days);
                if (absDays == 0) return "сегодня";
                if (absDays == 1) return "вчера";
                if (absDays < 7) return absDays + " дня назад";
                if (absDays < 30) return (absDays / 7) + " недель назад";
                if (absDays < 365) return (absDays / 30) + " месяцев назад";
                return (absDays / 365) + " лет назад";
            } else if (days > 0) {
                if (days == 1) return "завтра";
                if (days < 7) return "через " + days + " дня";
                if (days < 30) return "через " + (days / 7) + " недель";
                if (days < 365) return "через " + (days / 30) + " месяцев";
                return "через " + (days / 365) + " лет";
            } else {
                return "сегодня";
            }
        } catch (Exception e) {
            return "неверная дата";
        }
    }
}