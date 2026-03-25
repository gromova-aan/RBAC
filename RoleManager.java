
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RoleManager implements Repository<Role> {
    private final Map<String, Role> rolesById = new ConcurrentHashMap<>(); //хранит роли
    private final Map<String, Role> rolesByName = new ConcurrentHashMap<>(); //индекс для быстрого поиска по имени
    
    private AssignmentManager assignmentManager;
    
    public RoleManager() {
    }
    
    public RoleManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }
    
    public void setAssignmentManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }
    
    @Override
    public void add(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Роль не может быть null");
        }
        
        String roleId = role.getId();
        String roleName = role.getName();
        
        // Атомарные проверки и добавление
        if (rolesById.containsKey(roleId)) {
            throw new IllegalArgumentException(
                "Роль с ID '" + roleId + "' уже существует"
            );
        }
        
        // putIfAbsent - атомарная операция
        Role existingByName = rolesByName.putIfAbsent(roleName, role);
        if (existingByName != null) {
            throw new IllegalArgumentException(
                "Роль с именем '" + roleName + "' уже существует"
            );
        }
        
        rolesById.put(roleId, role);
        rolesByName.put(roleName, role);
    }
    
    @Override
    public boolean remove(Role role) {
        if (role == null) {
            return false;
        }
        
        String roleId = role.getId();
        String roleName = role.getName();
        
        if (!rolesById.containsKey(roleId)) {
            return false;
        }
        
        if (assignmentManager != null && assignmentManager.isRoleAssigned(role)) {
            throw new IllegalStateException(
                "Невозможно удалить роль '" + roleName + "', так как она назначена пользователям"
            );
        }
        
        // Атомарное удаление из обоих мап
        rolesById.remove(roleId);
        rolesByName.remove(roleName);
        
        return true;
    }
    
    public boolean removeByName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return false;
        }
        
        Optional<Role> roleOpt = findByName(roleName);
        if (roleOpt.isPresent()) {
            return remove(roleOpt.get());
        }
        
        return false;
    }
    
    @Override
    public Optional<Role> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        
        Role role = rolesById.get(id.trim());
        return Optional.ofNullable(role);
    }
    
    @Override
    public List<Role> findAll() {
        return new CopyOnWriteArrayList<>(rolesById.values());
    }
    
    @Override
    public int count() {
        return rolesById.size();
    }
    
    @Override
    public void clear() {
        rolesById.clear();
        rolesByName.clear();
    }
    
    public Optional<Role> findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        
        Role role = rolesByName.get(name.trim());
        return Optional.ofNullable(role);
    }
    
    public List<Role> findByFilter(RoleFilter filter) {
        if (filter == null) {
            return findAll();
        }
        
        return rolesById.values().parallelStream()
            .filter(filter::test)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        List<Role> result = findByFilter(filter);
        
        if (sorter != null) {
            result.sort(sorter);
        }
        
        return result;
    }

    public boolean exists(String name) { //сущ ли роль с таким именем
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        return rolesByName.containsKey(name.trim());
    }
    
    public void addPermissionToRole(String roleName, Permission permission) {
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя роли не может быть пустым");
        }
        
        if (permission == null) {
            throw new IllegalArgumentException("Право не может быть null");
        }
        
        // Атомарное обновление
        rolesByName.computeIfPresent(roleName.trim(), (name, role) -> {
            role.addPermission(permission);
            return role;
        });
        
        if (!rolesByName.containsKey(roleName.trim())) {
            throw new IllegalArgumentException(
                "Роль с именем '" + roleName + "' не найдена"
            );
        }
    }
    
    public void removePermissionFromRole(String roleName, Permission permission) {
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя роли не может быть пустым");
        }
        
        if (permission == null) {
            throw new IllegalArgumentException("Право не может быть null");
        }
        
        rolesByName.computeIfPresent(roleName.trim(), (name, role) -> {
            role.removePermission(permission);
            return role;
        });
        
        if (!rolesByName.containsKey(roleName.trim())) {
            throw new IllegalArgumentException(
                "Роль с именем '" + roleName + "' не найдена"
            );
        }
    }
    
    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        if (permissionName == null || resource == null) {
            return new ArrayList<>();
        }
        
        return rolesById.values().parallelStream()
            .filter(role -> role.hasPermission(permissionName, resource))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<Role> findRolesWithPermission(Permission permission) {
        if (permission == null) {
            return new ArrayList<>();
        }
        
        return findRolesWithPermission(permission.name(), permission.resource());
    }
    
    public Set<Permission> getAllPermissions() {
        return rolesById.values().parallelStream()
            .flatMap(role -> role.getPermissions().stream())
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
    
    public boolean hasAnyPermission(String roleName) {
        Optional<Role> roleOpt = findByName(roleName);
        if (roleOpt.isPresent()) {
            return !roleOpt.get().getPermissions().isEmpty();
        }
        return false;
    }
    
    public int getPermissionCount(String roleName) {
        Optional<Role> roleOpt = findByName(roleName);
        if (roleOpt.isPresent()) {
            return roleOpt.get().getPermissions().size();
        }
        return -1;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleManager that = (RoleManager) o;
        return Objects.equals(rolesById, that.rolesById);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(rolesById);
    }
    
    @Override
    public String toString() {
        return String.format("RoleManager{roles=%d}", rolesById.size());
    }
}