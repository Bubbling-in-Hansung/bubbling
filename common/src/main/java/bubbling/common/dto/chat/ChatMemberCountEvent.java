package bubbling.common.dto.chat;

public record ChatMemberCountEvent(
        String bubbleId,
        int currentMemberCnt
) {}