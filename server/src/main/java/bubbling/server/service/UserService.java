package bubbling.server.service;

import bubbling.common.enums.ProfileCharacter;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.AuthExceptionCode;
import bubbling.server.exception.code.UserExceptionCode;
import bubbling.server.store.UserStore;

import java.util.Objects;

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
        return store.findById(id).orElseThrow(
                () -> new BaseException(UserExceptionCode.NOT_FOUND)
        );
    }

    public User findByLoginId(String loginId) {
        return store.findByLoginId(loginId).orElseThrow(
                () -> new BaseException(AuthExceptionCode.INVALID_CREDENTIALS)
        );
    }

    public void update(User user, String nickname, ProfileCharacter profile) {
        Objects.requireNonNull(user, "사용자는 필수입니다.");

        user.setNickname(nickname);
        user.setProfile(profile);
    }
}
