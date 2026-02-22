@FunctionalInterface
public interface AssignmentFilter {
    boolean test(RoleAssignment assignment); //проходит ли назначение через фильтр

    default AssignmentFilter and(AssignmentFilter other) {
        if (other == null) {
            return this;
        }

        return assignment -> this.test(assignment) && other.test(assignment);
    }

    default AssignmentFilter or(AssignmentFilter other) {
        if (other == null) {
            return this;
        }
        return assignment -> this.test(assignment) || other.test(assignment);
    }
}
