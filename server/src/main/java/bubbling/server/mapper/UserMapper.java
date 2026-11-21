package bubbling.server.mapper;

import bubbling.common.dto.user.UserDto;
import bubbling.server.domain.User;

public class UserMapper {
    private UserMapper() {}

    public static UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getLoginId(),
                user.getNickname(),
                user.getProfile(),
                user.getCreatedAt().toString()
        );
    }
}
