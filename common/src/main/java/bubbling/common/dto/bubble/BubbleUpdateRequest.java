package bubbling.common.dto.bubble;

import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleMethod;

public record BubbleUpdateRequest(
        String bubbleId,
        String title,
        BubbleCategory category,
        BubbleMethod method,
        int maxMemberCnt,
        boolean isOpen
) {}