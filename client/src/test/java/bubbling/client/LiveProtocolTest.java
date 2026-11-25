package bubbling.client;

import bubbling.client.network.ClientMessageDispatcher;
import bubbling.client.ui.ApplicationView;
import bubbling.client.ui.model.UiData;
import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.auth.LoginResponse;
import bubbling.common.dto.bubble.BubbleDto;
import bubbling.common.dto.bubble.BubbleRemovedEvent;
import bubbling.common.dto.bubble.BubbleUpdateRequest;
import bubbling.common.dto.chat.ChatMessageDto;
import bubbling.common.dto.chat.ChatParticipantDto;
import bubbling.common.dto.chat.ChatRoomDto;
import bubbling.common.dto.chat.ChatRequestDto;
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
