package bubbling.server.seed;

import bubbling.common.codec.JsonCodec;
import bubbling.server.domain.User;
import bubbling.server.service.UserService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class UserSeedLoader {

    private final JsonCodec jsonCodec = new JsonCodec();

    private record SeedUser(
            String loginId,
            String passwordHash,
            String nickname
    ) {}

    public Map<String, User> loadUsers(UserService userService) {
        try (InputStream input = getClass().getResourceAsStream("/seed/users.json")) {
            if (input == null) {
                throw new IllegalStateException("users.json 파일을 찾을 수 없습니다.");
            }

            String json = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            SeedUser[] users = jsonCodec.fromPayload(
                    json,
                    SeedUser[].class
            );

            if (users == null || users.length == 0) {
                throw new IllegalStateException("테스트 계정 데이터가 비어 있습니다.");
            }

            Map<String, User> response = new HashMap<>();

            for (SeedUser data : users) {
                User user = userService.create(
                        data.loginId(),
                        data.passwordHash(),
                        data.nickname()
                );
                response.put(user.getLoginId(), user);
            }

            return response;
        } catch (IOException e) {
            throw new IllegalStateException("테스트 계정 로딩 실패", e);
        }
    }
}