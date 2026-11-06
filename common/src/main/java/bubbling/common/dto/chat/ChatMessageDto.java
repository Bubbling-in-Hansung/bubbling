package bubbling.common.dto.chat;

public record ChatMessageDto(
        String id,
        String roomId,
        String content,
        boolean mine,
        long createdAtEpochMillis
) {}