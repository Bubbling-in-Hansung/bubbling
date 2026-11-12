package bubbling.common.dto.chat;

public record ChatRequestDecisionResponse(
        String chatRequestId,
        String bubbleId
) {}