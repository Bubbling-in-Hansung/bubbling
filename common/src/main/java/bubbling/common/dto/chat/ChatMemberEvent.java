package bubbling.common.dto.chat;

public record ChatMemberEvent(
        String roomId,
        ChatParticipantDto participant
) {}