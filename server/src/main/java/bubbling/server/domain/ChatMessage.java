package bubbling.server.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ChatMessage {
    private final String id = UUID.randomUUID().toString();
    private final ChatMember chatMember;
    private final String message;
    private final LocalDateTime createdAt=  LocalDateTime.now();

    private ChatMessage(ChatMember chatMember, String message) {
        this.chatMember = chatMember;
        this.message = message;
    }

    public static ChatMessage create(ChatMember chatMember, String message) {
        Objects.requireNonNull(chatMember, "채팅 멤버는 필수입니다.");
        validateMessage(message);
        return new ChatMessage(chatMember, message);
    }

    private static void validateMessage(String message) {
        Objects.requireNonNull(message, "메세지는 필수입니다.");
        if(message.isEmpty()) {
            throw new IllegalArgumentException("메세지는 비어있을 수 없습니다.");
        }
    }

    public String getId() {
        return id;
    }

    public ChatMember getChatMember() {
        return chatMember;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
