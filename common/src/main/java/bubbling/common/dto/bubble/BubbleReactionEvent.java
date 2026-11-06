package bubbling.common.dto.bubble;

public record BubbleReactionEvent(
        String bubbleId,
        int reactionCount
) {}