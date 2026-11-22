package bubbling.common.dto.chat;

public record ChatLeftResponse(
        String roomId,
        String bubbleId,
        int currentMemberCnt
) {}
