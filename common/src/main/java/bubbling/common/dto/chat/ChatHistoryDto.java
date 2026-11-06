package bubbling.common.dto.chat;

import java.util.List;

public record ChatHistoryDto(
        String roomId,
        List<ChatMessageDto> messages
) {}
