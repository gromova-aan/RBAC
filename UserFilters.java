public class UserFilters {
    public static UserFilter byUsername(String username) { //точное совпадение
        if (username == null || username.trim().isEmpty()) {
            return user -> true;
        }

        final String searchUsername = username.trim();

        return user -> searchUsername.equals(user.username());
    }

    public static UserFilter byUsernameContains(String substring) { //содержит подстроку (игнорируя регистр)
        if (substring == null || substring.trim().isEmpty()) {
            return user -> true;
        }

        final String searchSubstring = substring.trim().toLowerCase();

        return user -> user.username().toLowerCase().contains(searchSubstring);
    }

    public static UserFilter byEmail(String email) { //точное совпадения по email
        if (email == null || email.trim().isEmpty()) {
            return user -> true;
        }
        
        final String searchEmail = email.trim();
        
        return user -> searchEmail.equals(user.email());
    }

    public static UserFilter byEmailDomain(String domain) { //email заканчивается на домен (например, "@company.com")
        if (domain == null || domain.trim().isEmpty()) {
            return user -> true;
        }
        
        final String searchDomain = domain.trim().toLowerCase();
        
        return user -> user.email().toLowerCase().endsWith(searchDomain);
    }

    public static UserFilter byFullNameContains(String substring) { //полное имя содержит подстроку
        if (substring == null || substring.trim().isEmpty()) {
            return user -> true;
        }
        
        final String searchSubstring = substring.trim().toLowerCase();
        
        return user -> user.fullName().toLowerCase().contains(searchSubstring);
    }
}
