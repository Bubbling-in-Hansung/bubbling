package bubbling.server.domain;

import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatMemberExceptionCode;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ChatMember {
    private final String id = UUID.randomUUID().toString();
    private final User user;
    private final ChatRoom chatRoom;
    private final String randomNickname;
    private final LocalDateTime joinedAt = LocalDateTime.now();
    private LocalDateTime leftAt;

    private ChatMember(User user, ChatRoom chatRoom, String randomNickname) {
        this.user = user;
        this.chatRoom = chatRoom;
        this.randomNickname = randomNickname;
    }

    public static ChatMember create(User user, ChatRoom chatRoom, String nickname) {
        Objects.requireNonNull(user, "사용자는 필수입니다.");
        Objects.requireNonNull(chatRoom, "채팅방은 필수입니다.");
        validateNickname(nickname);
        return new ChatMember(user, chatRoom, nickname);
    }

    private static void validateNickname(String nickname) {
        Objects.requireNonNull(nickname, "닉네임은 필수입니다.");
        if(nickname.length() < 2 || nickname.length() > 16) {
            throw new BaseException(ChatMemberExceptionCode.INVALID_NICKNAME);
        }
    }

    public synchronized boolean isActive() {
        return leftAt == null;
    }

    public synchronized void leave() {
        if(!isActive()) {
            throw new BaseException(ChatMemberExceptionCode.ALREADY_LEFT);
        }
        this.leftAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMember that = (ChatMember) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
