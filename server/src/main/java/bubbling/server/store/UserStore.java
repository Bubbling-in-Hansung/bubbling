package bubbling.server.store;

import bubbling.server.domain.User;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserStore {

    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();

    public User save(User user) {
        Objects.requireNonNull(user, "사용자는 필수입니다.");

        User check = users.putIfAbsent(user.getLoginId(), user);

        if (check != null) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        return user;
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    public Optional<User> findByLoginId(String loginId) {
        return Optional.ofNullable(users.get(loginId));
    }
}