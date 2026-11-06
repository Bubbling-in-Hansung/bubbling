package bubbling.server.domain;

import bubbling.common.enums.ChatRoomStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ChatRoom {
    private final String id = UUID.randomUUID().toString();
    private final Bubble bubble;
    private List<ChatMember> members = new ArrayList<>();
    private List<ChatMessage> messages = new ArrayList<>();
    private ChatRoomStatus status = ChatRoomStatus.ACTIVE;
    private final LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime endedAt;

    private ChatRoom(Bubble bubble) {
        this.bubble = bubble;
    }

    public static ChatRoom create(Bubble bubble) {
        Objects.requireNonNull(bubble, "버블은 필수입니다.");
        return new ChatRoom(bubble);
    }

    public synchronized int getCurrentMemberCount() {
        return (int) members.stream()
                .filter(ChatMember::isActive)
                .count();
    }

    public synchronized void addMember(ChatMember member) {
        Objects.requireNonNull(member, "참여자는 필수입니다.");
        validateActive();

        if (!member.isActive()) {
            throw new IllegalStateException("이미 퇴장한 참여 기록입니다.");
        }

        if (getCurrentMemberCount() >= bubble.getMaxMemberCnt()) {
            throw new IllegalStateException("채팅방 정원이 가득 찼습니다.");
        }

        boolean alreadyJoined = members.stream()
                .filter(ChatMember::isActive)
                .anyMatch(m -> m.getUser().getId()
                        .equals(member.getUser().getId()));

        if (alreadyJoined) {
            throw new IllegalStateException("이미 참여 중인 사용자입니다.");
        }

        if (members.stream().anyMatch(m -> m.getId().equals(member.getId()))) {
            throw new IllegalStateException("이미 등록된 참여 기록입니다.");
        }

        members.add(member);
    }

    public synchronized void leaveMember(String userId) {
        validateActive();

        ChatMember member = members.stream()
                .filter(ChatMember::isActive)
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("참여 중인 사용자가 아닙니다.")
                );

        member.leave();
    }

    public synchronized void addMessage(ChatMessage message) {
        Objects.requireNonNull(message, "메시지는 필수입니다.");
        validateEnded();

        ChatMember sender = message.getChatMember();

        boolean isMember = members.stream()
                .anyMatch(m -> m == sender && m.isActive());

        if (!isMember) {
            throw new IllegalStateException(
                    "현재 채팅방에 참여 중인 사용자만 메시지를 보낼 수 있습니다."
            );
        }

        messages.add(message);
    }

    public synchronized void end() {
        validateActive();

        this.status = ChatRoomStatus.ENDED;
        this.endedAt = LocalDateTime.now();

        for (ChatMember member : members) {
            if (member.isActive()) {
                member.leave();
            }
        }
    }

    private void validateActive() {
        if (status != ChatRoomStatus.ACTIVE) {
            throw new IllegalStateException("입장이 만료된 채팅방입니다.");
        }
    }

    private void validateEnded() {
        if (status == ChatRoomStatus.ENDED) {
            throw new IllegalStateException("대화가 종료된 채팅방입니다.");
        }
    }

    public String getId() {
        return id;
    }

    public Bubble getBubble() {
        return bubble;
    }

    public synchronized List<ChatMember> getMembers() {
        return List.copyOf(members);
    }

    public synchronized List<ChatMessage> getMessages() {
        return List.copyOf(messages);
    }

    public ChatRoomStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }
}
