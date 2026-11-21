package bubbling.common.dto.system;

import bubbling.common.dto.bubble.BubbleDto;

import java.util.List;

public record InitialStateDto(
        List<BubbleDto> bubbles,
        String myBubbleId,
        int onlineUserCnt
) {}