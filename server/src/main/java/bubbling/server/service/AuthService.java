package bubbling.server.service;

import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.AuthExceptionCode;
import bubbling.server.security.PasswordService;

import java.util.Objects;

public class AuthService {

    private final UserService userService;
    private final PasswordService passwordService;

    public AuthService(UserService userService, PasswordService passwordService) {
        this.userService = Objects.requireNonNull(userService);
        this.passwordService = Objects.requireNonNull(passwordService);
    }

    public User register(
            String loginId,
            String password,
            String nickname
    ) {
        User.validatePassword(password);

        String passwordHash = passwordService.hash(password);

        return userService.create(
                loginId,
                passwordHash,
                nickname
        );
    }

    public User login(String loginId, String password) {
        if (loginId == null || loginId.isBlank() || password == null || password.isEmpty()) {
            throw new BaseException(AuthExceptionCode.INVALID_CREDENTIALS);
        }

        User user = userService.findByLoginId(loginId);
        if (!passwordService.matches(password, user.getPassword())) {
            throw new BaseException(AuthExceptionCode.INVALID_CREDENTIALS);
        }

        return user;
    }
}
