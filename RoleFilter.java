@FunctionalInterface
public interface RoleFilter {
    boolean test(Role role);

    default RoleFilter and(RoleFilter other) {
        if (other == null) {
            return this;
        }
        return role -> this.test(role) && other.test(role);
    }

    default RoleFilter or(RoleFilter other) {
        if (other == null) {
            return this;
        }
        return role -> this.test(role) || other.test(role);
    }
}
