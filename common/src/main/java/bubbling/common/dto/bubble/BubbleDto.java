package bubbling.common.dto.bubble;

import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleMethod;
import bubbling.common.enums.BubbleStatus;

public record BubbleDto(
        String id,
        String title,
        BubbleCategory category,
        BubbleMethod method,
        int maxMemberCnt,
        int currentMemberCnt,
        boolean isOpen,
        BubbleStatus status,
        int reactionCount,
        String createdAt,
        String expiresAt
) {}