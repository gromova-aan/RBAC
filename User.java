
public record User (
    String username,
    String fullName,
    String email
) {
    public static User validate(String username, String fullName, String email) {

        ValidationUtils.requireNonNullEmpty(username, "Username");
        ValidationUtils.requireNonNullEmpty(fullName, "Full name");
        ValidationUtils.requireNonNullEmpty(email, "Email");
        
        if (!ValidationUtils.isValidUsername(username)) {
            throw new IllegalArgumentException(
                "Username должен содержать только латинские буквы, цифры и подчеркивание, " +
                "длина от 3 до 20 символов"
            );
        }
        
        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException(
                "Неверный формат email"
            );
        }
        
        String normUsername = ValidationUtils.normalizeString(username);
        String normFullName = ValidationUtils.normalizeString(fullName);
        String normEmail = ValidationUtils.normalizeString(email).toLowerCase();
        
        return new User(normUsername, normFullName, normEmail);
    }
    
    public String format() {
        return String.format("%s (%s) <%s>", username, fullName, email);
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


