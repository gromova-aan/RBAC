
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AssignmentManager implements Repository<RoleAssignment>{
    private final Map<String, RoleAssignment> assignments = new HashMap<>();

    @Override
    public void add(RoleAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Назначение не может быть null");
        }
        
        String assignmentId = assignment.assignmentId();
        
        if (assignments.containsKey(assignmentId)) {
            throw new IllegalArgumentException(
                "Назначение с ID '" + assignmentId + "' уже существует"
            );
        }
        
        //проверяем на дублирование активного назначения
        String username = assignment.user().username();
        String roleId = assignment.role().getId();
        
        for (RoleAssignment existing : assignments.values()) {
            if (existing.user().username().equals(username) && 
                existing.role().getId().equals(roleId) && 
                existing.isActive()) {
                
                throw new IllegalArgumentException(
                    "У пользователя '" + username + "' уже есть активное назначение роли '" + 
                    assignment.role().getName() + "'"
                );
            }
        }

        assignments.put(assignmentId, assignment);
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) {
            return false;
        }
        
        String assignmentId = assignment.assignmentId();
        
        if (assignments.containsKey(assignmentId)) {
            assignments.remove(assignmentId);
            return true;
        }
        
        return false;
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
        return new ArrayList<>(assignments.values());
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
        
        List<RoleAssignment> result = new ArrayList<>();
        String username = user.username();
        
        for (RoleAssignment assignment : assignments.values()) {
            if (assignment.user().username().equals(username)) {
                result.add(assignment);
            }
        }
        
        return result;
    }

    public List<RoleAssignment> findByRole(Role role) { //все назначения для роли
        if (role == null) {
            return new ArrayList<>();
        }
        
        List<RoleAssignment> result = new ArrayList<>();
        String roleId = role.getId();
        
        for (RoleAssignment assignment : assignments.values()) {
            if (assignment.role().getId().equals(roleId)) {
                result.add(assignment);
            }
        }
        
        return result;
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) {
            return findAll();
        }
        
        List<RoleAssignment> result = new ArrayList<>();
        
        for (RoleAssignment assignment : assignments.values()) {
            if (filter.test(assignment)) {
                result.add(assignment);
            }
        }
        
        return result;
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        List<RoleAssignment> result = findByFilter(filter);
        
        if (sorter != null) {
            result.sort(sorter);
        }
        
        return result;
    }

    public List<RoleAssignment> getActiveAssignments() { //только активные назначения
        List<RoleAssignment> result = new ArrayList<>();
        
        for (RoleAssignment assignment : assignments.values()) {
            if (assignment.isActive()) {
                result.add(assignment);
            }
        }
        
        return result;
    }

    public List<RoleAssignment> getExpiredAssignments() { //истекшие
        List<RoleAssignment> result = new ArrayList<>();
        
        for (RoleAssignment assignment : assignments.values()) {
            if (!assignment.isActive()) {
                result.add(assignment);
            }
        }
        
        return result;
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }
        
        String username = user.username();
        String roleId = role.getId();
        
        for (RoleAssignment assignment : assignments.values()) {
            if (assignment.user().username().equals(username) && 
                assignment.role().getId().equals(roleId) && 
                assignment.isActive()) {
                return true;
            }
        }
        
        return false;
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        if (user == null || permissionName == null || resource == null) {
            return false;
        }
        
        Set<Permission> userPermissions = getUserPermissions(user);
        
        for (Permission permission : userPermissions) {
            if (permission.name().equals(permissionName) && 
                permission.resource().equals(resource)) {
                return true;
            }
        }
        
        return false;
    }

    public Set<Permission> getUserPermissions(User user) { //получает все права из всех его активных ролей
        if (user == null) {
            return new HashSet<>();
        }
        
        Set<Permission> permissions = new HashSet<>();
        String username = user.username();
        
        for (RoleAssignment assignment : assignments.values()) {
            if (assignment.user().username().equals(username) && assignment.isActive()) {
                permissions.addAll(assignment.role().getPermissions());
            }
        }
        
        return permissions;
    }

    public void revokeAssignment(String assignmentId) { //отзывает назначение по айди
        if (assignmentId == null || assignmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("ID назначения не может быть пустым");
        }
        
        RoleAssignment assignment = assignments.get(assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException(
                "Назначение с ID '" + assignmentId + "' не найдено"
            );
        }
        
        if (!(assignment instanceof PermanentAssignment)) {
            throw new IllegalArgumentException(
                "Можно отозвать только постоянные назначения"
            );
        }
        
        PermanentAssignment perm = (PermanentAssignment) assignment;
        perm.revoke();
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) { //продлевает временное назнач
        if (assignmentId == null || assignmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("ID назначения не может быть пустым");
        }
        
        if (newExpirationDate == null || newExpirationDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Новая дата не может быть пустой");
        }
        
        RoleAssignment assignment = assignments.get(assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException(
                "Назначение с ID '" + assignmentId + "' не найдено"
            );
        }
        
        if (!(assignment instanceof TemporaryAssignment)) {
            throw new IllegalArgumentException(
                "Можно продлить только временные назначения"
            );
        }
        
        TemporaryAssignment temp = (TemporaryAssignment) assignment;
        temp.extend(newExpirationDate);
    }

    public boolean isRoleAssigned(Role role) { //назначена ли роль хотя бы одному пользователю
        if (role == null) {
            return false;
        }
        
        String roleId = role.getId();
        
        for (RoleAssignment assignment : assignments.values()) {
            if (assignment.role().getId().equals(roleId)) {
                return true;
            }
        }
        
        return false;
    }
}
