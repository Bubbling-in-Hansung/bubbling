package bubbling.common.dto.chat;

public record ChatTypingDto(
        String chatRoomId,
        String chatMemberId,
        boolean typing
) {}