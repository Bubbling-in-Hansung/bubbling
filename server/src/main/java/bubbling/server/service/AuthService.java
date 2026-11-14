package bubbling.server.service;

import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.AuthExceptionCode;
import bubbling.server.exception.code.GlobalExceptionCode;
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

    public void changePassword(User user, String currentPassword, String newPassword) {
        Objects.requireNonNull(user, "사용자는 필수입니다.");

        if (currentPassword == null || newPassword == null) {
            throw new BaseException(GlobalExceptionCode.INVALID_REQUEST);
        }

        if (!passwordService.matches(currentPassword, user.getPassword())) {
            throw new BaseException(AuthExceptionCode.INVALID_PASSWORD);
        }

        if(currentPassword.equals(newPassword)) {
            throw new BaseException(AuthExceptionCode.SAME_AS_CURRENT_PASSWORD);
        }

        User.validatePassword(newPassword);

        user.setPassword(passwordService.hash(newPassword));
    }
}
