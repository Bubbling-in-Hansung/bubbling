package bubbling.server.store;

import bubbling.server.domain.ChatMember;
import bubbling.server.domain.ChatRoom;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatMemberExceptionCode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatMemberStore {

    private final ConcurrentMap<String, ChatMember> members = new ConcurrentHashMap<>();

    public synchronized ChatMember save(ChatMember chatMember) {
        Objects.requireNonNull(chatMember, "채팅의 멤버는 필수입니다.");

        if(findByChatRoomAndUserAndIsActive(chatMember.getChatRoom(), chatMember.getUser()).isPresent()) {
            throw new BaseException(ChatMemberExceptionCode.ALREADY_JOINED);
        }

        members.put(chatMember.getId(), chatMember);

        return chatMember;
    }

    public Optional<ChatMember> findById(String id) {
        return members.values().stream()
                .filter(chatMember -> chatMember.getId().equals(id))
                .findFirst();
    }

    public Optional<ChatMember> findByChatRoomAndUserAndIsActive(ChatRoom chatRoom, User user) {
        return members.values().stream()
                .filter(member -> member.getChatRoom().getId().equals(chatRoom.getId()))
                .filter(member -> member.getUser().getId().equals(user.getId()))
                .filter(ChatMember::isActive)
                .findFirst();
    }

    public List<ChatMember> findAllByChatRoom(ChatRoom chatRoom) {
        return members.values().stream()
                .filter(chatMember -> chatMember.getChatRoom().equals(chatRoom)).toList();
    }

    public List<ChatMember> findAllByChatRoomAndIsActive(ChatRoom chatRoom) {
        return members.values().stream()
                .filter(chatMember -> chatMember.isActive() && chatMember.getChatRoom().equals(chatRoom))
                .toList();
    }

    public synchronized void delete(String id) {
        members.remove(id);
    }

    public synchronized void deleteAll(List<String> ids) {
        for(String id : ids) {
            delete(id);
        }
    }
}
