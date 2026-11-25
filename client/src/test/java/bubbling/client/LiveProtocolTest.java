package bubbling.client;

import bubbling.client.network.ClientMessageDispatcher;
import bubbling.client.ui.ApplicationView;
import bubbling.client.ui.model.UiData;
import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.auth.LoginResponse;
import bubbling.common.dto.bubble.BubbleDto;
import bubbling.common.dto.bubble.BubbleRemovedEvent;
import bubbling.common.dto.bubble.BubbleUpdateRequest;
import bubbling.common.dto.chat.ChatEndResponse;
import bubbling.common.dto.chat.ChatMessageDto;
import bubbling.common.dto.chat.ChatParticipantDto;
import bubbling.common.dto.chat.ChatRoomDto;
import bubbling.common.dto.chat.ChatRequestDto;
import bubbling.common.dto.system.ExceptionResponse;
import bubbling.common.dto.system.InitialStateDto;
import bubbling.common.dto.user.UserDto;
import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleMethod;
import bubbling.common.enums.BubbleStatus;
import bubbling.common.enums.ChatRoomStatus;
import bubbling.common.enums.ChatRequestRole;
import bubbling.common.enums.ChatRequestStatus;
import bubbling.common.enums.ProfileCharacter;
import bubbling.common.protocol.MessageEnvelope;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static bubbling.common.protocol.MessageType.*;
import static org.junit.jupiter.api.Assertions.*;

class LiveProtocolTest {
    @Test
    void appliesRoomOwnershipAndCorrelatesPayloadlessLeaveAcknowledgment() throws Exception {
        var sent = new ArrayList<MessageEnvelope>();
        var codec = new JsonCodec();
        var dispatcher = new ClientMessageDispatcher();
        var live = new LiveSession(sent::add);
        live.registerHandlers(dispatcher);
        var data = new UiData(live::send);
        var application = new AtomicReference<ApplicationView>();

        SwingUtilities.invokeAndWait(() -> {
            application.set(new ApplicationView(data, live));
            live.connected();
            live.authenticate(false, "tester123", "password", "");
            String loginId = sent.getLast().requestId();
            var user = new UserDto("user", "tester123", "테스터", ProfileCharacter.EXAMPLE, "2026-11-23T00:00:00");
            dispatcher.dispatch(new MessageEnvelope(LOGIN_RESULT, loginId, codec.toPayload(new LoginResponse(user))));

            var owned = bubble("owned");
            var shared = bubble("shared");
            dispatcher.dispatch(new MessageEnvelope(INITIAL_STATE, "initial",
                    codec.toPayload(new InitialStateDto(List.of(owned, shared), 2))));
            dispatcher.dispatch(new MessageEnvelope(MY_CHAT_ROOM_LIST, "list",
                    codec.toPayload(List.of(chat(owned, true), chat(shared, false)))));

        });
        SwingUtilities.invokeAndWait(() -> {
            var ownRoom = data.session().room("owned");
            assertTrue(ownRoom.view.owner());
            data.session().update(ownRoom, ownRoom.view, 20);
            var update = sent.getLast();
            assertEquals(BUBBLE_UPDATE, update.type());
            assertEquals(20, codec.fromPayload(update.payload(), BubbleUpdateRequest.class).expireMinute());
            var room = data.session().room("shared");
            application.get().enterRoom(room.view);
            assertEquals("chat", application.get().currentPage());
            data.session().leave(room);
            var leave = sent.getLast();
            assertEquals(CHAT_LEAVE, leave.type());
            dispatcher.dispatch(new MessageEnvelope(CHAT_LEFT, "unrelated", null));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("chat", application.get().currentPage());
            dispatcher.dispatch(new MessageEnvelope(CHAT_LEFT, sent.getLast().requestId(), null));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("home", application.get().currentPage());
            assertFalse(data.session().room("shared").joined);
            assertFalse(data.session().room("shared").listed);
            assertFalse(data.session().room("shared").visited);
            assertTrue(data.session().room("owned").joined);
            assertEquals(MY_CHAT_ROOM_LIST_REQUEST, sent.getLast().type());
            var updated = bubble("owned", "2099-01-01T00:30:00");
            dispatcher.dispatch(new MessageEnvelope(BUBBLE_UPDATED, null, codec.toPayload(updated)));
        });
        SwingUtilities.invokeAndWait(() -> {
            var expected = LocalDateTime.parse("2099-01-01T00:30:00").atZone(ZoneId.systemDefault()).toInstant();
            assertEquals(expected, data.session().room("owned").expiresAt);
            assertTrue(data.session().room("owned").view.owner());
            live.close();
        });
    }

    @Test
    void mergesSnapshotsAndDeduplicatesHistoryUsingNewIds() {
        var state = new UiData((type, payload) -> {}).session();
        var bubble = bubble("owned");
        state.applyChatRooms(new ChatRoomDto[] {chat(bubble, true)});
        state.applyInitialState(new InitialStateDto(List.of(bubble), 1));
        var room = state.room("owned");
        assertTrue(room.view.owner());
        assertTrue(room.view.privateRoom());
        assertTrue(room.joined);
        var message = new ChatMessageDto("message", "chat-owned", "me", "테스터", "안녕하세요", "2026-11-23T00:00:00");
        state.applyMessage(message);
        state.applyHistory("chat-owned", new ChatMessageDto[] {message});
        assertEquals(1, room.messages.size());
        assertTrue(room.messages.getFirst().mine());
    }

    @Test
    void reflectsServerCancellationOnlyForPendingRequestsOnExpiry() {
        var state = new UiData((type, payload) -> {}).session();
        state.applyRequest(new ChatRequestDto("pending", "bubble", ChatRequestStatus.PENDING, "대기 중",
                ChatRequestRole.OWNER, null));
        state.applyRequest(new ChatRequestDto("accepted", "bubble", ChatRequestStatus.ACCEPTED, "승인됨",
                ChatRequestRole.OWNER, null));
        state.markBubbleRemoved(new BubbleRemovedEvent("bubble", BubbleStatus.EXPIRED));
        assertEquals(ChatRequestStatus.CANCELLED, state.requests().getFirst().status);
        assertEquals(ChatRequestStatus.ACCEPTED, state.requests().getLast().status);
    }

    @Test
    void marksBubbleDeletedAndClearsConversationListing() {
        var state = new UiData((type, payload) -> {}).session();
        var bubble = bubble("owned");
        var room = state.applyChatRoom(chat(bubble, true));
        state.visit(room);
        assertTrue(room.listed);
        assertTrue(room.visited);
        assertFalse(room.deleted);

        state.markBubbleRemoved(new BubbleRemovedEvent("owned", BubbleStatus.EXPIRED));
        assertTrue(room.removed);
        assertFalse(room.deleted);

        state.markBubbleRemoved(new BubbleRemovedEvent("owned", BubbleStatus.DELETED));
        assertTrue(room.deleted);
        assertFalse(room.listed);
        assertFalse(room.visited);
        assertFalse(room.joined);
    }

    @Test
    void appliesRequestListAndCancelsPendingRequestsOnChatEnd() {
        var state = new UiData((type, payload) -> {}).session();
        var owned = bubble("owned");
        var ended = bubble("ended");
        state.applyChatRoom(chat(owned, true));
        var endedRoom = state.applyChatRoom(chat(ended, true));
        state.markEnded(new ChatEndResponse(endedRoom.conversation.chatRoomId(), "ended"));

        state.applyRequest(new ChatRequestDto("accepted", "owned", ChatRequestStatus.ACCEPTED, "기존승인",
                ChatRequestRole.OWNER, null));

        int added = state.applyRequestList(new ChatRequestDto[] {
                new ChatRequestDto("accepted", "owned", ChatRequestStatus.PENDING, "기존승인",
                        ChatRequestRole.OWNER, null),
                new ChatRequestDto("pending-1", "owned", ChatRequestStatus.PENDING, "대기1",
                        ChatRequestRole.OWNER, null),
                new ChatRequestDto("pending-2", "owned", ChatRequestStatus.PENDING, "대기2",
                        ChatRequestRole.OWNER, null),
                new ChatRequestDto("ended-req", "ended", ChatRequestStatus.PENDING, "종료방요청",
                        ChatRequestRole.OWNER, null)
        });

        assertEquals(2, added);
        assertEquals(3, state.requests().size());
        assertEquals(ChatRequestStatus.ACCEPTED, state.requests().get(0).status);
        assertEquals(ChatRequestStatus.PENDING, state.requests().get(1).status);
        assertEquals(ChatRequestStatus.PENDING, state.requests().get(2).status);

        state.markEnded(new ChatEndResponse("chat-owned", "owned"));
        assertEquals(ChatRequestStatus.ACCEPTED, state.requests().get(0).status);
        assertEquals(ChatRequestStatus.CANCELLED, state.requests().get(1).status);
        assertEquals(ChatRequestStatus.CANCELLED, state.requests().get(2).status);
    }

    @Test
    void logsOutAndAllowsReloginOnSameConnection() throws Exception {
        var sent = new ArrayList<MessageEnvelope>();
        var codec = new JsonCodec();
        var dispatcher = new ClientMessageDispatcher();
        var live = new LiveSession(sent::add);
        live.registerHandlers(dispatcher);
        var data = new UiData(live::send);
        var application = new AtomicReference<ApplicationView>();

        SwingUtilities.invokeAndWait(() -> {
            application.set(new ApplicationView(data, live));
            live.connected();
            live.authenticate(false, "tester123", "password", "");
            String loginId = sent.getLast().requestId();
            var user = new UserDto("user-1", "tester123", "테스터", ProfileCharacter.EXAMPLE, "2026-11-23T00:00:00");
            dispatcher.dispatch(new MessageEnvelope(LOGIN_RESULT, loginId, codec.toPayload(new LoginResponse(user))));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("home", application.get().currentPage());
            assertTrue(sent.stream().anyMatch(msg -> msg.type() == CHAT_REQUEST_LIST_REQUEST));
            dispatcher.dispatch(new MessageEnvelope(CHAT_REQUEST_LIST, null, codec.toPayload(List.of(
                    new ChatRequestDto("req-1", "owned", ChatRequestStatus.PENDING, "요청자",
                            ChatRequestRole.OWNER, null)))));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, data.session().requests().size());
            live.logout();
            var logoutMsg = sent.getLast();
            assertEquals(LOGOUT, logoutMsg.type());
            dispatcher.dispatch(new MessageEnvelope(LOGOUT_RESULT, logoutMsg.requestId(), null));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("login", application.get().currentPage());
            assertTrue(data.session().rooms().isEmpty());
            assertTrue(data.session().requests().isEmpty());

            live.authenticate(false, "other123", "password", "");
            String secondLoginId = sent.getLast().requestId();
            var secondUser = new UserDto("user-2", "other123", "다른사용자", ProfileCharacter.EXAMPLE,
                    "2026-11-23T00:00:00");
            dispatcher.dispatch(new MessageEnvelope(LOGIN_RESULT, secondLoginId,
                    codec.toPayload(new LoginResponse(secondUser))));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("home", application.get().currentPage());
            live.close();
        });
    }

    @Test
    void keepsReactionCountWhenReactionCooldownErrorArrives() throws Exception {
        var sent = new ArrayList<MessageEnvelope>();
        var codec = new JsonCodec();
        var dispatcher = new ClientMessageDispatcher();
        var live = new LiveSession(sent::add);
        live.registerHandlers(dispatcher);
        var data = new UiData(live::send);
        var application = new AtomicReference<ApplicationView>();

        SwingUtilities.invokeAndWait(() -> {
            application.set(new ApplicationView(data, live));
            live.connected();
            live.authenticate(false, "tester123", "password", "");
            String loginId = sent.getLast().requestId();
            var user = new UserDto("user-1", "tester123", "테스터", ProfileCharacter.EXAMPLE, "2026-11-23T00:00:00");
            dispatcher.dispatch(new MessageEnvelope(LOGIN_RESULT, loginId, codec.toPayload(new LoginResponse(user))));
            dispatcher.dispatch(new MessageEnvelope(INITIAL_STATE, "init",
                    codec.toPayload(new InitialStateDto(List.of(bubble("shared")), 1))));
        });
        SwingUtilities.invokeAndWait(() -> {
            var room = data.session().room("shared");
            data.session().react(room);
            var reactMsg = sent.getLast();
            assertEquals(BUBBLE_REACT, reactMsg.type());
            dispatcher.dispatch(new MessageEnvelope(ERROR, reactMsg.requestId(),
                    codec.toPayload(new ExceptionResponse("BUBBLE_008", "공감은 1분에 한 번씩 할 수 있습니다."))));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(0, data.session().room("shared").reactionCount);
            live.close();
        });
    }

    private static BubbleDto bubble(String id) {
        return bubble(id, "2099-01-01T00:00:00");
    }

    private static BubbleDto bubble(String id, String expiresAt) {
        return new BubbleDto(id, "고민", BubbleCategory.DAILY, BubbleMethod.TOGETHER, 4, 2, false,
                BubbleStatus.ALIVE, 0, "2026-11-23T00:00:00", expiresAt);
    }

    private static ChatRoomDto chat(BubbleDto bubble, boolean owner) {
        return new ChatRoomDto("chat-" + bubble.bubbleId(), bubble, ChatRoomStatus.ACTIVE, "me", owner,
                List.of(new ChatParticipantDto("me", "테스터", ProfileCharacter.EXAMPLE)), bubble.createdAt(), null);
    }
}
