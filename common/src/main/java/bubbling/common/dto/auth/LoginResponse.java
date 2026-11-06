package bubbling.common.dto.auth;

import bubbling.common.dto.user.UserDto;

public record LoginResponse(
        UserDto user
) {}