
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AssignmentFilters {
    private static  final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static AssignmentFilter byUser(User user) { //назначения для конкретного пользователя
        if (user == null) {
            return assignment -> true;
        }

        return assignment -> {
            User assignedUser = assignment.user();
            return user.username().equals(assignedUser.username());
        };
    }

    public static AssignmentFilter byUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return assignment -> true;
        }

        final String searchUsername = username.trim();
        return assignment -> searchUsername.equals(assignment.user().username());
    }

    public static AssignmentFilter byRole(Role role) { //назначения конкретной роли
        if (role == null) {
            return assignment -> true;
        }
        
        return assignment -> {
            Role assignedRole = assignment.role();
            //сравниваем по ID роли
            return role.getId().equals(assignedRole.getId());
        };
    }

    public static AssignmentFilter byRoleName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return assignment -> true;
        }
        
        final String searchRoleName = roleName.trim();
        return assignment -> searchRoleName.equals(assignment.role().getName());
    }

    public static AssignmentFilter activeOnly() { //только активные назначения
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() { //только неактивные назанчения
        return assignment -> !assignment.isActive();
    }

    public static AssignmentFilter byType(String type) { //"PERMANENT" или "TEMPORARY"
        if (type == null || type.trim().isEmpty()) {
            return assignment -> true;
        }
        
        final String searchType = type.trim().toUpperCase();
        return assignment -> searchType.equals(assignment.assignmentType());
    }

    public static AssignmentFilter assignedBy(String username) { //кто назначил
        if (username == null || username.trim().isEmpty()) {
            return assignment -> true;
        }
        
        final String searchUsername = username.trim();
        return assignment -> {
            AssignmentMetadata meta = assignment.metadata();
            return searchUsername.equals(meta.assignedBy());
        };
    }

    public static AssignmentFilter assignedAfter(String date) { //назначенные после опр даты
        if (date == null || date.trim().isEmpty()) {
            return assignment -> true;
        }
        
        try {
            LocalDate filterDate = LocalDate.parse(date.trim(), DATE_FORMATTER);
            
            return assignment -> {
                AssignmentMetadata meta = assignment.metadata();
                try {
                    LocalDate assignedDate = LocalDate.parse(
                        meta.assignedAt().substring(0, 10), DATE_FORMATTER
                    );
                    return assignedDate.isAfter(filterDate);
                } catch (Exception e) {
                    return false;
                }
            };
        } catch (DateTimeParseException e) {
            return assignment -> false;
        }
    }

    public static AssignmentFilter expiringBefore(String date) { //временные назначения, истекающие до даты
        if (date == null || date.trim().isEmpty()) {
            return assignment -> true;
        }
        
        try {
            LocalDate filterDate = LocalDate.parse(date.trim(), DATE_FORMATTER);
            
            return assignment -> {
                //проверяем и сразу приводим к типу TemporaryAssignment
                if (assignment instanceof TemporaryAssignment temp) {
                    try {
                        LocalDate expiresDate = LocalDate.parse(temp.getExpiresAt(), DATE_FORMATTER);
                        return expiresDate.isBefore(filterDate);
                    } catch (Exception e) {
                        return false;
                    }
                }
                
                return false; //не временное назначение
            };
        } catch (DateTimeParseException e) {
            return assignment -> false;
        }
    }
}
