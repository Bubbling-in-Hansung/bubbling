package bubbling.server.mapper;

import bubbling.common.dto.chat.ChatParticipantDto;
import bubbling.common.dto.chat.ChatRoomDto;
import bubbling.server.domain.ChatRoom;

import java.util.List;

public class ChatRoomMapper {
    private ChatRoomMapper() {}

    public static ChatRoomDto toDto(
            ChatRoom room,
            int currentMemberCnt,
            String myMemberId,
            List<ChatParticipantDto> participants
    ) {
        return new ChatRoomDto(
                room.getId(),
                room.getBubble().getId(),
                room.getBubble().getTitle(),
                room.getStatus(),
                room.getBubble().getMaxMemberCnt(),
                currentMemberCnt,
                myMemberId,
                participants,
                room.getCreatedAt().toString(),
                room.getEndedAt() == null
                        ? null
                        : room.getEndedAt().toString()
        );
    }
}
