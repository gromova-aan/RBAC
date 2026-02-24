
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class UserManager implements Repository<User> {
    private final Map<String, User> users = new HashMap<>(); //хранилище пользователей (ключ - username, значение - User)

    @Override
    public void add(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        
        String username = user.username();
        
        if (users.containsKey(username)) {
            throw new IllegalArgumentException(
                "Пользователь с username '" + username + "' уже существует"
            );
        }

        users.put(username, user);
    }

    @Override
    public boolean remove(User user) {
        if (user == null) {
            return false;
        }
        
        String username = user.username();
        
        //только если пользователь существует
        if (users.containsKey(username)) {
            users.remove(username);
            return true;
        }
        
        return false;
    }

    @Override
    public Optional<User> findById(String id) {
        //id = username
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        
        User user = users.get(id.trim());
        return Optional.ofNullable(user);
    }

    @Override
    public List<User> findAll() {
        //возвращаем копию списка, чтобы нельзя было изменить извне
        return new ArrayList<>(users.values());
    }
    
    @Override
    public int count() {
        return users.size();
    }
    
    @Override
    public void clear() {
        users.clear();
    }

    public Optional<User> findByUsername(String username) {
        return findById(username);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String searchEmail = email.trim();
        
        for (User user : users.values()) {
            if (searchEmail.equals(user.email())) {
                return Optional.of(user);
            }
        }
        
        return Optional.empty();
    }

    public List<User> findByFilter(UserFilter filter) {
        if (filter == null) {
            return findAll(); //без фильтра возвращаем всех
        }
        
        List<User> result = new ArrayList<>();
        
        for (User user : users.values()) {
            if (filter.test(user)) {
                result.add(user);
            }
        }
        
        return result;
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        List<User> result = findByFilter(filter);
        
        if (sorter != null) {
            result.sort(sorter);
        }
        
        return result;
    }

    public boolean exists(String username) { //сущ. ли пользователь с таким именем
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        return users.containsKey(username.trim());
    }

    public void update(String username, String newFullName, String newEmail) { //обновление данных 
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username не может быть пустым");
        }
        
        String trimmedUsername = username.trim();
        
        User existingUser = users.get(trimmedUsername);
        if (existingUser == null) {
            throw new IllegalArgumentException(
                "Пользователь с username '" + trimmedUsername + "' не найден"
            );
        }
        
        User updatedUser = User.validate(
            trimmedUsername, 
            newFullName, 
            newEmail
        );
        
        //замена старого пользователя новым
        users.put(trimmedUsername, updatedUser);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserManager that = (UserManager) o;
        return Objects.equals(users, that.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(users);
    }
}
