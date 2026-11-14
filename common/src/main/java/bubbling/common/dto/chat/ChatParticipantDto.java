package bubbling.common.dto.chat;

import bubbling.common.enums.ProfileCharacter;

public record ChatParticipantDto(
        String memberId,
        String nickname,
        ProfileCharacter profile
) {}
