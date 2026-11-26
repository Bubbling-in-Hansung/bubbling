package bubbling.server.fixture;

import bubbling.server.domain.User;
import bubbling.server.service.user.AuthService;

public final class UserFixtures {

    private UserFixtures() {}

    public static User createUser(AuthService authService, String id) {
        return authService.register(
                "testUser" + id,
                "password" + id + "!",
                "테스트유저" + id
        );
    }
}
