package bubbling.server.store;

import bubbling.server.domain.ChatMember;
import bubbling.server.domain.ChatRoom;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatMemberExceptionCode;

import java.util.List;
import java.util.Optional;

public class ChatMemberStore extends BaseStore<ChatMember> {

    public ChatMemberStore() {
        super(ChatMember::getId);
    }

    @Override
    protected void validateSave(ChatMember chatMember) {
        if(findByChatRoomAndUserAndIsActive(chatMember.getChatRoom(), chatMember.getUser()).isPresent()) {
            throw new BaseException(ChatMemberExceptionCode.ALREADY_JOINED);
        }
    }

    public Optional<ChatMember> findByChatRoomAndUserAndIsActive(ChatRoom chatRoom, User user) {
        return findAll().stream()
                .filter(member -> member.getChatRoom().getId().equals(chatRoom.getId()))
                .filter(member -> member.getUser().getId().equals(user.getId()))
                .filter(ChatMember::isActive)
                .findFirst();
    }

    public List<ChatMember> findAllByChatRoom(ChatRoom chatRoom) {
        return findAll().stream()
                .filter(chatMember -> chatMember.getChatRoom().equals(chatRoom)).toList();
    }

    public List<ChatMember> findAllByChatRoomAndIsActive(ChatRoom chatRoom) {
        return findAll().stream()
                .filter(chatMember -> chatMember.isActive() && chatMember.getChatRoom().equals(chatRoom))
                .toList();
    }
}
