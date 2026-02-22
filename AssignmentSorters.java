
import java.util.Comparator;

public class AssignmentSorters {
    public static Comparator<RoleAssignment> byUsername() {
        return (a1, a2) -> {
            String username1 = a1.user().username();
            String username2 = a2.user().username();
            return username1.compareTo(username2);
        };
    }

    public static Comparator<RoleAssignment> byRoleName() {
        return (a1, a2) -> {
            String roleName1 = a1.role().getName();
            String roleName2 = a2.role().getName();
            return roleName1.compareTo(roleName2);
        };
    }

    public static Comparator<RoleAssignment> byAssignmentDate() { //по дате назнач
        return (a1, a2) -> {
            String date1 = a1.metadata().assignedAt();
            String date2 = a2.metadata().assignedAt();
            return date1.compareTo(date2);
        };
    }
}
