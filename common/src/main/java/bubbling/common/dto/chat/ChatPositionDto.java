package bubbling.common.dto.chat;

public record ChatPositionDto(
        String chatRoomId,
        String chatMemberId,
        int x,
        int y
) {}
