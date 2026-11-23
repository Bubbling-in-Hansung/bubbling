package bubbling.common.dto.chat;

import bubbling.common.enums.ChatRequestRole;
import bubbling.common.enums.ChatRequestStatus;

public record ChatRequestDto(
        String chatRequestId,
        String bubbleId,
        ChatRequestStatus status,
        String requesterNickname,
        ChatRequestRole role,
        String createdAt
) {}