import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment {
    protected final String assignmentId;
    protected final User user;
    protected final Role role;
    protected final AssignmentMetadata metadata;

    //конструктор
    public AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {
        if (user == null) {
            throw new IllegalArgumentException("User не может быть null");
        } 
        if (role == null) {
            throw new IllegalArgumentException("Role не может быть null");
        } 
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata не может быть null");
        } 

        this.assignmentId = generateAssignmentId();
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }

    private String generateAssignmentId() {
        return "assign_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public String assignmentId() {
        return assignmentId;
    }
    
    @Override
    public User user() {
        return user;
    }
    
    @Override
    public Role role() {
        return role;
    }
    
    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }

    @Override
    public abstract boolean isActive();
    
    @Override
    public abstract String assignmentType();

    // equals и hashCode по assignmentId
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRoleAssignment that = (AbstractRoleAssignment) o;
        return Objects.equals(assignmentId, that.assignmentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }

    @Override
    public String toString() {
        return String.format("%s{id='%s', user='%s', role='%s'}",
            getClass().getSimpleName(), assignmentId, user.username(), role.getName());
    }

    public String summary() {
        String status = isActive() ? "ACTIVE" : "INACTIVE";
        String reason = (metadata.reason() != null && !metadata.reason().trim().isEmpty()) 
            ? metadata.reason() 
            : "Not specified";
        
        return String.format(
            "[%s] %s assigned to %s by %s at %s%n Reason: %s%n Status: %s",
            assignmentType(),
            role.getName(),
            user.username(),
            metadata.assignedBy(),
            metadata.assignedAt(),
            reason,
            status
        );
    }
}
