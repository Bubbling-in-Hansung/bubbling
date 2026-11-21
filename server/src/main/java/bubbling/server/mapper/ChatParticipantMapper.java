package bubbling.server.mapper;

import bubbling.common.dto.chat.ChatParticipantDto;
import bubbling.server.domain.ChatMember;

public class ChatParticipantMapper {
    private ChatParticipantMapper() {}

    public static ChatParticipantDto toDto(
            ChatMember chatMember
    ) {
        return new ChatParticipantDto(
                chatMember.getId(),
                chatMember.getRandomNickname(),
                chatMember.getUser().getProfile()
        );
    }
}
