package bubbling.server.service;

import bubbling.common.enums.ProfileCharacter;
import bubbling.server.domain.User;
import bubbling.server.store.UserStore;

public class UserService {

    private final UserStore store;

    public UserService(UserStore userStore) {
        this.store = userStore;
    }

    public User create(
            String loginId,
            String passwordHash,
            String nickname
    ) {
        User user = User.create(
                loginId,
                passwordHash,
                nickname,
                ProfileCharacter.EXAMPLE
        );

        return store.save(user);
    }

    public User findById(String id) {
        return store.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    public User findByLoginId(String loginId) {
        return store.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));
    }
}
