package bubbling.common.dto.user;

import bubbling.common.enums.ProfileCharacter;

public record UserUpdateRequest(
        String nickname,
        ProfileCharacter profile
) {}
