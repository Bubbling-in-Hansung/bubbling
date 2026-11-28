package bubbling.client.ui;

import bubbling.common.dto.bubble.BubbleDto;
import bubbling.common.dto.chat.ChatParticipantDto;
import bubbling.common.dto.chat.ChatRoomDto;
import bubbling.common.enums.*;

import java.util.List;

final class ServerRoomFixtures {
    private ServerRoomFixtures() {}

    static BubbleDto bubble(String id, String title, BubbleCategory category, String expiresAt) {
        return new BubbleDto(id, title, category, BubbleMethod.TOGETHER, 4, 1, false,
                BubbleStatus.ALIVE, 0, "2026-11-17T00:00:00", expiresAt);
    }

    static ChatRoomDto chat(BubbleDto bubble) {
        return new ChatRoomDto("chat-" + bubble.bubbleId(), bubble, ChatRoomStatus.ACTIVE, "me", true,
                List.of(new ChatParticipantDto("me", "테스터", ProfileCharacter.BUNNY)), bubble.createdAt(), null, null);
    }
}
