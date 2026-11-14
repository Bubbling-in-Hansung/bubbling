package bubbling.server.store;

import bubbling.common.enums.ChatRoomStatus;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.ChatRoom;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatRoomExceptionCode;

import java.util.Optional;

public class ChatRoomStore extends BaseStore<ChatRoom> {

    public ChatRoomStore() {
        super(ChatRoom::getId);
    }

    @Override
    protected void validateSave(ChatRoom chatRoom) {
        if (findByBubble(chatRoom.getBubble()).isPresent()) {
            throw new BaseException(ChatRoomExceptionCode.ALREADY_EXISTS);
        }
    }

    public Optional<ChatRoom> findByBubble(Bubble bubble) {
        return findAll().stream()
                .filter(cr -> cr.getBubble().getId().equals(bubble.getId()))
                .findFirst();
    }

    public Optional<ChatRoom> findByBubbleAndStatus(Bubble bubble, ChatRoomStatus status) {
        return findAll().stream()
                .filter(cr -> cr.getBubble().getId().equals(bubble.getId()) && cr.getStatus() == status)
                .findFirst();
    }
}