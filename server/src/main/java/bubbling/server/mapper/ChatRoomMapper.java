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
            boolean isOwner,
            String myMemberId,
            List<ChatParticipantDto> participants
    ) {
        return new ChatRoomDto(
                room.getId(),
                BubbleMapper.toDto(room.getBubble(), currentMemberCnt),
                room.getStatus(),
                myMemberId,
                isOwner,
                participants,
                room.getCreatedAt().toString(),
                room.getEndedAt() == null
                        ? null
                        : room.getEndedAt().toString(),
                room.getMusicUrl()
        );
    }
}
