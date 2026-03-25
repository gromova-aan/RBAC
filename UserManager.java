
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserManager implements Repository<User> {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    
    @Override
    public void add(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        
        String username = user.username();
        
        //putIfAbsent - атомарная операция: добавит только если ключа нет
        User existing = users.putIfAbsent(username, user);
        if (existing != null) {
            throw new IllegalArgumentException(
                "Пользователь с username '" + username + "' уже существует"
            );
        }
    }
    
    @Override
    public boolean remove(User user) {
        if (user == null) {
            return false;
        }
        
        String username = user.username();
        return users.remove(username, user); //атомарное удаление
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
        // Возвращаем копию, чтобы не блокировать оригинал
        return new CopyOnWriteArrayList<>(users.values());
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
        
        //Используем параллельный поиск для больших коллекций
        return users.values().parallelStream()
            .filter(user -> searchEmail.equals(user.email()))
            .findFirst();
    }
    
    public List<User> findByFilter(UserFilter filter) {
        if (filter == null) {
            return findAll();
        }
        
        //Параллельная фильтрация для улучшения производительности
        return users.values().parallelStream()
            .filter(filter::test)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }


    //Параллельная фильтрация пользователей
    public List<User> findByFilterParallel(UserFilter filter) {
        if (filter == null) {
            return findAll();
        }
        
        return users.values().parallelStream()
            .filter(filter::test)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        List<User> result = findByFilter(filter);
        
        if (sorter != null) {
            result.sort(sorter);
        }
        
        return result;
    }
    
    public boolean exists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        return users.containsKey(username.trim());
    }
    
    public void update(String username, String newFullName, String newEmail) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username не может быть пустым");
        }
        
        String trimmedUsername = username.trim();
        
        // Атомарно получаем и обновляем
        users.compute(trimmedUsername, (key, existingUser) -> {
            if (existingUser == null) {
                throw new IllegalArgumentException(
                    "Пользователь с username '" + trimmedUsername + "' не найден"
                );
            }
            
            User updatedUser = User.validate(trimmedUsername, newFullName, newEmail);
            return updatedUser;
        });
    }
    
    public void updateFullName(String username, String newFullName) {
        User user = findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException(
                "Пользователь с username '" + username + "' не найден"
            ));
        
        update(username, newFullName, user.email());
    }
    
    public void updateEmail(String username, String newEmail) {
        User user = findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException(
                "Пользователь с username '" + username + "' не найден"
            ));
        
        update(username, user.fullName(), newEmail);
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
    
    @Override
    public String toString() {
        return String.format("UserManager{users=%d}", users.size());
    }
}