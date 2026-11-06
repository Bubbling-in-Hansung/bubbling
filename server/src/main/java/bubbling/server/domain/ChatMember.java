package bubbling.server.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ChatMember {
    private final String id = UUID.randomUUID().toString();
    private final User user;
    private final String randomNickname;
    private final LocalDateTime joinedAt = LocalDateTime.now();
    private LocalDateTime leftAt;

    private ChatMember(User user, String randomNickname) {
        this.user = user;
        this.randomNickname = randomNickname;
    }

    public static ChatMember create(User user, String nickname) {
        Objects.requireNonNull(user, "사용자는 필수입니다.");
        validateNickname(nickname);
        return new ChatMember(user, nickname);
    }

    private static void validateNickname(String nickname) {
        Objects.requireNonNull(nickname, "닉네임은 필수입니다.");
        if(nickname.length() < 2 || nickname.length() > 16) {
            throw new IllegalArgumentException("닉네임은 2자 이상, 16자 이하로 설정해야합니다.");
        }
    }

    synchronized boolean isActive() {
        return leftAt == null;
    }

    synchronized void leave() {
        if(!isActive()) {
            throw new IllegalStateException("이미 대화를 나간 사용자입니다.");
        }
        this.leftAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getRandomNickname() {
        return randomNickname;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public LocalDateTime getLeftAt() {
        return leftAt;
    }
}
