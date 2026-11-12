package bubbling.server.store;

import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.UserExceptionCode;

import java.util.Optional;

public class UserStore extends BaseStore<User> {

    public UserStore() {
        super(User::getId);
    }

    @Override
    protected void validateSave(User user) {
        if (findByLoginId(user.getLoginId()).isPresent()) {
            throw new BaseException(UserExceptionCode.DUPLICATE_LOGIN_ID);
        }
    }

    public Optional<User> findByLoginId(String loginId) {
        return findAll().stream()
                .filter(user -> user.getLoginId().equals(loginId))
                .findFirst();
    }
}