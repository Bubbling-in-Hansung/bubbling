package bubbling.common.dto.chat;

public record ChatMessageRequest(
        String roomId,
        String content
) {}