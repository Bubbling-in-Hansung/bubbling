package bubbling.server.store;

import bubbling.server.domain.ChatMessage;
import bubbling.server.domain.ChatRoom;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatMessageStore {

    private final ConcurrentMap<String, ChatMessage> messages = new ConcurrentHashMap<>();

    public synchronized ChatMessage save(ChatMessage chatMessage) {
        Objects.requireNonNull(chatMessage, "채팅 메세지는 필수입니다.");

        if(findById(chatMessage.getId()).isPresent()) {
            throw new IllegalStateException("채팅 메세지의 아이디가 중복됩니다.");
        }

        messages.put(chatMessage.getId(), chatMessage);

        return chatMessage;
    }

    public Optional<ChatMessage> findById(String id) {
        return messages.values().stream()
                .filter(chatMember -> chatMember.getId().equals(id))
                .findFirst();
    }

    public List<ChatMessage> findAllByChatRoom(ChatRoom chatRoom) {
        return messages.values().stream()
                .filter(chatMessage -> chatMessage.getChatRoom().equals(chatRoom))
                .toList();
    }

    public synchronized void delete(String id) {
        messages.remove(id);
    }

    public synchronized void deleteAll(List<String> ids) {
        for(String id : ids) {
            delete(id);
        }
    }
}
