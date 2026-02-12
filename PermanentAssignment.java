public class PermanentAssignment extends AbstractRoleAssignment{
    private boolean revoked;

    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata) {
        super(user, role, metadata); //вызываем конструктор родителя
        this.revoked = false;
    }

    //отозвать назначение
    public void revoke() {
        this.revoked = true;
    }

    public boolean isRevoked() {
        return revoked;
    }

    @Override 
    public boolean isActive() {
        return !revoked;    //true если не отозвано
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }

    @Override
    public String toString() {
        return String.format("PermanentAssignment{id='%s', user='%s', role='%s', revoked=%s}",
            assignmentId, user.username(), role.getName(), revoked);
    }
}
