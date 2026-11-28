package bubbling.client.ui.model;

import bubbling.client.service.SessionRequests;
import bubbling.common.enums.ChatRequestStatus;
import bubbling.common.enums.ChatRequestRole;
import bubbling.common.dto.bubble.BubbleDto;
import bubbling.common.dto.bubble.BubbleRemovedEvent;
import bubbling.common.dto.chat.*;
import bubbling.common.dto.game.GameRankingData;
import bubbling.common.dto.game.GameRankingResponse;
import bubbling.common.dto.system.InitialStateDto;
import bubbling.common.dto.user.UserDto;
import bubbling.common.enums.BubbleStatus;
import bubbling.common.enums.ChatRoomStatus;
import bubbling.common.enums.GameRankingType;
import bubbling.common.enums.GameType;
import bubbling.common.enums.ProfileCharacter;
import bubbling.common.protocol.MessageType;
import java.time.*;
import java.util.*;
import java.util.function.BiConsumer;

/** Presentation state populated by server responses and events. */
public final class UiSession {
    private static final Duration RETRY_DELAY = Duration.ofSeconds(60);
    private static final Duration SPEECH_LIFETIME = Duration.ofSeconds(7);
    private static final int MAX_SPEECH_BUBBLES = 3;
    private static final Duration ACTIVITY_WINDOW = Duration.ofSeconds(60);
    public static final class Room {
        public final String id;
        public final String bubbleId;
        public RoomView view;
        public int people;
        public int reactionCount;
        public boolean joined;
        public boolean ended;
        public boolean removed;
        public boolean deleted;
        public boolean visited;
        public boolean listed;
        public Instant expiresAt;
        public ChatRoomDto conversation;
        private String myMemberId;
        public int historyRevision;
        private final List<Message> messageLog = new ArrayList<>();
        public final List<Message> messages = Collections.unmodifiableList(messageLog);
        private final Map<String, ChatMessageDto> received = new LinkedHashMap<>();
        private final Map<String, Deque<Message>> recentSpeechByMember = new HashMap<>();
        private final Set<String> typingMembers = new HashSet<>();
        private Instant lastActivityAt;
        Room(String id, RoomView view) {
            this.id = id;
            this.bubbleId = id;
            this.view = view;
            people = view.people();
        }

        private void recordMessage(Message view) {
            messageLog.add(view);
            cacheMessageMetadata(view);
        }

        private void rebuildMessageCaches() {
            recentSpeechByMember.clear();
            lastActivityAt = null;
            for (Message view : messageLog) {
                cacheMessageMetadata(view);
            }
        }

        private void cacheMessageMetadata(Message view) {
            if (view.at() == null) return;
            if (lastActivityAt == null || view.at().isAfter(lastActivityAt)) {
                lastActivityAt = view.at();
            }
            if (view.memberId() != null) {
                Deque<Message> deque = recentSpeechByMember.computeIfAbsent(
                        view.memberId(), key -> new ArrayDeque<>(MAX_SPEECH_BUBBLES));
                deque.addLast(view);
                if (deque.size() > MAX_SPEECH_BUBBLES) {
                    deque.removeFirst();
                }
            }
        }

        public List<ChatParticipantDto> participants() {
            return conversation == null || conversation.participants() == null ? List.of() : conversation.participants();
        }

        public int bubbleDiameter() {
            if (reactionCount >= 20) return view.size() + 48;
            if (reactionCount >= 10) return view.size() + 32;
            if (reactionCount >= 5) return view.size() + 16;
            return view.size();
        }
    }

    public record Message(String sender, String text, boolean mine, Instant at, String memberId) {}

    public static final class Request {
        public final String id;
        public final String roomId;
        public final String name;
        public final boolean incoming;
        public ChatRequestStatus status = ChatRequestStatus.PENDING;
        Request(String id, String roomId, String name, boolean incoming) {
            this.id = id;
            this.roomId = roomId;
            this.name = name;
            this.incoming = incoming;
        }
    }

    private final SessionRequests liveRequests;
    private final Clock clock;
    private final LinkedHashMap<String, Room> rooms = new LinkedHashMap<>();
    private final List<Request> requests = new ArrayList<>();
    private final Map<String, Instant> retry = new HashMap<>();
    private final Set<String> rejected = new HashSet<>();
    private final Set<String> ownedBubbles = new HashSet<>();
    private final EnumMap<GameType, Integer> bestScores = new EnumMap<>(GameType.class);
    private String currentUserId;
    private String recent;
    private int character;
    public UiSession(Clock clock, BiConsumer<MessageType, Object> transport) {
        this.clock = clock;
        liveRequests = new SessionRequests(Objects.requireNonNull(transport));
    }

    public void applyInitialState(InitialStateDto snapshot) {
        Set<String> visible = new HashSet<>();
        for (BubbleDto bubble : snapshot.bubbles()) {
            visible.add(bubble.bubbleId());
            applyBubble(bubble);
        }
        rooms.values().stream().filter(room -> !visible.contains(room.bubbleId)).forEach(room -> room.removed = true);
    }

    public Room applyBubble(BubbleDto bubble) {
        String bubbleId = bubble.bubbleId();
        int hash = bubbleId.hashCode();
        var view = new RoomView(bubble.title(), bubble.category().getLabel(),
                ConversationMode.fromMethod(bubble.method()).label(), bubble.currentMemberCnt(),
                bubble.maxMemberCnt(), !bubble.isOpen(), bubble.status() == BubbleStatus.EXPIRED,
                ownedBubbles.contains(bubbleId), Math.floorMod(hash, CharacterChoice.count()),
                .15 + Math.floorMod(hash, 100) / 150.0, .15 + Math.floorMod(hash >>> 8, 100) / 150.0, 152);
        var room = rooms.computeIfAbsent(bubbleId, id -> new Room(id, view));
        if (!room.view.equals(view)) room.view = view;
        room.expiresAt = timestamp(bubble.expiresAt());
        room.people = bubble.currentMemberCnt();
        room.reactionCount = Math.max(room.reactionCount, bubble.reactionCount());
        room.removed = bubble.status() != BubbleStatus.ALIVE;
        room.deleted = bubble.status() == BubbleStatus.DELETED;
        return room;
    }

    public Room applyChatRoom(ChatRoomDto chat) {
        if (chat.isOwner()) ownedBubbles.add(chat.bubble().bubbleId());
        var room = applyBubble(chat.bubble());
        if (chat.myMemberId() != null && !chat.myMemberId().equals(room.myMemberId)) {
            room.myMemberId = chat.myMemberId();
            rebuildHistory(room);
        }
        room.conversation = chat;
        room.listed = true;
        room.joined = chat.status() == ChatRoomStatus.ACTIVE && chat.myMemberId() != null;
        room.ended = chat.status() == ChatRoomStatus.ENDED;
        if (room.ended) {
            room.typingMembers.clear();
        } else if (chat.participants() != null) {
            Set<String> activeIds = new HashSet<>();
            for (ChatParticipantDto participant : chat.participants()) {
                activeIds.add(participant.chatMemberId());
            }
            room.typingMembers.retainAll(activeIds);
        }
        return room;
    }

    public void applyChatRooms(ChatRoomDto[] conversations) {
        rooms.values().forEach(room -> room.listed = false);
        for (ChatRoomDto conversation : conversations) applyChatRoom(conversation);
        rooms.values().stream().filter(room -> !room.listed).forEach(room -> {
            room.joined = false;
            room.visited = false;
        });
    }

    public void markLeft(String id) {
        Room room = rooms.get(id);
        if (room != null) {
            room.joined = false;
            room.listed = false;
            room.visited = false;
        }
    }

    public void markEnded(ChatEndResponse response) {
        for (Request request : requests) {
            if (request.roomId.equals(response.bubbleId()) && request.status == ChatRequestStatus.PENDING) {
                request.status = ChatRequestStatus.CANCELLED;
            }
        }
        Room room = chatRoom(response.chatRoomId());
        if (room == null) return;
        room.ended = true;
        room.joined = false;
        room.people = 0;
        room.typingMembers.clear();
    }

    public void markBubbleRemoved(BubbleRemovedEvent event) {
        if (event.status() == BubbleStatus.EXPIRED) {
            for (Request request : requests) {
                if (request.roomId.equals(event.bubbleId()) && request.status == ChatRequestStatus.PENDING) {
                    request.status = ChatRequestStatus.CANCELLED;
                }
            }
        }
        Room room = rooms.get(event.bubbleId());
        if (room == null) return;
        room.removed = true;
        if (event.status() == BubbleStatus.DELETED) {
            ownedBubbles.remove(event.bubbleId());
            room.deleted = true;
            room.joined = false;
            room.listed = false;
            room.visited = false;
            room.ended = room.conversation != null;
            if (room.id.equals(recent)) recent = null;
        }
    }

    public void updateMemberCount(String bubbleId, int count) {
        Room room = rooms.get(bubbleId);
        if (room != null) room.people = count;
    }

    public void updateReactionCount(String bubbleId, int count) {
        Room room = rooms.get(bubbleId);
        if (room != null) room.reactionCount = Math.max(room.reactionCount, count);
    }

    public boolean applyRequest(ChatRequestDto response) {
        boolean incoming = response.role() == ChatRequestRole.OWNER;
        var request = requests.stream().filter(item -> item.id.equals(response.chatRequestId())).findFirst().orElse(null);
        boolean added = request == null;
        if (added) {
            request = new Request(response.chatRequestId(), response.bubbleId(),
                    Objects.toString(response.requesterNickname(), "참여 요청"), incoming);
            requests.add(request);
        }
        if (response.status() == ChatRequestStatus.REJECTED) {
            reject(request);
        } else {
            request.status = response.status();
        }
        return added;
    }

    public int applyRequestList(ChatRequestDto[] list) {
        if (list == null) return 0;
        int addedIncomingPending = 0;
        for (ChatRequestDto dto : list) {
            if (dto == null || dto.chatRequestId() == null) continue;
            var existing = requests.stream()
                    .filter(item -> item.id.equals(dto.chatRequestId()))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                if (existing.status == ChatRequestStatus.PENDING && dto.status() != null) {
                    if (dto.status() == ChatRequestStatus.REJECTED) {
                        reject(existing);
                    } else {
                        existing.status = dto.status();
                    }
                }
                continue;
            }
            Room targetRoom = rooms.get(dto.bubbleId());
            if (targetRoom != null && (targetRoom.ended || targetRoom.removed)) {
                continue;
            }
            if (applyRequest(dto) && dto.role() == ChatRequestRole.OWNER
                    && dto.status() == ChatRequestStatus.PENDING) {
                addedIncomingPending++;
            }
        }
        return addedIncomingPending;
    }

    public void applyHistory(String roomId, ChatMessageDto[] history) {
        Room room = chatRoom(roomId);
        if (room == null) return;
        Map<String, ChatMessageDto> ordered = new LinkedHashMap<>();
        for (ChatMessageDto message : history) {
            if (roomId.equals(message.chatRoomId())) ordered.putIfAbsent(message.chatMessageId(), message);
        }
        room.received.forEach(ordered::putIfAbsent);
        room.received.clear();
        room.received.putAll(ordered);
        rebuildHistory(room);
    }

    public Room applyMessage(ChatMessageDto message) {
        Room room = chatRoom(message.chatRoomId());
        if (room == null || room.received.putIfAbsent(message.chatMessageId(), message) != null) return null;
        if (message.senderMemberId() != null) {
            room.typingMembers.remove(message.senderMemberId());
        }
        room.recordMessage(messageView(room, message));
        return room;
    }

    public void applyTyping(ChatTypingDto event) {
        if (event == null || event.chatRoomId() == null || event.chatMemberId() == null) return;
        Room room = chatRoom(event.chatRoomId());
        if (room == null || room.ended || event.chatMemberId().equals(room.myMemberId)) return;
        if (event.typing()) {
            room.typingMembers.add(event.chatMemberId());
        } else {
            room.typingMembers.remove(event.chatMemberId());
        }
    }

    public Room chatRoom(String chatId) {
        if (chatId == null) return null;
        return rooms.values().stream().filter(room -> room.conversation != null && chatId.equals(room.conversation.chatRoomId()))
                .findFirst().orElse(null);
    }

    private void rebuildHistory(Room room) {
        var messages = room.received.values().stream().map(message -> messageView(room, message)).toList();
        if (room.messageLog.equals(messages)) return;
        if (messages.size() < room.messageLog.size() || !messages.subList(0, room.messageLog.size()).equals(room.messageLog)) {
            room.historyRevision++;
            room.messageLog.clear();
        }
        room.messageLog.addAll(messages.subList(room.messageLog.size(), messages.size()));
        room.rebuildMessageCaches();
    }

    private Message messageView(Room room, ChatMessageDto message) {
        return new Message(message.senderNickname(), message.content(),
                room.myMemberId != null && room.myMemberId.equals(message.senderMemberId()),
                timestamp(message.createdAt()), message.senderMemberId());
    }

    private Instant timestamp(String raw) {
        if (raw == null) return null;
        try {
            return LocalDateTime.parse(raw).atZone(clock.getZone()).toInstant();
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    public void resetLiveState() {
        rooms.clear();
        ownedBubbles.clear();
        requests.clear();
        rejected.clear();
        retry.clear();
        bestScores.clear();
        currentUserId = null;
        recent = null;
        character = 0;
    }

    public Instant now() {
        return clock.instant();
    }

    public List<Room> rooms() {
        return List.copyOf(rooms.values());
    }

    public Room room(String id) {
        return rooms.get(id);
    }

    public Room find(RoomView view) {
        if (view == null) return null;
        return rooms.values().stream().filter(r -> r.view == view).findFirst()
                .or(() -> rooms.values().stream().filter(r -> view.equals(r.view)).findFirst())
                .orElse(null);
    }

    public boolean expired(Room r) {
        return !r.ended && (r.view.expired() || r.removed
                || r.expiresAt != null && !r.expiresAt.isAfter(now()));
    }

    public boolean hidden(Room r) {
        return r.removed || r.ended || r.expiresAt != null && !r.expiresAt.isAfter(now()) || r.people >= r.view.capacity();
    }

    public boolean canReact(Room room) {
        return !room.view.owner() && !room.removed
                && (room.expiresAt == null || room.expiresAt.isAfter(now()));
    }

    public void react(Room room) {
        if (canReact(room)) liveRequests.reactToBubble(room.bubbleId);
    }

    public void requestDetail(Room room) {
        if (room != null && !room.deleted) liveRequests.requestBubbleDetail(room.bubbleId);
    }

    public List<Request> requests() {
        return List.copyOf(requests);
    }

    public Request outgoing(Room r) {
        return requests.stream().filter(q -> !q.incoming && q.roomId.equals(r.id)).reduce((a, b) -> b).orElse(null);
    }

    public long retrySeconds(Room r) {
        var at = retry.get(r.bubbleId);
        return at == null ? 0 : Math.max(0, (Duration.between(now(), at).toMillis() + 999) / 1000);
    }

    public void request(Room r) {
        if (hidden(r) || retrySeconds(r) > 0) {
            throw new IllegalStateException("지금은 참여 요청을 보낼 수 없어요.");
        }
        var old = outgoing(r);
        if (old != null && old.status == ChatRequestStatus.PENDING) {
            return;
        }
        liveRequests.requestChat(r.bubbleId);
    }

    public void requestIncomingRequests() {
        liveRequests.requestChatRequests();
    }

    public void decide(Request q, boolean accept) {
        if (q.status != ChatRequestStatus.PENDING) {
            return;
        }
        Room r = room(q.roomId);
        if (accept && (r == null || hidden(r))) {
            throw new IllegalStateException("지금은 이 방의 요청을 승인할 수 없어요.");
        }
        liveRequests.decideChat(q.id, accept);
    }

    public void markOutgoingAccepted(Room room) {
        Request request = outgoing(room);
        if (request != null && request.status == ChatRequestStatus.PENDING) {
            request.status = ChatRequestStatus.ACCEPTED;
        }
    }

    private void reject(Request q) {
        q.status = ChatRequestStatus.REJECTED;
        if (!q.incoming && rejected.add(q.id)) {
            retry.put(q.roomId, now().plus(RETRY_DELAY));
        }
    }

    public boolean canOpen(Room r) {
        return r.ended || r.joined || !hidden(r) && (!r.view.privateRoom() || r.view.owner());
    }

    public void openLiveRoom(Room room) {
        if (!canOpen(room)) return;
        liveRequests.joinChat(room.bubbleId);
    }

    public void visit(Room r) {
        r.visited = true;
        if (r.view.owner()) {
            recent = r.id;
        }
    }

    public Room recent() {
        return recent == null ? null : rooms.get(recent);
    }

    public void leave(Room r) {
        liveRequests.leaveChat(r.bubbleId);
    }

    public void end(Room r) {
        liveRequests.endChat(r.conversation.chatRoomId());
    }

    public void send(Room r, String text) {
        liveRequests.sendMessage(r.conversation.chatRoomId(), text);
    }

    public void moveCharacter(Room r, int x, int y) {
        if (r == null || r.conversation == null || r.conversation.myMemberId() == null) return;
        liveRequests.moveCharacter(r.conversation.chatRoomId(), r.conversation.myMemberId(), x, y);
    }

    public void setTyping(Room r, boolean typing) {
        if (r == null || r.ended || !r.joined || r.conversation == null || r.conversation.myMemberId() == null) {
            return;
        }
        liveRequests.sendTyping(r.conversation.chatRoomId(), r.conversation.myMemberId(), typing);
    }

    public boolean isTyping(Room r, String memberId) {
        return r != null && !r.ended && memberId != null && r.typingMembers.contains(memberId);
    }

    public List<Message> speech(Room r, String memberId) {
        if (r == null || memberId == null) return List.of();
        Deque<Message> recent = r.recentSpeechByMember.get(memberId);
        if (recent == null || recent.isEmpty()) return List.of();
        Instant currentNow = now();
        List<Message> active = new ArrayList<>(recent.size());
        for (Message message : recent) {
            if (message.at() != null && message.at().plus(SPEECH_LIFETIME).isAfter(currentNow)) {
                active.add(message);
            }
        }
        return List.copyOf(active);
    }

    public boolean hasRecentActivity(Room r) {
        if (r == null || r.lastActivityAt == null) return false;
        Instant currentNow = now();
        return !r.lastActivityAt.isAfter(currentNow)
                && !r.lastActivityAt.isBefore(currentNow.minus(ACTIVITY_WINDOW));
    }

    public int character() {
        return character;
    }

    public String currentUserId() {
        return currentUserId;
    }

    public void applyAccount(UserDto user) {
        currentUserId = user == null ? null : user.id();
        applyProfile(user == null ? null : user.profile());
    }

    public void applyProfile(ProfileCharacter profile) {
        character = CharacterChoice.indexOf(profile);
    }

    public int bestScore(GameType gameType) {
        return gameType == null ? 0 : bestScores.getOrDefault(gameType, 0);
    }

    public void recordLocalScore(GameType gameType, int score) {
        if (gameType == null) return;
        bestScores.merge(gameType, Math.max(0, score), Math::max);
    }

    public void applyGameRanking(GameType gameType, GameRankingResponse response) {
        if (response == null || response.rankingType() != GameRankingType.MAX
                || gameType == null || currentUserId == null || response.rankings() == null) {
            return;
        }
        for (GameRankingData entry : response.rankings()) {
            if (entry != null && entry.user() != null && currentUserId.equals(entry.user().id())) {
                bestScores.merge(gameType, Math.max(0, entry.score()), Math::max);
                break;
            }
        }
    }

    public void create(RoomView view, int minutes) {
        liveRequests.createBubble(view, minutes);
    }

    public void update(Room r, RoomView view, int minutes) {
        liveRequests.updateBubble(r.bubbleId, view, minutes);
    }

    public void remove(Room r) {
        liveRequests.deleteBubble(r.bubbleId);
    }

    public void changePassword(String currentPassword, String newPassword) {
        liveRequests.changePassword(currentPassword, newPassword);
    }

    public void saveGameResult(GameType gameType, int score) {
        liveRequests.saveGameResult(gameType, score);
    }

    public void requestGameRanking(GameRankingType rankingType, GameType gameType, int limit) {
        liveRequests.requestGameRanking(rankingType, gameType, limit);
    }
}
