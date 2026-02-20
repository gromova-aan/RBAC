//функциональный интерфейс для фильтрации пользователей с одним абстр методом
@FunctionalInterface
public interface UserFilter {
    boolean test(User user);

    default UserFilter and(UserFilter other) {
        if (other == null) {
            return this;
        }

        return user->this.test(user) && other.test(user);
    }

    default UserFilter or(UserFilter other) {
        if (other == null) {
            return this;
        }

        return user->this.test(user) || other.test(user);
    }
}
