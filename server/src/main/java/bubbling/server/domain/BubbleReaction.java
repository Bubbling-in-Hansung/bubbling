package bubbling.server.domain;

import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.BubbleExceptionCode;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class BubbleReaction {
    private final String id = UUID.randomUUID().toString();
    private final Bubble bubble;
    private final User user;
    private LocalDateTime lastReactAt;

    private BubbleReaction(Bubble bubble, User user) {
        this.bubble = bubble;
        this.user = user;
        this.lastReactAt = LocalDateTime.now();
    }

    public static BubbleReaction create(Bubble bubble, User user) {
        Objects.requireNonNull(bubble, "버블은 필수입니다.");
        Objects.requireNonNull(user, "사용자는 필수입니다.");

        if(!bubble.isAlive()) {
            throw new BaseException(BubbleExceptionCode.EXPIRED);
        }

        return new BubbleReaction(bubble, user);
    }

    public String getId() {
        return id;
    }

    public Bubble getBubble() {
        return bubble;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getLastReactAt() {
        return lastReactAt;
    }

    public synchronized void react() {
        if(!bubble.isAlive()) {
            throw new BaseException(BubbleExceptionCode.EXPIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(lastReactAt.plusMinutes(1))) {
            throw new BaseException(BubbleExceptionCode.INVALID_REACTION_PERIOD);
        }

        this.lastReactAt = now;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BubbleReaction that = (BubbleReaction) o;
        return Objects.equals(id, that.id) && Objects.equals(bubble, that.bubble) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bubble, user);
    }
}
