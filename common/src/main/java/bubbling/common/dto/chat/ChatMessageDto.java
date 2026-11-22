package bubbling.common.dto.chat;

public record ChatMessageDto(
        String chatMessageId,
        String chatRoomId,
        String senderMemberId,
        String senderNickname,
        String content,
        String createdAt
) {}