package bubbling.client.ui.model;

import bubbling.client.service.SessionRequests;
import bubbling.common.enums.ChatRequestStatus;
import bubbling.common.enums.ChatRequestRole;
import bubbling.common.dto.bubble.BubbleDto;
import bubbling.common.dto.bubble.BubbleRemovedEvent;
import bubbling.common.dto.chat.*;
import bubbling.common.dto.system.InitialStateDto;
import bubbling.common.enums.BubbleStatus;
import bubbling.common.enums.ChatRoomStatus;
import bubbling.common.enums.ProfileCharacter;
import bubbling.common.protocol.MessageType;
import java.time.*;
import java.util.*;
import java.util.function.BiConsumer;

/** Presentation state used by the memory preview and live screens. */
public final class UiSession {
    private static final Duration RETRY_DELAY = Duration.ofSeconds(60);
    public static final class Room {
        public final String id;
        public final String bubbleId;
        public RoomView view;
        public int people;
        public boolean joined;
        public boolean ended;
        public boolean removed;
        public boolean visited;
        public boolean listed;
        public Instant expiresAt;
        private int previewExpirationMinutes = 30;
        public ChatRoomDto conversation;
        private String myMemberId;
        public int historyRevision;
        public final List<Message> messages = new ArrayList<>();
        private final Map<String, ChatMessageDto> received = new LinkedHashMap<>();
        Room(String id, RoomView view, Instant now) {
            this.id = id;
            this.bubbleId = id;
            this.view = view;
            people = view.people();
            joined = view.owner();
            expiresAt = view.expired() ? now : now.plusSeconds(previewExpirationMinutes * 60L);
        }

        public List<ChatParticipantDto> participants() {
            return conversation == null || conversation.participants() == null ? List.of() : conversation.participants();
        }
    }

    public record Message(String sender, String text, boolean mine, Instant at, String memberId) {
        public Message(String sender, String text, boolean mine, Instant at) {
            this(sender, text, mine, at, null);
        }
    }

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

    private final boolean preview;
    private final SessionRequests liveRequests;
    private final Clock clock;
    private final LinkedHashMap<String, Room> rooms = new LinkedHashMap<>();
    private final List<Request> requests = new ArrayList<>();
    private final Map<String, Instant> retry = new HashMap<>();
    private final Set<String> rejected = new HashSet<>();
    private final Set<String> ownedBubbles = new HashSet<>();
    private String recent;
    private int character;
    public UiSession(boolean preview, List<RoomView> views) {
        this(preview, views, Clock.systemUTC());
    }

    public UiSession(boolean preview, List<RoomView> views, Clock clock) {
        this(preview, views, clock, null);
    }

    UiSession(boolean preview, List<RoomView> views, Clock clock, BiConsumer<MessageType, Object> transport) {
        this.preview = preview;
        this.clock = clock;
        liveRequests = transport == null ? null : new SessionRequests(transport);
        int i = 0;
        for (var v : views) {
            var room = new Room("preview-" + i++, v, clock.instant());
            rooms.put(room.id, room);
            if (preview && v.owner()) {
                requests.add(new Request(UUID.randomUUID().toString(), room.id, "달빛 구름", true));
                requests.add(new Request(UUID.randomUUID().toString(), room.id, "작은 여우", true));
            }
        }
    }

    public boolean preview() {
        return preview;
    }

    public boolean live() {
        return liveRequests != null;
    }

    public boolean interactive() {
        return preview || live();
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
                ConversationMode.valueOf(bubble.method().name()).label(), bubble.currentMemberCnt(),
                bubble.maxMemberCnt(), !bubble.isOpen(), bubble.status() == BubbleStatus.EXPIRED,
                ownedBubbles.contains(bubbleId), Math.floorMod(hash, CharacterChoice.count()),
                .15 + Math.floorMod(hash, 100) / 150.0, .15 + Math.floorMod(hash >>> 8, 100) / 150.0, 152);
        var room = rooms.computeIfAbsent(bubbleId, id -> new Room(id, view, now()));
        if (!room.view.equals(view)) room.view = view;
        room.expiresAt = timestamp(bubble.expiresAt());
        room.people = bubble.currentMemberCnt();
        room.removed = bubble.status() != BubbleStatus.ALIVE;
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
        return room;
    }

    public void applyChatRooms(ChatRoomDto[] conversations) {
        rooms.values().forEach(room -> room.listed = false);
        for (ChatRoomDto conversation : conversations) applyChatRoom(conversation);
        rooms.values().stream().filter(room -> room.conversation != null && !room.listed)
                .forEach(room -> room.joined = false);
    }

    public void markLeft(String id) {
        Room room = rooms.get(id);
        if (room != null) {
            room.joined = false;
            room.listed = false;
        }
    }

    public void markEnded(ChatEndResponse response) {
        Room room = chatRoom(response.chatRoomId());
        if (room == null) return;
        room.ended = true;
        room.joined = false;
        room.people = 0;
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
            room.joined = false;
            room.listed = false;
            room.ended = room.conversation != null;
            if (room.id.equals(recent)) recent = null;
        }
    }

    public void updateMemberCount(String bubbleId, int count) {
        Room room = rooms.get(bubbleId);
        if (room != null) room.people = count;
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
        room.messages.add(messageView(room, message));
        return room;
    }

    private Room chatRoom(String chatId) {
        return rooms.values().stream().filter(room -> room.conversation != null && room.conversation.chatRoomId().equals(chatId))
                .findFirst().orElse(null);
    }

    private void rebuildHistory(Room room) {
        var messages = room.received.values().stream().map(message -> messageView(room, message)).toList();
        if (room.messages.equals(messages)) return;
        if (messages.size() < room.messages.size() || !messages.subList(0, room.messages.size()).equals(room.messages)) {
            room.historyRevision++;
            room.messages.clear();
        }
        room.messages.addAll(messages.subList(room.messages.size(), messages.size()));
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
        recent = null;
        character = 0;
    }

    public Instant now() {
        return clock.instant();
    }

    public List<Room> rooms() {
        tick();
        return List.copyOf(rooms.values());
    }

    public Room room(String id) {
        return rooms.get(id);
    }

    public Room find(RoomView view) {
        return rooms.values().stream().filter(r -> r.view == view).findFirst().orElse(null);
    }

    public boolean hidden(Room r) {
        return r.removed || r.ended || r.expiresAt != null && !r.expiresAt.isAfter(now()) || r.people >= r.view.capacity();
    }

    public List<Request> requests() {
        tick();
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
        tick();
        if (hidden(r) || retrySeconds(r) > 0) {
            throw new IllegalStateException("지금은 참여 요청을 보낼 수 없어요.");
        }
        var old = outgoing(r);
        if (old != null && old.status == ChatRequestStatus.PENDING) {
            return;
        }
        if (live()) {
            liveRequests.requestChat(r.bubbleId);
            return;
        }
        writable();
        requests.add(new Request(UUID.randomUUID().toString(), r.id, "나", false));
    }

    public void decide(Request q, boolean accept) {
        tick();
        if (q.status != ChatRequestStatus.PENDING) {
            return;
        }
        Room r = room(q.roomId);
        if (live()) {
            if (accept && (r == null || hidden(r))) {
                throw new IllegalStateException("지금은 이 방의 요청을 승인할 수 없어요.");
            }
            liveRequests.decideChat(q.id, accept);
            return;
        }
        writable();
        if (accept && !hidden(r)) {
            q.status = ChatRequestStatus.ACCEPTED;
            r.people++;
            if (!q.incoming) {
                r.joined = true;
                r.visited = true;
            }
        } else {
            reject(q);
        }
        tick();
    }

    private void reject(Request q) {
        q.status = ChatRequestStatus.REJECTED;
        if (!q.incoming && rejected.add(q.id)) {
            retry.put(q.roomId, now().plus(RETRY_DELAY));
        }
    }

    public void tick() {
        if (!preview) {
            return;
        }
        for (var q : requests) {
            if (q.status == ChatRequestStatus.PENDING && hidden(room(q.roomId))) {
                reject(q);
            }
        }
    }

    public boolean canOpen(Room r) {
        return r.ended || r.joined || !hidden(r) && (!r.view.privateRoom() || r.view.owner());
    }

    public void openLiveRoom(Room room) {
        if (!canOpen(room)) return;
        liveRequests.joinChat(room.bubbleId);
    }

    public void enter(Room r) {
        writable();
        if (!r.ended && !r.joined) {
            if (!canOpen(r)) {
                throw new IllegalStateException("참여 승인이 필요하거나 입장할 수 없는 방이에요.");
            }
            r.joined = true;
            r.people++;
        }
        r.visited = true;
        if (r.view.owner()) {
            recent = r.id;
        }
        tick();
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
        if (live()) {
            liveRequests.leaveChat(r.bubbleId);
            return;
        }
        writable();
        if (r.joined) {
            r.joined = false;
            r.people = Math.max(0, r.people - 1);
        }
    }

    public void end(Room r) {
        if (live()) {
            liveRequests.endChat(r.conversation.chatRoomId());
            return;
        }
        writable();
        if (!r.view.owner()) {
            throw new IllegalStateException("방장만 종료할 수 있어요.");
        }
        r.ended = true;
        r.joined = false;
        r.people = 0;
        tick();
    }

    public void send(Room r, String text, boolean mine) {
        if (live()) {
            liveRequests.sendMessage(r.conversation.chatRoomId(), text);
            return;
        }
        writable();
        if (r.ended || !r.joined) {
            throw new IllegalStateException("대화에 참여한 상태에서만 전송할 수 있어요.");
        }
        if (text == null || text.isBlank()) {
            return;
        }
        Instant sentAt = now();
        r.messages.add(new Message(mine ? "익명의 고래" : "달빛 구름", text.strip(), mine, sentAt));
        if (!r.removed && r.expiresAt != null && r.expiresAt.isAfter(sentAt)) {
            r.expiresAt = sentAt.plusSeconds(r.previewExpirationMinutes * 60L);
        }
    }

    public List<Message> speech(Room r, String sender) {
        var values = r.messages.stream().filter(m -> Objects.equals(live() ? m.memberId() : m.sender(), sender)
                && m.at() != null && m.at().plusSeconds(7).isAfter(now())).toList();
        return values.subList(Math.max(0, values.size() - 3), values.size());
    }

    public int character() {
        return character;
    }

    public void applyProfile(ProfileCharacter profile) {
        character = CharacterChoice.indexOf(profile);
    }

    public void character(int value) {
        writable();
        character = Math.floorMod(value, CharacterChoice.count());
    }

    public Room create(RoomView view, int minutes) {
        if (live()) {
            liveRequests.createBubble(view, minutes);
            return null;
        }
        writable();
        Room r = new Room(UUID.randomUUID().toString(), view, now());
        r.previewExpirationMinutes = minutes;
        r.expiresAt = now().plusSeconds(minutes * 60L);
        rooms.put(r.id, r);
        recent = r.id;
        return r;
    }

    public void update(Room r, RoomView view, int minutes) {
        if (live()) {
            liveRequests.updateBubble(r.bubbleId, view, minutes);
            return;
        }
        writable();
        if (!r.view.owner()) {
            throw new IllegalStateException("내 비눗방울만 수정할 수 있어요.");
        }
        r.view = view;
        r.previewExpirationMinutes = minutes;
        r.expiresAt = now().plusSeconds(minutes * 60L);
        tick();
    }

    public void remove(Room r) {
        if (live()) {
            liveRequests.deleteBubble(r.bubbleId);
            return;
        }
        writable();
        if (!r.view.owner()) {
            throw new IllegalStateException("내 비눗방울만 삭제할 수 있어요.");
        }
        r.removed = true;
        tick();
    }

    private void writable() {
        if (!preview) {
            throw new IllegalStateException("서비스 연결 후 사용할 수 있어요.");
        }
    }

}
