package bubbling.server.domain;

import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatMessageExceptionCode;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ChatMessage {
    private final String id = UUID.randomUUID().toString();
    private final ChatRoom chatRoom;
    private final ChatMember chatMember;
    private final String message;
    private final LocalDateTime createdAt=  LocalDateTime.now();

    private ChatMessage(ChatRoom chatRoom, ChatMember chatMember, String message) {
        this.chatRoom = chatRoom;
        this.chatMember = chatMember;
        this.message = message;
    }

    public static ChatMessage create(ChatRoom chatRoom, ChatMember chatMember, String message) {
        Objects.requireNonNull(chatMember, "채팅 멤버는 필수입니다.");
        validateMessage(message);
        return new ChatMessage(chatRoom, chatMember, message);
    }

    public static void validateMessage(String message) {
        Objects.requireNonNull(message, "메세지는 필수입니다.");
        if(message.isEmpty() || message.length() > 1000) {
            throw new BaseException(ChatMessageExceptionCode.INVALID_MESSAGE);
        }
    }

    public String getId() {
        return id;
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
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
