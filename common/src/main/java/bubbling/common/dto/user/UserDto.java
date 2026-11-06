package bubbling.common.dto.user;

import bubbling.common.enums.ProfileCharacter;

public record UserDto(
        String id,
        String loginId,
        String nickname,
        ProfileCharacter profile,
        String createdAt
) {}
