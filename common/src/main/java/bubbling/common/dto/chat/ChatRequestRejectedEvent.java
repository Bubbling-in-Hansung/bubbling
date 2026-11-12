package bubbling.common.dto.chat;

public record ChatRequestRejectedEvent(
        String chatRequestId,
        String bubbleId
) {}
