package bubbling.common.dto.chat;

import bubbling.common.enums.ChatRoomStatus;

import java.util.List;

public record ChatRoomDto(
        String id,
        String bubbleId,
        String bubbleTitle,
        ChatRoomStatus status,
        int maxMemberCnt,
        int currentMemberCnt,
        String myMemberId,
        List<ChatParticipantDto> participants,
        String createdAt,
        String endedAt
) {}