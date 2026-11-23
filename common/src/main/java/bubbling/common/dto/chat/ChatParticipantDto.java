package bubbling.common.dto.chat;

import bubbling.common.enums.ProfileCharacter;

public record ChatParticipantDto(
        String chatMemberId,
        String nickname,
        ProfileCharacter profile
) {}
