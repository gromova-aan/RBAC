import java.util.List;
import java.util.Scanner;

//класс для интерактивного взаимодействия с пользователем в консоли
public class ConsoleUtils {
    
    //Запрашивает строку у пользователя
    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(message + ": ");
            String input = scanner.nextLine().trim();
            
            if (!required || !input.isEmpty()) {
                return input;
            }
            System.out.println("Ошибка: значение не может быть пустым");
        }
    }
    
    //Запрашивает целое число в заданном диапазоне
    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message + " [" + min + "-" + max + "]: ");
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Ошибка: введите число от " + min + " до " + max);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: введите целое число");
            }
        }
    }
    
    //Запрашивает подтверждение (да/нет)
    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(message + " (да/нет): ");
            String input = scanner.nextLine().trim().toLowerCase();
            
            if (input.equals("да")) {
                return true;
            } else if (input.equals("нет")) {
                return false;
            }
            System.out.println("Ошибка: введите 'да' или 'нет'");
        }
    }
    
    //Запрашивает выбор элемента из списка
    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Список вариантов не может быть пустым");
        }
        
        System.out.println("\n" + message + ":");
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("  %d. %s\n", i + 1, options.get(i));
        }
        
        while (true) {
            System.out.print("Ваш выбор (1-" + options.size() + "): ");
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= options.size()) {
                    return options.get(choice - 1);
                }
                System.out.println("Ошибка: введите число от 1 до " + options.size());
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: введите целое число");
            }
        }
    }
}