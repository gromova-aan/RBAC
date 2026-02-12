public interface RoleAssignment {
    String assignmentId();
    User user();
    Role role();
    AssignmentMetadata metadata();
    boolean isActive();
    String assignmentType(); //тип назначения ("PERMANENT" или "TEMPORARY")
}
