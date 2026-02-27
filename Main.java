import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== RBAC СИСТЕМА УПРАВЛЕНИЯ ДОСТУПОМ ===\n");
        
        RBACSystem system = new RBACSystem();
        system.initialize();
        
        CommandParser parser = new CommandParser();
        CommandRegistry.registerAllCommands(parser, system);
        
        System.out.println("Система инициализирована. Текущий пользователь: " + system.getCurrentUser());
        System.out.println("Введите 'help' для списка команд.\n");
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            if (input.trim().isEmpty()) continue;
            parser.parseAndExecute(input, scanner, system);
        }
    }
}