package bubbling.common.dto.chat;

public record ChatMessageRequest(
        String chatRoomId,
        String content
) {}