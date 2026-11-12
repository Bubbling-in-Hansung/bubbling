package bubbling.server.store;

import bubbling.common.enums.ChatRoomStatus;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.ChatRoom;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatRoomExceptionCode;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatRoomStore {

    private final ConcurrentMap<String, ChatRoom> chatRooms =
            new ConcurrentHashMap<>();

    public synchronized ChatRoom save(ChatRoom chatRoom) {
        Objects.requireNonNull(chatRoom, "채팅방은 필수입니다.");

        if (findByBubble(chatRoom.getBubble()).isPresent()) {
            throw new BaseException(ChatRoomExceptionCode.ALREADY_EXISTS);
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