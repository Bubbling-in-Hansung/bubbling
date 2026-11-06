package bubbling.common.dto.chat;

import bubbling.common.enums.ChatRequestStatus;

public record ChatRequestDto(
        String id,
        String bubbleId,
        ChatRequestStatus status,
        boolean incoming, // true: 방장이 받은 요청, false: 본인이 보낸 요청
        String createdAt
) {}