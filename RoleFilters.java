import java.util.Set;

public class RoleFilters {
    public static RoleFilter byName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return role -> true;
        }

        final String searchName = name.trim();
        return role -> searchName.equals(role.getName());
    }

    public static RoleFilter byNameContains(String substring) { //часть имени роли
        if (substring == null || substring.trim().isEmpty()) {
            return role -> true;
        }
        
        final String searchSubstring = substring.trim().toLowerCase();
        return role -> role.getName().toLowerCase().contains(searchSubstring);
    }

    public static RoleFilter hasPermission(Permission permission) { //есть ли конкретное право
        if (permission == null) {
            return role -> true;
        }
        
        return role -> role.hasPermission(permission);
    }

    public static RoleFilter hasPermission(String permissionName, String resource) { //право с опр именем и ресурсом
        if (permissionName == null || resource == null) {
            return role -> true;
        }
        
        final String name = permissionName.trim();
        final String res = resource.trim();
        
        return role -> role.hasPermission(name, res);
    }

    public static RoleFilter hasAtLeastNPermissions(int n) { //роль имеет минимум n прав
        if (n < 0) {
            return role -> true; //если n отрицательное, пропускаем все роли
        }
        
        final int minPermissions = n;
        
        return role -> {
            Set<Permission> permissions = role.getPermissions();
            return permissions.size() >= minPermissions;
        };
    }
}
