package bubbling.common.dto.bubble;

import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleMethod;

public record BubbleUpdateRequest(
        String bubbleId,
        String title,
        BubbleCategory category,
        BubbleMethod method,
        int maxMemberCnt,
        boolean isOpen,
        int expireMinute // 요청 시각으로부터 추가 됨
) {}