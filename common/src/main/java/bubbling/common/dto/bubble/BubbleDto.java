package bubbling.common.dto.bubble;

import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleStatus;

import java.time.LocalDateTime;

public record BubbleDto(
        String id,
        String title,
        BubbleCategory category,
        BubbleStatus status,
        int reactionCount,
        LocalDateTime expiredAt
) {}