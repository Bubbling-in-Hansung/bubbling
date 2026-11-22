package bubbling.common.dto.chat;

public record ChatMemberEvent(
        String chatRoomId,
        ChatParticipantDto participant
) {}