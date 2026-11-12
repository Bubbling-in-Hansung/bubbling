package bubbling.common.dto.chat;

public record ChatJoinResponse(
        String chatRoomId,
        String bubbleId,
        String chatMemberId,
        String chatMemberNickname,
        int totalCount
) {}
