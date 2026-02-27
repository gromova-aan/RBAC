
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {
    private final Map<String, Command> commands;  //зарег. команды
    private final Map<String, String> commandDescriptions; //описания команд для справки

    public CommandParser() {
        this.commands = new HashMap<>();
        this.commandDescriptions = new HashMap<>();
    }

    public void registerCommand(String name, String description, Command command) {
        commands.put(name, command);
        commandDescriptions.put(name, description);
    }

    public void printHelp() {
        System.out.println("\nДоступные команды: \n");
        
        commands.keySet().stream()
            .sorted()
            .forEach(name -> {
                String description = commandDescriptions.get(name);
                System.out.printf("- %s\t - %s\n", name, description);
            });
        
        System.out.println();
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        Command command = commands.get(commandName);
        
        if (command != null) {
            try {
                command.execute(scanner, system);
            } catch (Exception e) {
                System.out.println("Ошибка при выполнении команды: " + e.getMessage());
            }
        } else {
            System.out.println("Неизвестная команда. Введите 'help' для списка команд.");
        }
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) { 
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        
        //разделяем ввод на части по пробелам
        String[] parts = input.trim().split("\\s+");
        String commandName = parts[0].toLowerCase();
        
        executeCommand(commandName, scanner, system);
    }
}
