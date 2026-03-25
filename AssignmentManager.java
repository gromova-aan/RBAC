import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AssignmentManager implements Repository<RoleAssignment> {
    // Хранилище назначений: ключ - assignmentId, значение - RoleAssignment (потокобезопасная Map)
    private final Map<String, RoleAssignment> assignments = new ConcurrentHashMap<>();

    private UserManager userManager;
    private RoleManager roleManager;
    
    public AssignmentManager() {
    }
    
    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.userManager = userManager;
        this.roleManager = roleManager;
    }
    
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }
    
    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Назначение не может быть null");
        }
        
        String assignmentId = assignment.assignmentId();
        
        // атомарная проверка существования
        if (assignments.containsKey(assignmentId)) {
            throw new IllegalArgumentException(
                "Назначение с ID '" + assignmentId + "' уже существует"
            );
        }
        
        // проверяем существование пользователя и роли (если менеджеры доступны)
        if (userManager != null) {
            String username = assignment.user().username();
            if (!userManager.exists(username)) {
                throw new IllegalArgumentException(
                    "Пользователь '" + username + "' не существует"
                );
            }
        }
        
        if (roleManager != null) {
            String roleName = assignment.role().getName();
            if (!roleManager.exists(roleName)) {
                throw new IllegalArgumentException(
                    "Роль '" + roleName + "' не существует"
                );
            }
        }
        
        // проверяем на дублирование активного назначения (параллельно для производительности)
        String username = assignment.user().username();
        String roleId = assignment.role().getId();
        
        boolean hasDuplicate = assignments.values().parallelStream()
            .anyMatch(existing -> 
                existing.user().username().equals(username) && 
                existing.role().getId().equals(roleId) && 
                existing.isActive()
            );
        
        if (hasDuplicate) {
            throw new IllegalArgumentException(
                "У пользователя '" + username + "' уже есть активное назначение роли '" + 
                assignment.role().getName() + "'"
            );
        }

        assignments.put(assignmentId, assignment);
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) {
            return false;
        }
        
        String assignmentId = assignment.assignmentId();
        
        // атомарное удаление
        return assignments.remove(assignmentId) != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        
        RoleAssignment assignment = assignments.get(id.trim());
        return Optional.ofNullable(assignment);
    }
    
    @Override
    public List<RoleAssignment> findAll() {
        // возвращаем потокобезопасную копию
        return new CopyOnWriteArrayList<>(assignments.values());
    }

    @Override
    public int count() {
        return assignments.size();
    }
    
    @Override
    public void clear() {
        assignments.clear();
    }

    public List<RoleAssignment> findByUser(User user) { //все назначения для пользователя
        if (user == null) {
            return new ArrayList<>();
        }
        
        String username = user.username();
        
        // параллельная фильтрация для улучшения производительности
        return assignments.values().parallelStream()
            .filter(a -> a.user().username().equals(username))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<RoleAssignment> findByRole(Role role) { //все назначения для роли
        if (role == null) {
            return new ArrayList<>();
        }
        
        String roleId = role.getId();
        
        // параллельная фильтрация для улучшения производительности
        return assignments.values().parallelStream()
            .filter(a -> a.role().getId().equals(roleId))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) {
            return findAll();
        }
        
        // параллельная фильтрация
        return assignments.values().parallelStream()
            .filter(filter::test)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        List<RoleAssignment> result = findByFilter(filter);
        
        if (sorter != null) {
            result.sort(sorter);
        }
        
        return result;
    }

    public List<RoleAssignment> getActiveAssignments() { //только активные назначения
        // параллельная фильтрация
        return assignments.values().parallelStream()
            .filter(RoleAssignment::isActive)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<RoleAssignment> getExpiredAssignments() { //истекшие
        // параллельная фильтрация
        return assignments.values().parallelStream()
            .filter(a -> !a.isActive())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }
        
        String username = user.username();
        String roleId = role.getId();
        
        // параллельная проверка
        return assignments.values().parallelStream()
            .anyMatch(a -> 
                a.user().username().equals(username) && 
                a.role().getId().equals(roleId) && 
                a.isActive()
            );
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        if (user == null || permissionName == null || resource == null) {
            return false;
        }
        
        Set<Permission> userPermissions = getUserPermissions(user);
        
        // параллельная проверка
        return userPermissions.parallelStream()
            .anyMatch(p -> p.name().equals(permissionName) && p.resource().equals(resource));
    }

    public Set<Permission> getUserPermissions(User user) { //получает все права из всех его активных ролей
        if (user == null) {
            return new HashSet<>();
        }
        
        String username = user.username();
        
        // параллельная агрегация прав
        return assignments.values().parallelStream()
            .filter(a -> a.user().username().equals(username) && a.isActive())
            .flatMap(a -> a.role().getPermissions().stream())
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    public void revokeAssignment(String assignmentId) { //отзывает назначение по айди
        if (assignmentId == null || assignmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("ID назначения не может быть пустым");
        }
        
        // атомарное обновление
        assignments.computeIfPresent(assignmentId.trim(), (id, assignment) -> {
            if (!(assignment instanceof PermanentAssignment)) {
                throw new IllegalArgumentException(
                    "Можно отозвать только постоянные назначения"
                );
            }
            
            PermanentAssignment perm = (PermanentAssignment) assignment;
            perm.revoke();
            return perm;
        });
        
        if (!assignments.containsKey(assignmentId.trim())) {
            throw new IllegalArgumentException(
                "Назначение с ID '" + assignmentId + "' не найдено"
            );
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) { //продлевает временное назнач
        if (assignmentId == null || assignmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("ID назначения не может быть пустым");
        }
        
        if (newExpirationDate == null || newExpirationDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Новая дата не может быть пустой");
        }
        
        // атомарное обновление
        assignments.computeIfPresent(assignmentId.trim(), (id, assignment) -> {
            if (!(assignment instanceof TemporaryAssignment)) {
                throw new IllegalArgumentException(
                    "Можно продлить только временные назначения"
                );
            }
            
            TemporaryAssignment temp = (TemporaryAssignment) assignment;
            temp.extend(newExpirationDate);
            return temp;
        });
        
        if (!assignments.containsKey(assignmentId.trim())) {
            throw new IllegalArgumentException(
                "Назначение с ID '" + assignmentId + "' не найдено"
            );
        }
    }

    public boolean isRoleAssigned(Role role) { //назначена ли роль хотя бы одному пользователю
        if (role == null) {
            return false;
        }
        
        String roleId = role.getId();
        
        // параллельная проверка
        return assignments.values().parallelStream()
            .anyMatch(a -> a.role().getId().equals(roleId));
    }
}