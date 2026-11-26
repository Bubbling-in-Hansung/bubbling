package bubbling.common.dto.chat;

import bubbling.common.dto.bubble.BubbleDto;
import bubbling.common.enums.ChatRoomStatus;

import java.util.List;

public record ChatRoomDto(
        String chatRoomId,
        BubbleDto bubble,
        ChatRoomStatus status,
        String myMemberId,
        boolean isOwner,
        List<ChatParticipantDto> participants,
        String createdAt,
        String endedAt,
        String musicUrl
) {}