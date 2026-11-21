package bubbling.server.fixture;

import bubbling.server.domain.ChatMember;
import bubbling.server.domain.ChatMessage;
import bubbling.server.domain.ChatRoom;

public final class ChatFixtures {

    private ChatFixtures() {}

    public static ChatMessage createChatMessage(ChatRoom chatRoom, ChatMember chatMember, String message) {
        return ChatMessage.create(
                chatRoom,
                chatMember,
                message
        );
    }

    public static ChatMessage createChatMessage(ChatRoom chatRoom, ChatMember chatMember) {
        return createChatMessage(chatRoom, chatMember, "Hello, world!");
    }
}
