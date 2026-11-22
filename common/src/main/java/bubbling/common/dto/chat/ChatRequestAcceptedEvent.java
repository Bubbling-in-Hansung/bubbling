package bubbling.common.dto.chat;

public record ChatRequestAcceptedEvent(
        String chatRequestId,
        String chatRoomId,
        String chatMemberId,
        String randomNickname
) {}