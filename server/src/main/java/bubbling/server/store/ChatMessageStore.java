package bubbling.server.store;

import bubbling.server.domain.ChatMessage;
import bubbling.server.domain.ChatRoom;

import java.util.List;

public class ChatMessageStore extends BaseStore<ChatMessage> {

    public ChatMessageStore() {
        super(ChatMessage::getId);
    }

    public List<ChatMessage> findAllByChatRoom(ChatRoom chatRoom) {
        return findAll().stream()
                .filter(chatMessage -> chatMessage.getChatRoom().equals(chatRoom))
                .toList();
    }
}
