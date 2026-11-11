package bubbling.server.service;

import bubbling.server.domain.ChatMember;
import bubbling.server.domain.ChatMessage;
import bubbling.server.domain.ChatRoom;
import bubbling.server.store.ChatMessageStore;

import java.util.List;
import java.util.Objects;

public class ChatMessageService {

    private final ChatMessageStore store;

    public ChatMessageService(ChatMessageStore store) {
        this.store = store;
    }

    public ChatMessage create(
            ChatRoom chatRoom,
            ChatMember chatMember,
            String message
    ) {
        Objects.requireNonNull(chatRoom, "채팅방은 필수입니다.");
        Objects.requireNonNull(chatMember, "채팅 멤버는 필수입니다.");

        if(!chatMember.getChatRoom().equals(chatRoom)) {
            throw new IllegalArgumentException("채팅 멤버가 채팅방에 소속되어 있지 않습니다.");
        }

        ChatMessage.validateMessage(message);
        ChatMessage chatMessage = ChatMessage.create(chatRoom, chatMember, message);

        return store.save(chatMessage);
    }

    public void deleteAllByChatRoom(ChatRoom chatRoom) {
        List<ChatMessage> chatMessages = store.findAllByChatRoom(chatRoom);
        store.deleteAll(chatMessages.stream().map(ChatMessage::getId).toList());
    }
}
