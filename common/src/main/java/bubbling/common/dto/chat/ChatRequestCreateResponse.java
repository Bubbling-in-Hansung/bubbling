package bubbling.common.dto.chat;

public record ChatRequestCreateResponse(
        String chatRequestId,
        String bubbleId
) {}