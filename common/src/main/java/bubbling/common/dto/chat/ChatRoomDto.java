package bubbling.common.dto.chat;

import bubbling.common.enums.ChatRoomStatus;

public record ChatRoomDto(
        String id,
        String bubbleId,
        ChatRoomStatus status,
        long createdAtEpochMillis
) {}