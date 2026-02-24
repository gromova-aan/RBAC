
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoleManager implements Repository<Role> {
    private final Map<String, Role> rolesById = new HashMap<>(); //хранить роли

    private final Map<String, Role> rolesByName = new HashMap<>(); //индекс для быстрого поиска по имени

    @Override
    public void add(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Роль не может быть null");
        }
        
        String roleId = role.getId();
        String roleName = role.getName();
        
        if (rolesById.containsKey(roleId)) {
            throw new IllegalArgumentException(
                "Роль с ID '" + roleId + "' уже существует"
            );
        }
        
        if (rolesByName.containsKey(roleName)) {
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
    
        rolesById.remove(roleId);
        rolesByName.remove(roleName);
        
        return true;
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
        return new ArrayList<>(rolesById.values());
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
        
        List<Role> result = new ArrayList<>();
        
        for (Role role : rolesById.values()) {
            if (filter.test(role)) {
                result.add(role);
            }
        }
        
        return result;
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
        
        Role role = rolesByName.get(roleName.trim());
        if (role == null) {
            throw new IllegalArgumentException(
                "Роль с именем '" + roleName + "' не найдена"
            );
        }
        
        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя роли не может быть пустым");
        }
        
        if (permission == null) {
            throw new IllegalArgumentException("Право не может быть null");
        }
        
        Role role = rolesByName.get(roleName.trim());
        if (role == null) {
            throw new IllegalArgumentException(
                "Роль с именем '" + roleName + "' не найдена"
            );
        }
        
        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        if (permissionName == null || resource == null) {
            return new ArrayList<>();
        }
        
        List<Role> result = new ArrayList<>();
        
        for (Role role : rolesById.values()) {
            if (role.hasPermission(permissionName, resource)) {
                result.add(role);
            }
        }
        
        return result;
    }
}
