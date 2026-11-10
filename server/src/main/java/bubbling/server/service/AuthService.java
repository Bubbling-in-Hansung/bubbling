package bubbling.server.service;

import bubbling.server.domain.User;
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
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        User user = userService.findByLoginId(loginId);
        if (!passwordService.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;
    }
}
