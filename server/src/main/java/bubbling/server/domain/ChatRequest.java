package bubbling.server.domain;

import bubbling.common.enums.ChatRequestStatus;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ChatRequest {
    private final String id = UUID.randomUUID().toString();
    private final Bubble bubble;
    private final User requester;
    private ChatRequestStatus status = ChatRequestStatus.PENDING;
    private final LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime respondedAt;

    private ChatRequest(Bubble bubble, User requester) {
        this.bubble = bubble;
        this.requester = requester;
    }

    public static ChatRequest create(Bubble bubble, User requester) {
        Objects.requireNonNull(bubble, "버블은 필수입니다.");
        Objects.requireNonNull(requester, "요청자는 필수입니다.");

        if (bubble.getOwner().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("자신의 버블에는 참여 요청을 보낼 수 없습니다.");
        }

        return new ChatRequest(bubble, requester);
    }

    public synchronized void accept() {
        validatePending();

        this.status = ChatRequestStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public synchronized void reject() {
        validatePending();

        this.status = ChatRequestStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    private void validatePending() {
        if (status != ChatRequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 참여 요청입니다.");
        }
    }

    public String getId() {
        return id;
    }

    public Bubble getBubble() {
        return bubble;
    }

    public User getRequester() {
        return requester;
    }

    public ChatRequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }
}
