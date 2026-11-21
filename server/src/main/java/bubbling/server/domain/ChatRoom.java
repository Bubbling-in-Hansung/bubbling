package bubbling.server.domain;

import bubbling.common.enums.ChatRoomStatus;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatRoomExceptionCode;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ChatRoom {
    private final String id = UUID.randomUUID().toString();
    private final Bubble bubble;
    private ChatRoomStatus status = ChatRoomStatus.ACTIVE;
    private final LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime endedAt;

    private ChatRoom(Bubble bubble) {
        this.bubble = bubble;
    }

    public static ChatRoom create(Bubble bubble) {
        Objects.requireNonNull(bubble, "버블은 필수입니다.");
        return new ChatRoom(bubble);
    }

    public synchronized void end() {
        if(status == ChatRoomStatus.ENDED) {
            throw new BaseException(ChatRoomExceptionCode.ENDED);
        }

        status = ChatRoomStatus.ENDED;
        endedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == ChatRoomStatus.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public Bubble getBubble() {
        return bubble;
    }

    public ChatRoomStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoom chatRoom = (ChatRoom) o;
        return Objects.equals(id, chatRoom.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
