import java.util.*;

public class Role {
    private final String id;
    private final String name;
    private final String description;
    private final Set<Permission> permissions;

    private static int nextId = 1;       //генератор id

    public Role(String name, String description, Set<Permission> permissions) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название роли не может быть пустым");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Описание роли не может быть пустым");
        }

        this.id = generateId();
        this.name = name.trim();
        this.description = description.trim();

        //новый HashSet, чтобы никто не мог изменить наш набор извне
        this.permissions = permissions != null 
            ? new HashSet<>(permissions)  // если передали набор - копируем
            : new HashSet<>();            // если null - создаем пустой
    }

    public Role(String name, String description) {
        this(name, description, new HashSet<>());
    }

    private static String generateId() {
        return "role_" + nextId++;
    }

    //геттеры
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions); //никто не сможет изменить
    }

    public void addPermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Право не может быть null");
        }
        permissions.add(permission); //добавляем в хэшсет
    }

    public void removePermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Право не может быть null");
        }
        permissions.remove(permission);
    }

    //наличие права по объекту
    public boolean hasPermission(Permission permission) {
        if (permission == null) {
            return false;
        }
        return permissions.contains(permission);
    }

    //наличие права по имени и ресурсу
    public boolean hasPermission(String permissionName, String resource) {
        if (permissionName == null || resource == null) {
            return false;
        }
        
        //перебираем все права и ищем совпадение
        for (Permission p : permissions) {
            if (p.name().equals(permissionName.toUpperCase()) && 
                p.resource().equals(resource.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override  //переопределение, сравнение объектов
    public boolean equals(Object o) {
        if (this == o) return true;                    //тот же объект
        if (o == null || getClass() != o.getClass()) return false; //не Role или null
        Role role = (Role) o;                          //приводим к Role
        return Objects.equals(id, role.id);           //сравниваем только по id
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);                       // hashCode только от id
    }

    @Override
    public String toString() {
        return String.format("Role{id='%s', name='%s', description='%s', permissions=%d}",
            id, name, description, permissions.size());
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
       
        sb.append(String.format("Role: %s [ID: %s]%n", name, id));
        sb.append(String.format("Description: %s%n", description));
        sb.append(String.format("Permissions (%d):%n", permissions.size()));
        
        for (Permission p : permissions) {
            sb.append(String.format("  - %s%n", p.format()));
        }
        return sb.toString();
    }
}