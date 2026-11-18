package bubbling.common.dto.user;

import bubbling.common.enums.ProfileCharacter;

public record UserDto(
        String id, // UUID
        String loginId,
        String nickname,
        ProfileCharacter profile,
        String createdAt
) {}
