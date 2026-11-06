package bubbling.common.dto.chat;

import bubbling.common.enums.ChatRequestStatus;

public record ChatRequestDto(
        String id,
        String bubbleId,
        ChatRequestStatus status,
        boolean incoming,
        long createdAtEpochMillis
) {}