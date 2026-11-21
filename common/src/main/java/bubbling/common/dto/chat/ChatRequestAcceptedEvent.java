package bubbling.common.dto.chat;

public record ChatRequestAcceptedEvent(
        String chatRequestId,
        String roomId,
        String memberId,
        String randomNickname
) {}