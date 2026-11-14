package bubbling.server.mapper;

import bubbling.common.dto.bubble.BubbleDto;
import bubbling.server.domain.Bubble;

public class BubbleMapper {
    private BubbleMapper() {}

    public static BubbleDto toDto(
            Bubble bubble,
            int currentMemberCnt
    ) {
        return new BubbleDto(
                bubble.getId(),
                bubble.getTitle(),
                bubble.getCategory(),
                bubble.getMethod(),
                bubble.getMaxMemberCnt(),
                currentMemberCnt,
                bubble.getIsOpen(),
                bubble.getStatus(),
                bubble.getReactionCnt(),
                bubble.getCreatedAt().toString(),
                bubble.getExpiresAt().toString()
        );
    }
}
