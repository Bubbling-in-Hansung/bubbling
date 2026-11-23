package bubbling.client.ui;

import bubbling.client.ui.model.UiSession;
import bubbling.common.dto.chat.ChatEndResponse;
import bubbling.common.dto.chat.ChatMessageDto;
import bubbling.common.dto.chat.ChatRequestDto;
import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.ChatRequestRole;
import bubbling.common.enums.ChatRequestStatus;
import bubbling.common.protocol.MessageType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UiSessionTest {
    private static final class Time extends Clock {
        private Instant value = Instant.parse("2026-11-17T00:00:00Z");

        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(value, zone); }
        public Instant instant() { return value; }
        void advance(long seconds) { value = value.plusSeconds(seconds); }
    }

    private final Time clock = new Time();
    private final List<MessageType> sent = new ArrayList<>();
    private final UiSession session = new UiSession(clock, (type, payload) -> sent.add(type));

    @Test
    void rejectionKeepsStatusAndDeadlineAcrossRepeatedServerResults() {
        var room = session.applyBubble(ServerRoomFixtures.bubble("first", "방", BubbleCategory.DAILY, "2099-01-01T00:00:00"));
        session.request(room);
        assertNull(session.outgoing(room));
        var rejection = new ChatRequestDto("request", room.id, ChatRequestStatus.REJECTED,
                "테스터", ChatRequestRole.REQUESTER, null);
        session.applyRequest(rejection);
        assertEquals(60, session.retrySeconds(room));
        clock.advance(30);
        session.applyRequest(rejection);
        assertEquals(30, session.retrySeconds(room));
        assertEquals(ChatRequestStatus.REJECTED, session.outgoing(room).status);
        assertThrows(IllegalStateException.class, () -> session.request(room));
        clock.advance(30);
        session.request(room);
        assertEquals(List.of(MessageType.CHAT_REQUEST, MessageType.CHAT_REQUEST), sent);
    }

    @Test
    void speechExpiresWithoutDeletingHistory() {
        var bubble = ServerRoomFixtures.bubble("first", "방", BubbleCategory.DAILY, "2099-01-01T00:00:00");
        var room = session.applyChatRoom(ServerRoomFixtures.chat(bubble));
        for (int i = 0; i < 4; i++) {
            session.applyMessage(new ChatMessageDto("message-" + i, "chat-first", "me", "테스터",
                    "message " + i, "2026-11-17T00:00:00"));
        }
        assertEquals(3, session.speech(room, "me").size());
        assertEquals("message 1", session.speech(room, "me").getFirst().text());
        clock.advance(7);
        assertTrue(session.speech(room, "me").isEmpty());
        session.markEnded(new ChatEndResponse("chat-first", "first"));
        assertFalse(room.joined);
        assertTrue(room.ended);
        assertEquals(4, room.messages.size());
    }

    @Test
    void messageSendKeepsServerExpirationUntilBubbleUpdateArrives() {
        var bubble = ServerRoomFixtures.bubble("first", "방", BubbleCategory.DAILY, "2026-11-17T00:20:00");
        var room = session.applyChatRoom(ServerRoomFixtures.chat(bubble));
        clock.advance(60);
        session.send(room, "new message");
        assertEquals(List.of(MessageType.CHAT_MESSAGE), sent);
        assertTrue(room.messages.isEmpty());
        assertEquals(Instant.parse("2026-11-17T00:20:00Z"), room.expiresAt);

        session.applyBubble(ServerRoomFixtures.bubble("first", "방", BubbleCategory.DAILY, "2026-11-17T00:21:00"));
        assertEquals(Instant.parse("2026-11-17T00:21:00Z"), room.expiresAt);
        clock.advance(20 * 60L);
        assertTrue(session.hidden(room));
        assertTrue(room.joined);
        assertFalse(room.ended);
    }
}
