package bubbling.common.dto.bubble;

import bubbling.common.enums.BubbleStatus;

public record BubbleRemovedEvent(
        String bubbleId,
        BubbleStatus status
) {}