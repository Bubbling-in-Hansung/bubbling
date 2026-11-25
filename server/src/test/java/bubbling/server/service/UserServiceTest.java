package bubbling.server.service;

import bubbling.common.enums.ProfileCharacter;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.fixture.UserFixtures;
import bubbling.server.service.user.PasswordService;
import bubbling.server.service.user.AuthService;
import bubbling.server.service.user.UserService;
import bubbling.server.store.UserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserStore userStore;
    private UserService userService;
    private AuthService authService;
    private PasswordService passwordService = new PasswordService();

    @BeforeEach
    void setUp() {
        userStore = new UserStore();
        userService = new UserService(userStore);
        authService = new AuthService(userService, passwordService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"새로운 닉네임", "새로운닉네임", "12newNickname", "12345"})
    @DisplayName("성공적인 프로필 수정")
    void updateSuccess(String newNickname) {
        User user = UserFixtures.createUser(authService, "1");

        user = userService.update(
                user,
                newNickname,
                ProfileCharacter.EXAMPLE
        );

        User updatedUser = userService.findById(user.getId());
        assertAll(
                () -> assertEquals(updatedUser.getNickname(), newNickname),
                () -> assertEquals(updatedUser.getProfile(), ProfileCharacter.EXAMPLE)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"닉네임!", "a", "abcdefghijklmnopqrstuvwxyzabcdefghizjlmnopqrstuvwxyz"})
    @DisplayName("닉네임 변경 실패")
    void updateFailed(String newNickname) {
        User user = UserFixtures.createUser(authService, "1");

        assertThrows(
                BaseException.class,
                () -> userService.update(
                        user,
                        newNickname,
                        ProfileCharacter.EXAMPLE
                )
        );
    }
}