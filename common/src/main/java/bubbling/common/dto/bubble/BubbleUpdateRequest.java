package bubbling.common.dto.bubble;

import bubbling.common.enums.BubbleCategory;

public record BubbleUpdateRequest(
        String bubbleId,
        String content,
        BubbleCategory category
) {}