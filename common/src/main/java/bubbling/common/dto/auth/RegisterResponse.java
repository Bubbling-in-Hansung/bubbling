package bubbling.common.dto.auth;

import bubbling.common.dto.user.UserDto;

public record RegisterResponse(
        UserDto user
) {}