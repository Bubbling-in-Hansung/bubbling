package bubbling.client.service;

import bubbling.client.ui.model.ConversationMode;
import bubbling.client.ui.model.RoomView;
import bubbling.common.dto.auth.PasswordChangeRequest;
import bubbling.common.dto.bubble.BubbleCreateRequest;
import bubbling.common.dto.bubble.BubbleDeleteRequest;
import bubbling.common.dto.bubble.BubbleDetailRequest;
import bubbling.common.dto.bubble.BubbleReactionRequest;
import bubbling.common.dto.bubble.BubbleUpdateRequest;
import bubbling.common.dto.chat.ChatEndRequest;
import bubbling.common.dto.chat.ChatJoinRequest;
import bubbling.common.dto.chat.ChatLeaveRequest;
import bubbling.common.dto.chat.ChatMessageRequest;
import bubbling.common.dto.chat.ChatPositionDto;
import bubbling.common.dto.chat.ChatRequestCreateRequest;
import bubbling.common.dto.chat.ChatRequestDecisionRequest;
import bubbling.common.dto.chat.ChatTypingDto;
import bubbling.common.dto.game.GameResultRequest;
import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleMethod;
import bubbling.common.enums.GameType;
import bubbling.common.protocol.MessageType;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

import static bubbling.common.protocol.MessageType.*;

/** Maps screen actions to the common request types used by the live session. */
public final class SessionRequests {
    private final BiConsumer<MessageType, Object> send;

    public SessionRequests(BiConsumer<MessageType, Object> send) {
        this.send = send;
    }

    public void createBubble(RoomView room, int minutes) {
        send.accept(BUBBLE_CREATE, new BubbleCreateRequest(room.title(), category(room), method(room),
                room.capacity(), !room.privateRoom(), minutes));
    }

    public void updateBubble(String bubbleId, RoomView room, int minutes) {
        send.accept(BUBBLE_UPDATE, new BubbleUpdateRequest(bubbleId, room.title(), category(room), method(room),
                room.capacity(), !room.privateRoom(), minutes));
    }

    public void deleteBubble(String bubbleId) {
        send.accept(BUBBLE_DELETE, new BubbleDeleteRequest(bubbleId));
    }

    public void reactToBubble(String bubbleId) {
        send.accept(BUBBLE_REACT, new BubbleReactionRequest(bubbleId));
    }

    public void requestBubbleDetail(String bubbleId) {
        send.accept(BUBBLE_DETAIL_REQUEST, new BubbleDetailRequest(bubbleId));
    }

    public void requestChat(String bubbleId) {
        send.accept(CHAT_REQUEST, new ChatRequestCreateRequest(bubbleId));
    }

    public void decideChat(String requestId, boolean accept) {
        send.accept(accept ? CHAT_ACCEPT : CHAT_REJECT, new ChatRequestDecisionRequest(requestId));
    }

    public void joinChat(String bubbleId) {
        send.accept(CHAT_JOIN, new ChatJoinRequest(bubbleId));
    }

    public void leaveChat(String bubbleId) {
        send.accept(CHAT_LEAVE, new ChatLeaveRequest(bubbleId));
    }

    public void endChat(String roomId) {
        send.accept(CHAT_END, new ChatEndRequest(roomId));
    }

    public void sendMessage(String roomId, String text) {
        send.accept(CHAT_MESSAGE, new ChatMessageRequest(roomId, text.strip()));
    }

    public void changePassword(String currentPassword, String newPassword) {
        send.accept(PASSWORD_CHANGE_REQUEST, new PasswordChangeRequest(currentPassword, newPassword));
    }

    public void requestChatRequests() {
        send.accept(CHAT_REQUEST_LIST_REQUEST, null);
    }

    public void moveCharacter(String roomId, String memberId, int x, int y) {
        send.accept(CHAT_POSITION_REQUEST, new ChatPositionDto(roomId, memberId, x, y));
    }

    public void sendTyping(String roomId, String memberId, boolean typing) {
        send.accept(CHAT_TYPING, new ChatTypingDto(roomId, memberId, typing));
    }

    public void saveGameResult(GameType gameType, int score) {
        send.accept(GAME_RESULT_REQUEST, new GameResultRequest(
                Objects.requireNonNull(gameType), Math.max(0, score)));
    }

    private static BubbleCategory category(RoomView room) {
        return Arrays.stream(BubbleCategory.values()).filter(value -> value.getLabel().equals(room.category()))
                .findFirst().orElseThrow();
    }

    private static BubbleMethod method(RoomView room) {
        return ConversationMode.fromLabel(room.mode()).method();
    }
}
