package bubbling.common.dto.bubble;

import bubbling.common.enums.BubbleCategory;

public record BubbleCreateRequest(
        String title,
        BubbleCategory category
) {}