package bubbling.server.domain;

import bubbling.common.enums.ChatRoomStatus;

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
            throw new IllegalStateException("이미 종료된 채팅방입니다.");
        }

        status = ChatRoomStatus.ENDED;
        endedAt = LocalDateTime.now();
    }

    private void validateActive() {
        if (status != ChatRoomStatus.ACTIVE) {
            throw new IllegalStateException("입장이 만료된 채팅방입니다.");
        }
    }

    private void validateEnded() {
        if (status == ChatRoomStatus.ENDED) {
            throw new IllegalStateException("대화가 종료된 채팅방입니다.");
        }
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
}
