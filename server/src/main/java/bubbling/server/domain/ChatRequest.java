package bubbling.server.domain;

import bubbling.common.enums.ChatRequestStatus;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.BubbleExceptionCode;
import bubbling.server.exception.code.ChatRequestExceptionCode;

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

        if (!bubble.isAlive()) {
            throw new BaseException(BubbleExceptionCode.EXPIRED);
        }

        if (bubble.getIsOpen()) {
            throw new BaseException(ChatRequestExceptionCode.OPEN_CHATROOM);
        }

        if (bubble.getOwner().equals(requester)) {
            throw new BaseException(ChatRequestExceptionCode.NOT_ALLOWED);
        }

        return new ChatRequest(bubble, requester);
    }

    public synchronized void accept(User user) {
        validateAcceptable(user);

        this.status = ChatRequestStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public synchronized void reject(User user) {
        validateAcceptable(user);

        this.status = ChatRequestStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    private void validatePending() {
        if (status != ChatRequestStatus.PENDING) {
            throw new BaseException(ChatRequestExceptionCode.ALREADY_PROCESSED);
        }
    }

    private void validateOwner(User user) {
        if (!bubble.getOwner().getId().equals(user.getId())) {
            throw new BaseException(ChatRequestExceptionCode.FORBIDDEN);
        }
    }

    public void validateAcceptable(User user) {
        validatePending();
        validateOwner(user);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRequest that = (ChatRequest) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
