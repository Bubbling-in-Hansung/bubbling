package bubbling.server.service;

import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.security.PasswordService;
import bubbling.server.store.UserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        UserStore userStore = new UserStore();

        UserService userService = new UserService(userStore);
        PasswordService passwordService = new PasswordService();

        authService = new AuthService(
                userService,
                passwordService
        );
    }

    @Test
    @DisplayName("정상적인 회원가입")
    void registerSuccess() {
        // Given
        String loginId = "testuser01";
        String password = "password123";
        String nickname = "테스터";

        // When
        User user = authService.register(
                loginId,
                password,
                nickname
        );

        // Then
        assertNotNull(user);
        assertEquals(loginId, user.getLoginId());
        assertEquals(nickname, user.getNickname());
    }

    @Test
    @DisplayName("중복된 아이디로 회원가입 시도")
    void registerDuplicateLoginId() {
        // Given
        authService.register(
                "testuser01",
                "password123",
                "테스터1"
        );

        // When & Then
        assertThrows(
                BaseException.class,
                () -> authService.register(
                        "testuser01",
                        "password456",
                        "테스터2"
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "test123", "abcdefghijabcdefghijabcdefghijabcdefghij"})
    @DisplayName("아이디가 8자 미만 또는 32자 초과면 회원가입 불가")
    void registerInvalidLoginId(String loginId) {
        assertThrows(
                BaseException.class,
                () -> authService.register(
                        loginId,
                        "password123",
                        "테스터"
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "test123", "1234567890123456789012345678901234567890123456789012345678901234567890"})
    @DisplayName("패스워드가 8자 미만 또는 64자 초과면 회원가입 불가")
    void registerInvalidPasswordLength(String password) {
        assertThrows(
                BaseException.class,
                () -> authService.register(
                        "testuser01",
                        password,
                        "테스터"
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"테스트용패스워드1", "testpassword★"})
    @DisplayName("패스워드에 허용하지 않는 특수문자가 포함되면 회원가입 불가")
    void registerInvalidPasswordUTF(String password) {
        assertThrows(
                BaseException.class,
                () -> authService.register(
                        "testuser01",
                        password,
                        "테스터"
                )
        );
    }

    @Test
    @DisplayName("정상적인 로그인")
    void loginSuccess() {
        // Given
        User registeredUser = authService.register(
                "testuser01",
                "password123",
                "테스터"
        );

        // When
        User loggedInUser = authService.login(
                "testuser01",
                "password123"
        );

        // Then
        assertSame(registeredUser, loggedInUser);
    }

    @Test
    @DisplayName("비밀번호가 틀린 로그인")
    void loginWrongPassword() {
        // Given
        authService.register(
                "testuser01",
                "password123",
                "테스터"
        );

        // When & Then
        assertThrows(
                BaseException.class,
                () -> authService.login(
                        "testuser01",
                        "wrongpassword"
                )
        );
    }

    @Test
    @DisplayName("존재하지 않는 아이디로 로그인")
    void loginUserNotFound() {
        assertThrows(
                BaseException.class,
                () -> authService.login(
                        "unknownuser",
                        "password123"
                )
        );
    }
}