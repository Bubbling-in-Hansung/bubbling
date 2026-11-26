package bubbling.client;

import bubbling.client.network.ClientMessageDispatcher;
import bubbling.client.ui.ApplicationView;
import bubbling.client.ui.dialog.UiErrors;
import bubbling.client.ui.model.UiSession;
import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.auth.LoginRequest;
import bubbling.common.dto.auth.LoginResponse;
import bubbling.common.dto.auth.RegisterRequest;
import bubbling.common.dto.auth.RegisterResponse;
import bubbling.common.dto.bubble.BubbleDto;
import bubbling.common.dto.bubble.BubbleReactionEvent;
import bubbling.common.dto.bubble.BubbleRemovedEvent;
import bubbling.common.dto.chat.ChatEndResponse;
import bubbling.common.dto.chat.ChatHistoryRequest;
import bubbling.common.dto.chat.ChatLeaveRequest;
import bubbling.common.dto.chat.ChatMemberCountEvent;
import bubbling.common.dto.chat.ChatMemberEvent;
import bubbling.common.dto.chat.ChatMessageDto;
import bubbling.common.dto.chat.ChatPositionDto;
import bubbling.common.dto.chat.ChatRequestDto;
import bubbling.common.dto.chat.ChatRoomDto;
import bubbling.common.dto.system.ExceptionResponse;
import bubbling.common.dto.system.InitialStateDto;
import bubbling.common.dto.system.OnlineStatusDto;
import bubbling.common.dto.user.UserDto;
import bubbling.common.dto.user.UserUpdateRequest;
import bubbling.common.enums.BubbleStatus;
import bubbling.common.enums.ChatRequestRole;
import bubbling.common.enums.ChatRequestStatus;
import bubbling.common.enums.ProfileCharacter;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import static bubbling.common.protocol.MessageType.*;

/** Owns the live application protocol. All state transitions run on the EDT. */
public final class LiveSession {
    private static final String LOGIN_ID_FORMAT = "[A-Za-z][A-Za-z0-9]{7,31}";
    private static final String PASSWORD_FORMAT = "(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]{8,64}";
    private static final String NICKNAME_FORMAT = "[가-힣A-Za-z0-9]+(?:\\s[가-힣A-Za-z0-9]+)*";
    private static final int AUTHENTICATION_TIMEOUT_MS = 10_000;
    private static final int PING_INTERVAL_MS = 15_000;
    private static final int PONG_TIMEOUT_MS = 5_000;
    private final Consumer<MessageEnvelope> transport;
    private final Runnable closeTransport;
    private final JsonCodec codec = new JsonCodec();
    private final Map<String, String> historyRequests = new HashMap<>();
    private final Map<String, String> leaveRequests = new HashMap<>();
    private final Timer authenticationTimeout;
    private final Timer pingTimer;
    private final Timer pongTimeout;
    private ApplicationView view;
    private UiSession state;
    private UserDto user;
    private boolean connected;
    private boolean disconnected;
    private boolean closed;
    private String authId;
    private String logoutId;
    private String pingId;
    private MessageType authResult;
    private String opening;

    public LiveSession(Consumer<MessageEnvelope> transport) {
        this(transport, () -> {});
    }

    public LiveSession(Consumer<MessageEnvelope> transport, Runnable closeTransport) {
        this.transport = Objects.requireNonNull(transport);
        this.closeTransport = Objects.requireNonNull(closeTransport);
        authenticationTimeout = new Timer(AUTHENTICATION_TIMEOUT_MS,
                event -> disconnected("인증 응답이 없습니다. 앱을 다시 실행해 주세요."));
        authenticationTimeout.setRepeats(false);
        pongTimeout = new Timer(PONG_TIMEOUT_MS, event -> onPongTimeout());
        pongTimeout.setRepeats(false);
        pingTimer = new Timer(PING_INTERVAL_MS, event -> sendPing());
        pingTimer.setInitialDelay(PING_INTERVAL_MS);
    }

    public void bind(ApplicationView view, UiSession state) {
        this.view = view;
        this.state = state;
    }

    public void registerHandlers(ClientMessageDispatcher dispatcher) {
        registerAuthHandlers(dispatcher);
        registerSystemAndUserHandlers(dispatcher);
        registerBubbleHandlers(dispatcher);
        registerChatHandlers(dispatcher);
    }

    private void registerAuthHandlers(ClientMessageDispatcher dispatcher) {
        on(dispatcher, LOGIN_RESULT, message -> {
            if (!authMatches(message)) return;
            signedIn(dispatcher.decode(message, LoginResponse.class).user());
        });
        on(dispatcher, REGISTER_RESULT, message -> {
            if (!authMatches(message)) return;
            signedIn(dispatcher.decode(message, RegisterResponse.class).user());
        });
        onAuthenticated(dispatcher, PASSWORD_CHANGED, message -> view.passwordChanged());
        on(dispatcher, LOGOUT_RESULT, message -> {
            if (logoutId == null || !logoutId.equals(message.requestId())) return;
            logoutId = null;
            user = null;
            opening = null;
            historyRequests.clear();
            leaveRequests.clear();
            view.resetLiveState();
            finishAuthentication("");
        });
        on(dispatcher, ERROR, message -> {
            ExceptionResponse response = dispatcher.decodeError(message);
            String detail = response.message() == null ? "서버에서 요청을 처리하지 못했습니다." : response.message();
            System.err.println("[ERROR] " + detail);
            if (authId != null && authId.equals(message.requestId())) {
                finishAuthentication(detail);
            } else {
                clearFailedRequest(message);
                UiErrors.show(view, new ExceptionResponse(response.code(), detail));
            }
        });
    }

    private void registerSystemAndUserHandlers(ClientMessageDispatcher dispatcher) {
        on(dispatcher, PONG, message -> {
            if (pingId != null && pingId.equals(message.requestId())) {
                pingId = null;
                pongTimeout.stop();
            }
        });
        onAuthenticated(dispatcher, INITIAL_STATE, InitialStateDto.class, snapshot -> {
            state.applyInitialState(snapshot);
            view.setOnline(snapshot.onlineUserCnt());
            view.refreshLiveState();
        });
        on(dispatcher, ONLINE_STATUS, message ->
                view.setOnline(dispatcher.decode(message, OnlineStatusDto.class).onlineUserCnt()));
        onAuthenticated(dispatcher, USER_INFO, UserDto.class, this::applyAccount);
        onAuthenticated(dispatcher, USER_UPDATED, UserDto.class, this::applyAccount);
    }

    private void registerBubbleHandlers(ClientMessageDispatcher dispatcher) {
        onAuthenticated(dispatcher, BUBBLE_CREATED, message -> refreshRooms());
        onAuthenticated(dispatcher, BUBBLE_UPDATED, BubbleDto.class, bubble -> {
            state.applyBubble(bubble);
            view.refreshLiveState();
            refreshRooms();
        });
        onAuthenticated(dispatcher, BUBBLE_REACTED, BubbleReactionEvent.class, reaction -> {
            state.updateReactionCount(reaction.bubbleId(), reaction.reactionCount());
            view.refreshLiveState();
        });
        onAuthenticated(dispatcher, BUBBLE_REMOVED, BubbleRemovedEvent.class, event -> {
            state.markBubbleRemoved(event);
            if (event.status() == BubbleStatus.DELETED) {
                view.returnFromChat(event.bubbleId());
            }
            view.refreshLiveState();
            refreshRooms();
        });
    }

    private void registerChatHandlers(ClientMessageDispatcher dispatcher) {
        onAuthenticated(dispatcher, MY_CHAT_ROOM_LIST, ChatRoomDto[].class, rooms -> {
            state.applyChatRooms(rooms);
            view.refreshLiveState();
        });
        onAuthenticated(dispatcher, CHAT_JOINED, message ->
                openRoom(message.requestId(), dispatcher.decode(message, ChatRoomDto.class)));
        onAuthenticated(dispatcher, CHAT_LEFT, message -> {
            String bubbleId = leaveRequests.remove(message.requestId());
            if (bubbleId == null) return;
            state.markLeft(bubbleId);
            view.returnFromChat(bubbleId);
            refreshRooms();
        });
        onAuthenticated(dispatcher, CHAT_ENDED, ChatEndResponse.class, response -> {
            state.markEnded(response);
            view.returnFromChat(response.bubbleId());
            view.refreshLiveState();
            refreshRooms();
        });
        onAuthenticated(dispatcher, CHAT_MEMBER_JOINED, ChatMemberEvent.class, event -> {
            UiSession.Room room = state.chatRoom(event.chatRoomId());
            if (room != null && !room.view.privateRoom()) {
                view.memberJoined(event.participant(), room);
                view.refreshLiveState();
            }
            refreshRooms();
        });
        onAuthenticated(dispatcher, CHAT_MEMBER_LEFT, message -> refreshRooms());
        onAuthenticated(dispatcher, CHAT_MEMBER_COUNT_CHANGED, ChatMemberCountEvent.class, count -> {
            state.updateMemberCount(count.bubbleId(), count.currentMemberCnt());
            view.refreshLiveState();
        });
        for (MessageType type : new MessageType[] {CHAT_REQUESTED, CHAT_ACCEPTED, CHAT_REJECTED}) {
            onAuthenticated(dispatcher, type, ChatRequestDto.class, request -> {
                boolean added = state.applyRequest(request);
                if (added && type == CHAT_REQUESTED && request.role() == ChatRequestRole.OWNER
                        && request.status() == ChatRequestStatus.PENDING) {
                    view.requestReceived(request.bubbleId());
                }
                view.refreshLiveState();
            });
        }
        onAuthenticated(dispatcher, CHAT_REQUEST_LIST, ChatRequestDto[].class, list -> {
            int added = state.applyRequestList(list);
            if (added > 0) {
                view.requestsLoaded(added);
            }
            view.refreshLiveState();
        });
        onAuthenticated(dispatcher, CHAT_HISTORY, message -> {
            String roomId = historyRequests.remove(message.requestId());
            if (roomId == null) return;
            state.applyHistory(roomId, dispatcher.decode(message, ChatMessageDto[].class));
            view.refreshLiveState();
        });
        onAuthenticated(dispatcher, CHAT_MESSAGE, message -> {
            var room = state.applyMessage(dispatcher.decode(message, ChatMessageDto.class));
            if (room != null && message.requestId() == null) {
                view.chatMessageReceived(room);
            }
            view.refreshLiveState();
        });
        onAuthenticated(dispatcher, CHAT_POSITION_CHANGED, message -> {});
        onAuthenticated(dispatcher, CHAT_POSITION_EVENT, ChatPositionDto.class,
                position -> view.characterMoved(position));
    }

    private void onAuthenticated(ClientMessageDispatcher dispatcher, MessageType type,
            ClientMessageDispatcher.Command handler) {
        on(dispatcher, type, message -> {
            if (user == null) return;
            handler.execute(message);
        });
    }

    private <T> void onAuthenticated(ClientMessageDispatcher dispatcher, MessageType type,
            Class<T> payloadType, Consumer<T> handler) {
        onAuthenticated(dispatcher, type, message -> handler.accept(dispatcher.decode(message, payloadType)));
    }

    private void on(ClientMessageDispatcher dispatcher, MessageType type, ClientMessageDispatcher.Command handler) {
        dispatcher.register(type, message -> SwingUtilities.invokeLater(() -> {
            if (closed || !connected && type != ONLINE_STATUS) return;
            try {
                handler.execute(message);
            } catch (RuntimeException e) {
                dispatcher.reportFailure(type, e);
                if (authId != null && authId.equals(message.requestId())) {
                    disconnected("서버 인증 응답을 처리하지 못했습니다. 앱을 다시 실행해 주세요.");
                }
                clearFailedRequest(message);
            }
        }));
    }

    public void connected() {
        if (closed) return;
        connected = true;
        disconnected = false;
        stopPing();
        pingTimer.restart();
        view.authentication(true, "");
    }

    public void disconnected(String detail) {
        if (closed || disconnected) return;
        disconnected = true;
        connected = false;
        stopPing();
        user = null;
        logoutId = null;
        opening = null;
        historyRequests.clear();
        leaveRequests.clear();
        view.resetLiveState();
        finishAuthentication(detail);
    }

    public void authenticate(boolean signup, String loginId, String password, String nickname) {
        if (!connected || authId != null || user != null) return;
        if (loginId.isBlank() || password.isEmpty()) {
            view.authentication(true, "아이디와 비밀번호를 입력해 주세요.");
            return;
        }
        if (signup) {
            nickname = nickname.strip();
            if (!loginId.matches(LOGIN_ID_FORMAT)) {
                view.authentication(true, "아이디는 영문으로 시작하고 영문과 숫자 8~32자로 입력해 주세요.");
                return;
            }
            if (!password.matches(PASSWORD_FORMAT)) {
                view.authentication(true, "비밀번호 8~64자에 영문·숫자·특수문자를 포함해 주세요.");
                return;
            }
            if (nickname.length() < 2 || nickname.length() > 32 || !nickname.matches(NICKNAME_FORMAT)) {
                view.authentication(true, "닉네임은 한글·영문·숫자 2~32자로 입력해 주세요.");
                return;
            }
        }
        authId = UUID.randomUUID().toString();
        authResult = signup ? REGISTER_RESULT : LOGIN_RESULT;
        view.authentication(false, "서버에 확인 중입니다…");
        authenticationTimeout.start();
        transmit(signup ? REGISTER : LOGIN, authId, signup
                ? new RegisterRequest(loginId, password, nickname)
                : new LoginRequest(loginId, password));
    }

    private boolean authMatches(MessageEnvelope message) {
        return authId != null && authId.equals(message.requestId()) && authResult == message.type();
    }

    private void finishAuthentication(String status) {
        authenticationTimeout.stop();
        authId = null;
        authResult = null;
        view.authentication(connected, status);
    }

    private void signedIn(UserDto account) {
        user = Objects.requireNonNull(account);
        finishAuthentication("");
        view.signedIn(user);
        send(CHAT_REQUEST_LIST_REQUEST, null);
        refreshRooms();
    }

    public void logout() {
        if (!connected || user == null || logoutId != null) return;
        logoutId = UUID.randomUUID().toString();
        transmit(LOGOUT, logoutId, null);
    }

    private void applyAccount(UserDto account) {
        user = Objects.requireNonNull(account);
        view.updateAccount(user);
    }

    public void requestAccount() {
        send(USER_INFO_REQUEST, null);
    }

    public void requestChatRooms() {
        send(MY_CHAT_ROOM_LIST_REQUEST, null);
    }

    public void updateProfile(ProfileCharacter profile) {
        if (user != null) {
            send(USER_UPDATE, new UserUpdateRequest(user.nickname(), profile));
        }
    }

    public void send(MessageType type, Object payload) {
        if (!connected || user == null) return;
        String id = UUID.randomUUID().toString();
        if (type == CHAT_JOIN) {
            opening = id;
        }
        if (type == CHAT_LEAVE && payload instanceof ChatLeaveRequest request) {
            leaveRequests.put(id, request.bubbleId());
        }
        transmit(type, id, payload);
    }

    public void requestHistory(String roomId) {
        if (!connected || user == null || historyRequests.containsValue(roomId)) return;
        String id = UUID.randomUUID().toString();
        historyRequests.put(id, roomId);
        transmit(CHAT_HISTORY_REQUEST, id, new ChatHistoryRequest(roomId));
    }

    public void close() {
        closed = true;
        connected = false;
        stopPing();
        logoutId = null;
        leaveRequests.clear();
        authenticationTimeout.stop();
    }

    void sendPing() {
        if (closed || !connected || pingId != null) return;
        String id = UUID.randomUUID().toString();
        pingId = id;
        transmit(PING, id, null);
        if (connected && id.equals(pingId)) {
            pongTimeout.restart();
        }
    }

    void onPongTimeout() {
        if (closed || !connected || pingId == null) return;
        closeTransport.run();
        disconnected("서버 응답이 없습니다. 앱을 다시 실행해 주세요.");
    }

    boolean isPingScheduled() {
        return pingTimer.isRunning();
    }

    boolean isAwaitingPong() {
        return pongTimeout.isRunning() && pingId != null;
    }

    private void stopPing() {
        pingId = null;
        pingTimer.stop();
        pongTimeout.stop();
    }

    private void clearFailedRequest(MessageEnvelope message) {
        if (logoutId != null && logoutId.equals(message.requestId())) logoutId = null;
        if (opening != null && opening.equals(message.requestId())) opening = null;
        historyRequests.remove(message.requestId());
        leaveRequests.remove(message.requestId());
    }

    private void transmit(MessageType type, String id, Object payload) {
        try {
            transport.accept(new MessageEnvelope(type, id, codec.toPayload(payload)));
        } catch (IllegalStateException e) {
            disconnected("서버 연결이 끊겼습니다. 앱을 다시 실행해 주세요.");
        }
    }

    private void refreshRooms() {
        send(INITIAL_STATE_REQUEST, null);
        send(MY_CHAT_ROOM_LIST_REQUEST, null);
    }

    private void openRoom(String requestId, ChatRoomDto chat) {
        UiSession.Room room = state.applyChatRoom(chat);
        boolean requested = opening != null && opening.equals(requestId);
        if (requested) opening = null;
        if (requested) {
            view.openLiveChat(room);
            requestHistory(chat.chatRoomId());
        } else if (requestId == null && room.joined) {
            state.markOutgoingAccepted(room);
            view.requestApproved(chat.myMemberId(), room);
        }
        view.refreshLiveState();
    }
}
