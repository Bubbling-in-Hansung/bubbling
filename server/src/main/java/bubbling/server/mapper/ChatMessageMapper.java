package bubbling.server.mapper;

import bubbling.common.dto.chat.ChatMessageDto;
import bubbling.server.domain.ChatMessage;

public class ChatMessageMapper {
    private ChatMessageMapper() {}

    public static ChatMessageDto toDto(ChatMessage message) {
        return new ChatMessageDto(
                message.getId(),
                message.getChatRoom().getId(),
                message.getChatMember().getId(),
                message.getChatMember().getRandomNickname(),
                message.getMessage(),
                message.getCreatedAt().toString()
        );
    }
}
