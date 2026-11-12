package bubbling.common.dto.chat;

public record ChatRequestCreateEvent(
        String chatRequestId,
        String bubbleId,
        String anonymous,
        String createdAt
) {}