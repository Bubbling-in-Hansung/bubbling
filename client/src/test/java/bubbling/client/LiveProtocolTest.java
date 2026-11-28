package bubbling.client;

import bubbling.client.network.ClientMessageDispatcher;
import bubbling.client.ui.ApplicationView;
import bubbling.client.ui.dialog.NotificationCards;
import bubbling.client.ui.drawing.MessageBubble;
import bubbling.client.ui.drawing.RoomScene;
import bubbling.client.ui.drawing.TypingBubble;
import bubbling.client.ui.game.CatchFavoriteGamePanel;
import bubbling.client.ui.game.DinoRunGamePanel;
import bubbling.client.ui.game.DinoSprites;
import bubbling.client.ui.game.GameGuidePanel;
import bubbling.client.ui.game.GameLobbyPanel;
import bubbling.client.ui.game.GameView;
import bubbling.client.ui.model.UiData;
import bubbling.client.ui.screen.ChatScreen;
import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.auth.LoginResponse;
import bubbling.common.dto.bubble.BubbleDetailRequest;
import bubbling.common.dto.bubble.BubbleDto;
import bubbling.common.dto.bubble.BubbleRemovedEvent;
import bubbling.common.dto.bubble.BubbleUpdateRequest;
import bubbling.common.dto.chat.ChatEndResponse;
import bubbling.common.dto.chat.ChatMemberEvent;
import bubbling.common.dto.chat.ChatMessageDto;
import bubbling.common.dto.chat.ChatParticipantDto;
import bubbling.common.dto.chat.ChatPositionDto;
import bubbling.common.dto.chat.ChatRoomDto;
import bubbling.common.dto.chat.ChatRequestDto;
import bubbling.common.dto.chat.ChatTypingDto;
import bubbling.common.dto.game.GameResultRequest;
import bubbling.common.dto.system.ExceptionResponse;
import bubbling.common.dto.system.InitialStateDto;
import bubbling.common.dto.user.UserDto;
import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleMethod;
import bubbling.common.enums.BubbleStatus;
import bubbling.common.enums.ChatRoomStatus;
import bubbling.common.enums.ChatRequestRole;
import bubbling.common.enums.ChatRequestStatus;
import bubbling.common.enums.GameType;
import bubbling.common.enums.ProfileCharacter;
import bubbling.common.protocol.MessageEnvelope;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test
    void syncsCharacterPositionRequestAndEventInChatRoom() throws Exception {
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
            var owned = bubble("owned");
            var chatRoom = new ChatRoomDto("chat-owned", owned, ChatRoomStatus.ACTIVE, "me", true,
                    List.of(
                            new ChatParticipantDto("me", "테스터", ProfileCharacter.EXAMPLE),
                            new ChatParticipantDto("peer", "상대방", ProfileCharacter.EXAMPLE)),
                    owned.createdAt(), null, null);
            dispatcher.dispatch(new MessageEnvelope(MY_CHAT_ROOM_LIST, "list", codec.toPayload(List.of(chatRoom))));
        });
        SwingUtilities.invokeAndWait(() -> {
            var room = data.session().room("owned");
            application.get().enterRoom(room.view);
            assertEquals("chat", application.get().currentPage());
            RoomScene scene = findComponent(application.get(), RoomScene.class);
            assertNotNull(scene);
            scene.setSize(1000, 600);

            data.session().moveCharacter(room, 650, 400);
            var moveMsg = sent.getLast();
            assertEquals(CHAT_POSITION_REQUEST, moveMsg.type());
            assertEquals(new ChatPositionDto("chat-owned", "me", 650, 400),
                    codec.fromPayload(moveMsg.payload(), ChatPositionDto.class));

            dispatcher.dispatch(new MessageEnvelope(CHAT_POSITION_CHANGED, moveMsg.requestId(), null));
            dispatcher.dispatch(new MessageEnvelope(CHAT_POSITION_EVENT, null,
                    codec.toPayload(new ChatPositionDto("chat-owned", "peer", 320, 720))));
        });
        SwingUtilities.invokeAndWait(() -> {
            RoomScene scene = findComponent(application.get(), RoomScene.class);
            assertNotNull(scene);
            assertEquals(1, scene.paintOrder().getLast());
            assertEquals(320, scene.participantHead(1).x);
            assertEquals(370, scene.participantHead(1).y);
            live.close();
        });
    }

    @Test
    void notifiesAndIncrementsChatBadgeWhenMemberJoinsOpenRoom() throws Exception {
        var sent = new ArrayList<MessageEnvelope>();
        var codec = new JsonCodec();
        var dispatcher = new ClientMessageDispatcher();
        var live = new LiveSession(sent::add);
        live.registerHandlers(dispatcher);
        var data = new UiData(live::send);
        var application = new AtomicReference<ApplicationView>();
        var root = new AtomicReference<JRootPane>();

        SwingUtilities.invokeAndWait(() -> {
            var pane = new JRootPane();
            var view = new ApplicationView(data, live);
            pane.setSize(1200, 800);
            pane.setContentPane(view);
            root.set(pane);
            application.set(view);
            live.connected();
            live.authenticate(false, "tester123", "password", "");
            String loginId = sent.getLast().requestId();
            var user = new UserDto("user-1", "tester123", "테스터", ProfileCharacter.EXAMPLE, "2026-11-23T00:00:00");
            dispatcher.dispatch(new MessageEnvelope(LOGIN_RESULT, loginId, codec.toPayload(new LoginResponse(user))));

            var openBubble = new BubbleDto("open", "공개 고민", BubbleCategory.DAILY, BubbleMethod.TOGETHER,
                    4, 1, true, BubbleStatus.ALIVE, 0, "2026-11-23T00:00:00", "2099-01-01T00:00:00");
            var privateBubble = bubble("private");
            dispatcher.dispatch(new MessageEnvelope(MY_CHAT_ROOM_LIST, "list",
                    codec.toPayload(List.of(chat(openBubble, true), chat(privateBubble, true)))));
        });
        SwingUtilities.invokeAndWait(() -> {
            dispatcher.dispatch(new MessageEnvelope(CHAT_MEMBER_JOINED, null, codec.toPayload(
                    new ChatMemberEvent("chat-open",
                            new ChatParticipantDto("member-2", "말랑한 토끼", ProfileCharacter.BUNNY)))));
        });
        SwingUtilities.invokeAndWait(() -> {
            JButton chatButton = findButton(application.get(), "채팅");
            assertNotNull(chatButton);
            assertEquals("채팅, 확인하지 않은 알림 1건", chatButton.getAccessibleContext().getAccessibleName());
            assertNotNull(findByAccessibleName(root.get().getLayeredPane(), "새 참여자가 들어왔어요"));

            dispatcher.dispatch(new MessageEnvelope(CHAT_MEMBER_JOINED, null, codec.toPayload(
                    new ChatMemberEvent("chat-open",
                            new ChatParticipantDto("member-2", "말랑한 토끼", ProfileCharacter.BUNNY)))));
            dispatcher.dispatch(new MessageEnvelope(CHAT_MEMBER_JOINED, null, codec.toPayload(
                    new ChatMemberEvent("chat-open",
                            new ChatParticipantDto("member-3", "포근한 여우", ProfileCharacter.FOX)))));
            dispatcher.dispatch(new MessageEnvelope(CHAT_MEMBER_JOINED, null, codec.toPayload(
                    new ChatMemberEvent("chat-private",
                            new ChatParticipantDto("member-4", "졸린 펭귄", ProfileCharacter.PENGUIN)))));
        });
        SwingUtilities.invokeAndWait(() -> {
            JButton chatButton = findButton(application.get(), "채팅");
            assertNotNull(chatButton);
            assertEquals("채팅, 확인하지 않은 알림 2건", chatButton.getAccessibleContext().getAccessibleName());

            application.get().showPage("conversations");
            assertEquals("채팅", chatButton.getAccessibleContext().getAccessibleName());
            NotificationCards.clear(application.get());
            live.close();
        });
    }

    @Test
    void maintainsConnectionOnMatchingPongAndDisconnectsOnPongTimeout() throws Exception {
        var sent = new ArrayList<MessageEnvelope>();
        var closedCount = new AtomicInteger();
        var codec = new JsonCodec();
        var dispatcher = new ClientMessageDispatcher();
        var live = new LiveSession(sent::add, closedCount::incrementAndGet);
        live.registerHandlers(dispatcher);
        var data = new UiData(live::send);
        var application = new AtomicReference<ApplicationView>();

        SwingUtilities.invokeAndWait(() -> {
            application.set(new ApplicationView(data, live));
            live.connected();
            assertTrue(live.isPingScheduled());
            live.authenticate(false, "tester123", "password", "");
            String loginId = sent.getLast().requestId();
            var user = new UserDto("user-1", "tester123", "테스터", ProfileCharacter.EXAMPLE, "2026-11-23T00:00:00");
            dispatcher.dispatch(new MessageEnvelope(LOGIN_RESULT, loginId, codec.toPayload(new LoginResponse(user))));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("home", application.get().currentPage());
            live.sendPing();
            var ping = sent.getLast();
            assertEquals(PING, ping.type());
            assertNotNull(ping.requestId());
            assertTrue(live.isAwaitingPong());

            dispatcher.dispatch(new MessageEnvelope(PONG, "unrelated", null));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(live.isAwaitingPong());
            String pingId = sent.getLast().requestId();
            dispatcher.dispatch(new MessageEnvelope(PONG, pingId, null));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(live.isAwaitingPong());
            assertEquals(0, closedCount.get());
            assertEquals("home", application.get().currentPage());

            live.sendPing();
            assertTrue(live.isAwaitingPong());
            live.onPongTimeout();
            assertEquals(1, closedCount.get());
            assertFalse(live.isPingScheduled());
            assertFalse(live.isAwaitingPong());
            assertEquals("login", application.get().currentPage());

            live.disconnected("서버 연결이 끊겼습니다. 앱을 다시 실행해 주세요.");
            assertEquals(1, closedCount.get());
            live.close();
        });
    }

    @Test
    void requestsAndAppliesBubbleDetail() throws Exception {
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
            data.session().requestDetail(room);
            var detailReq = sent.getLast();
            assertEquals(BUBBLE_DETAIL_REQUEST, detailReq.type());
            assertEquals(new BubbleDetailRequest("shared"),
                    codec.fromPayload(detailReq.payload(), BubbleDetailRequest.class));

            var detailed = new BubbleDto("shared", "최신 고민 제목", BubbleCategory.CAREER, BubbleMethod.SOLUTION,
                    4, 3, true, BubbleStatus.ALIVE, 7, "2026-11-23T00:00:00", "2099-01-02T00:00:00");
            dispatcher.dispatch(new MessageEnvelope(BUBBLE_DETAIL, detailReq.requestId(), codec.toPayload(detailed)));
        });
        SwingUtilities.invokeAndWait(() -> {
            var room = data.session().room("shared");
            assertEquals("최신 고민 제목", room.view.title());
            assertEquals(3, room.people);
            assertEquals(7, room.reactionCount);
            assertEquals(LocalDateTime.parse("2099-01-02T00:00:00").atZone(ZoneId.systemDefault()).toInstant(),
                    room.expiresAt);
            live.close();
        });
    }

    @Test
    void syncsTypingStatusAndRendersTypingBubbleBelowSpeechMessages() throws Exception {
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
            var owned = bubble("owned");
            var chatRoom = new ChatRoomDto("chat-owned", owned, ChatRoomStatus.ACTIVE, "me", true,
                    List.of(
                            new ChatParticipantDto("me", "테스터", ProfileCharacter.EXAMPLE),
                            new ChatParticipantDto("peer", "상대방", ProfileCharacter.EXAMPLE)),
                    owned.createdAt(), null, null);
            dispatcher.dispatch(new MessageEnvelope(MY_CHAT_ROOM_LIST, "list", codec.toPayload(List.of(chatRoom))));
        });
        SwingUtilities.invokeAndWait(() -> {
            var room = data.session().room("owned");
            application.get().enterRoom(room.view);
            assertEquals("chat", application.get().currentPage());

            ChatScreen chatScreen = findComponent(application.get(), ChatScreen.class);
            assertNotNull(chatScreen);
            JTextField input = findComponent(chatScreen, JTextField.class);
            assertNotNull(input);
            long beforeTyping = sent.stream().filter(msg -> msg.type() == CHAT_TYPING).count();
            input.setText("안");
            input.setText("안녕");
            input.setText("안녕하세요");
            var typingSent = sent.stream().filter(msg -> msg.type() == CHAT_TYPING).toList();
            assertEquals(beforeTyping + 1, typingSent.size());
            assertEquals(new ChatTypingDto("chat-owned", "me", true),
                    codec.fromPayload(typingSent.getLast().payload(), ChatTypingDto.class));
            input.setText("");
            var afterClear = sent.stream().filter(msg -> msg.type() == CHAT_TYPING).toList();
            assertEquals(beforeTyping + 2, afterClear.size());
            assertEquals(new ChatTypingDto("chat-owned", "me", false),
                    codec.fromPayload(afterClear.getLast().payload(), ChatTypingDto.class));

            String now = LocalDateTime.now().toString();
            for (int i = 1; i <= 3; i++) {
                dispatcher.dispatch(new MessageEnvelope(CHAT_MESSAGE, null, codec.toPayload(
                        new ChatMessageDto("m-" + i, "chat-owned", "peer", "상대방", "메시지 " + i, now))));
            }
            dispatcher.dispatch(new MessageEnvelope(CHAT_TYPING_EVENT, null,
                    codec.toPayload(new ChatTypingDto("chat-owned", "peer", true))));
        });
        SwingUtilities.invokeAndWait(() -> {
            TypingBubble indicator = findComponent(application.get(), TypingBubble.class);
            assertNotNull(indicator);
            Container stack = indicator.getParent();
            assertNotNull(stack);
            long messageBubbles = java.util.Arrays.stream(stack.getComponents())
                    .filter(MessageBubble.class::isInstance).count();
            assertEquals(3, messageBubbles);
            assertSame(indicator, stack.getComponent(stack.getComponentCount() - 1));

            String now = LocalDateTime.now().toString();
            dispatcher.dispatch(new MessageEnvelope(CHAT_MESSAGE, null, codec.toPayload(
                    new ChatMessageDto("m-4", "chat-owned", "peer", "상대방", "완료 메시지", now))));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertNull(findComponent(application.get(), TypingBubble.class));
            dispatcher.dispatch(new MessageEnvelope(CHAT_TYPING_EVENT, null,
                    codec.toPayload(new ChatTypingDto("chat-owned", "peer", true))));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertNotNull(findComponent(application.get(), TypingBubble.class));
            dispatcher.dispatch(new MessageEnvelope(CHAT_TYPING_EVENT, null,
                    codec.toPayload(new ChatTypingDto("chat-owned", "peer", false))));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertNull(findComponent(application.get(), TypingBubble.class));
            live.close();
        });
    }

    @Test
    void supportsMiniGameEntryGuideCharacterSwitchAndResultSaving() throws Exception {
        var sent = new ArrayList<MessageEnvelope>();
        var codec = new JsonCodec();
        var dispatcher = new ClientMessageDispatcher();
        var live = new LiveSession(sent::add);
        live.registerHandlers(dispatcher);
        var data = new UiData(live::send);
        var application = new AtomicReference<ApplicationView>();
        var gameRoot = new AtomicReference<JRootPane>();
        var gameViewRef = new AtomicReference<GameView>();
        var closed = new AtomicBoolean(false);

        SwingUtilities.invokeAndWait(() -> {
            application.set(new ApplicationView(data, live));
            live.connected();
            live.authenticate(false, "tester123", "password", "");
            String loginId = sent.getLast().requestId();
            var user = new UserDto("user-1", "tester123", "테스터", ProfileCharacter.FOX, "2026-11-23T00:00:00");
            dispatcher.dispatch(new MessageEnvelope(LOGIN_RESULT, loginId, codec.toPayload(new LoginResponse(user))));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(DinoSprites.isLoaded());
            assertNotNull(findButton(application.get(), "게임"));

            var root = new JRootPane();
            var gameView = new GameView(data.session(), () -> closed.set(true));
            root.setSize(880, 640);
            root.setContentPane(gameView);
            root.doLayout();
            gameRoot.set(root);
            gameViewRef.set(gameView);

            GameLobbyPanel lobby = findComponent(gameView, GameLobbyPanel.class);
            assertNotNull(lobby);
            assertTrue(lobby.isVisible());
            assertEquals("여우", lobby.selectedCharacter().label());

            JButton guideButton = findButton(lobby, "게임 방법");
            JButton rankingButton = findButton(lobby, "랭킹 확인");
            JButton startButton = findButton(lobby, "게임 시작");
            JButton lobbyExitButton = findButton(lobby, "게임 종료");
            assertNotNull(guideButton);
            assertNotNull(rankingButton);
            assertNotNull(startButton);
            assertNotNull(lobbyExitButton);

            rankingButton.doClick();
            assertTrue(sent.stream().noneMatch(msg -> msg.type() == GAME_RANKING_REQUEST));
            assertNotNull(findByAccessibleName(root.getLayeredPane(), "요청을 처리하지 못했어요"));
            assertTrue(containsTextArea(root.getLayeredPane(), "미구현 된 내용입니다."));

            guideButton.doClick();
            GameGuidePanel guide = findComponent(gameView, GameGuidePanel.class);
            assertNotNull(guide);
            assertTrue(guide.isVisible());
            assertFalse(lobby.isVisible());
            assertNotNull(findButton(guide, "게임 종료"));

            JButton backToLobby = findButton(guide, "게임 선택으로");
            assertNotNull(backToLobby);
            backToLobby.doClick();
            assertTrue(lobby.isVisible());

            assertEquals(GameType.DINOSAUR_RUN, lobby.selectedGame());
            startButton.doClick();
            DinoRunGamePanel dinoPanel = findComponent(gameView, DinoRunGamePanel.class);
            assertNotNull(dinoPanel);
            assertTrue(dinoPanel.isVisible());
            assertTrue(dinoPanel.isRunning());
            assertNotNull(findButton(dinoPanel, "게임 종료"));
            JButton exitDino = findButton(dinoPanel, "게임 선택");
            assertNotNull(exitDino);
            exitDino.doClick();
            assertFalse(dinoPanel.isRunning());
            assertTrue(lobby.isVisible());

            JButton penguinButton = findButton(lobby, "펭귄");
            assertNotNull(penguinButton);
            penguinButton.doClick();
            assertEquals(GameType.CATCH_FAVORITE, lobby.selectedGame());
            assertEquals("펭귄", lobby.selectedCharacter().label());

            startButton.doClick();
            CatchFavoriteGamePanel catchPanel = findComponent(gameView, CatchFavoriteGamePanel.class);
            assertNotNull(catchPanel);
            assertTrue(catchPanel.isVisible());
            assertTrue(catchPanel.isRunning());
            assertNotNull(findButton(catchPanel, "게임 종료"));
            assertNotNull(findByAccessibleName(catchPanel, "펭귄 ♥ 물고기"));

            assertTrue(CatchFavoriteGamePanel.spawnIntervalForProgress(0.0)
                    > CatchFavoriteGamePanel.spawnIntervalForProgress(0.5));
            assertTrue(CatchFavoriteGamePanel.spawnIntervalForProgress(0.5)
                    > CatchFavoriteGamePanel.spawnIntervalForProgress(1.0));
            assertTrue(CatchFavoriteGamePanel.maxItemsForProgress(0.0)
                    < CatchFavoriteGamePanel.maxItemsForProgress(0.5));
            assertTrue(CatchFavoriteGamePanel.maxItemsForProgress(0.5)
                    < CatchFavoriteGamePanel.maxItemsForProgress(1.0));
            assertTrue(CatchFavoriteGamePanel.minLifetimeForProgress(0.0)
                    > CatchFavoriteGamePanel.minLifetimeForProgress(0.5));
            assertTrue(CatchFavoriteGamePanel.minLifetimeForProgress(0.5)
                    > CatchFavoriteGamePanel.minLifetimeForProgress(1.0));
            assertTrue(CatchFavoriteGamePanel.minLifetimeForProgress(1.0) >= 300L);
            assertTrue(CatchFavoriteGamePanel.spawnIntervalForProgress(1.0) <= 80L);
            assertTrue(CatchFavoriteGamePanel.maxItemsForProgress(1.0) >= 30);

            JButton exitToLobby = findButton(catchPanel, "게임 선택");
            assertNotNull(exitToLobby);
            exitToLobby.doClick();
            assertFalse(catchPanel.isRunning());
            assertTrue(lobby.isVisible());

            assertFalse(closed.get());
            lobbyExitButton.doClick();
            assertTrue(closed.get());

            data.session().saveGameResult(GameType.DINOSAUR_RUN, 180);
            var dinoResult = sent.getLast();
            assertEquals(GAME_RESULT_REQUEST, dinoResult.type());
            assertEquals(new GameResultRequest(GameType.DINOSAUR_RUN, 180),
                    codec.fromPayload(dinoResult.payload(), GameResultRequest.class));

            data.session().saveGameResult(GameType.CATCH_FAVORITE, -10);
            var clampedResult = sent.getLast();
            assertEquals(GAME_RESULT_REQUEST, clampedResult.type());
            assertEquals(new GameResultRequest(GameType.CATCH_FAVORITE, 0),
                    codec.fromPayload(clampedResult.payload(), GameResultRequest.class));

            dispatcher.dispatch(new MessageEnvelope(GAME_RESULT_SAVED, dinoResult.requestId(), null));
            gameView.onResultSaved();
            assertNotNull(findByAccessibleName(root.getLayeredPane(), "점수 저장 완료"));
        });
        SwingUtilities.invokeAndWait(() -> {
            gameViewRef.get().stopAllGames();
            NotificationCards.clear(gameRoot.get());
            live.close();
        });
    }

    private static boolean containsTextArea(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTextArea area && text.equals(area.getText())) return true;
            if (child instanceof Container nested && containsTextArea(nested, text)) return true;
        }
        return false;
    }

    private static JButton findButton(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton button && text.equals(button.getText())) return button;
            if (child instanceof Container nested) {
                JButton found = findButton(nested, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Component findByAccessibleName(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getAccessibleContext().getAccessibleName())) return child;
            if (child instanceof Container nested) {
                Component found = findByAccessibleName(nested, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T extends Component> T findComponent(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container nested) {
                T found = findComponent(nested, type);
                if (found != null) return found;
            }
        }
        return null;
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
                List.of(new ChatParticipantDto("me", "테스터", ProfileCharacter.EXAMPLE)), bubble.createdAt(), null, null);
    }
}
