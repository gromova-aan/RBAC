import java.util.regex.Pattern;

public class ValidationUtils {
    
    private static final Pattern USERNAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    
    private static final Pattern DATE_PATTERN = 
        Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    
    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        return USERNAME_PATTERN.matcher(username).matches();
    }
    
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    public static boolean isValidDate(String date) {
        if (date == null) return false;
        if (!DATE_PATTERN.matcher(date).matches()) return false;
        
        String[] parts = date.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);
        
        if (month < 1 || month > 12) return false;
        if (day < 1 || day > 31) return false;
        
        if (month == 2) {
            if (day > 29) return false;
            if (day == 29 && !isLeapYear(year)) return false;
        } else if (month == 4 || month == 6 || month == 9 || month == 11) {
            if (day > 30) return false;
        }
        
        return true;
    }
    
    //Проверяет, является ли год високосным.
    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
    
    public static String normalizeString(String input, boolean toUpperCase) {
        if (input == null) return "";
        String trimmed = input.trim().replaceAll("\\s+", " ");
        if (toUpperCase) {
            return trimmed.toUpperCase();
        } else {
            return trimmed.toLowerCase();
        }
    }
    
    public static String normalizeString(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ");
    }
    
    public static void requireNonNullEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                fieldName + " не может быть пустым"
            );
        }
    }
    
    public static void requireNonNull(Object obj, String fieldName) {
        if (obj == null) {
            throw new IllegalArgumentException(
                fieldName + " не может быть null"
            );
        }
    }
    
    public static void requireInRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                fieldName + " должно быть от " + min + " до " + max
            );
        }
    }
}