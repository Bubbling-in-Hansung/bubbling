package bubbling.common.dto.chat;

public record ChatPositionDto(
        String chatRoomId,
        String chatMemberId,
        double x,
        double y
) {}
