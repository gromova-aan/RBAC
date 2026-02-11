import java.util.regex.Pattern;

public record User (
    String username,
    String fullName,
    String email
) {
    private static final Pattern USERNAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");

    public static User validate(String username, String fullName, String email) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username не может быть пустым");
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("FullName не может быть пустым");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                "Username должен содержать только латинские буквы, цифры и подчеркивание. Длина от 3 до 20 символов."
            );
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException(
                "Email должен содержать @ и точку после @"
            );
        }

        return new User(username, fullName, email);
    }
    
    public String format() {
        return String.format ("%s (%s) <%s>", username, fullName, email);
    }

    public static void main(String[] args) {
        try {
            User user1 = User.validate("gromova_ann", "Gromova Nastya", "nastya@example.com");
            System.out.println("Успешно создан: " + user1.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        try {
            User user2 = User.validate("gr", "Gromova Nastya", "nastya@example.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка (короткий username): " + e.getMessage());
        }

        try {
            User user3 = User.validate("gr@omova_ann", "Gromova Nastya", "nastya@example.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка (спецсимволы): " + e.getMessage());
        }

        try {
            User user4 = User.validate("gromova_ann", "Gromova Nastya", "email");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка (email): " + e.getMessage());
        }
        
        try {
            User user5 = User.validate("", "Gromova Nastya", "nastya@example.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка (пустой username): " + e.getMessage());
        }
    }
}


