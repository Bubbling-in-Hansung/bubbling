package bubbling.common.dto.chat;

import bubbling.common.enums.ChatRequestRole;
import bubbling.common.enums.ChatRequestStatus;

public record ChatRequestDto(
        String id,
        String bubbleId,
        ChatRequestStatus status,
        String requesterNickname,
        ChatRequestRole role, // true: 방장에게, false: 채팅에 들어가고 싶다고 요청한 사용자에게
        String createdAt
) {}