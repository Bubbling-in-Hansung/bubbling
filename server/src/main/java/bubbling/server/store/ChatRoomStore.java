package bubbling.server.store;

import bubbling.common.enums.ChatRoomStatus;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.ChatRoom;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatRoomStore {

    private final ConcurrentMap<String, ChatRoom> chatRooms =
            new ConcurrentHashMap<>();

    public synchronized ChatRoom save(ChatRoom chatRoom) {
        Objects.requireNonNull(chatRoom, "채팅방은 필수입니다.");

        if (findById(chatRoom.getId()).isPresent()) {
            throw new IllegalStateException("이미 존재하는 채팅방 아이디입니다.");
        }

        if (findByBubble(chatRoom.getBubble()).isPresent()) {
            throw new IllegalStateException("고민 하나당 하나의 채팅방만 생성할 수 있습니다.");
        }

        chatRooms.put(chatRoom.getId(), chatRoom);

        return chatRoom;
    }

    public Optional<ChatRoom> findById(String id) {
        return Optional.ofNullable(chatRooms.get(id));
    }

    public Optional<ChatRoom> findByBubble(Bubble bubble) {
        return chatRooms.values().stream()
                .filter(cr -> cr.getBubble().getId().equals(bubble.getId()))
                .findFirst();
    }

    public Optional<ChatRoom> findByBubbleAndStatus(Bubble bubble, ChatRoomStatus status) {
        return chatRooms.values().stream()
                .filter(cr -> cr.getBubble().getId().equals(bubble.getId()) && cr.getStatus() == status)
                .findFirst();
    }

    public synchronized void delete(String id) {
        chatRooms.remove(id);
    }
}