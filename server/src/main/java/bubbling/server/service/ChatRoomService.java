package bubbling.server.service;

import bubbling.common.enums.ChatRequestStatus;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.ChatMember;
import bubbling.server.domain.ChatRoom;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.BubbleExceptionCode;
import bubbling.server.exception.code.ChatMemberExceptionCode;
import bubbling.server.exception.code.ChatRequestExceptionCode;
import bubbling.server.exception.code.ChatRoomExceptionCode;
import bubbling.server.store.ChatRequestStore;
import bubbling.server.store.ChatRoomStore;

public class ChatRoomService {

    private final ChatRoomStore store;
    private final ChatMemberService chatMemberService;
    private final ChatMessageService chatMessageService;
    private final ChatRequestStore chatRequestStore;

    public ChatRoomService(
            ChatRoomStore store,
            ChatMessageService chatMessageService,
            ChatMemberService chatMemberService,
            ChatRequestStore chatRequestStore
    ) {
        this.store = store;
        this.chatMessageService = chatMessageService;
        this.chatMemberService = chatMemberService;
        this.chatRequestStore = chatRequestStore;
    }

    public ChatRoom create(Bubble bubble) {
        ChatRoom chatRoom = ChatRoom.create(bubble);

        chatMemberService.create(bubble.getOwner(), chatRoom);

        try {
            return store.save(chatRoom);
        } catch (RuntimeException e) {
            chatMemberService.deleteAllByChatRoom(chatRoom);
            throw new BaseException(ChatRoomExceptionCode.FAILED_TO_CREATION);
        }
    }

    public ChatRoom findById(String id) {
        return store.findById(id).orElseThrow(
                () -> new BaseException(ChatRoomExceptionCode.NOT_FOUND)
        );
    }

    public ChatRoom findByBubble(Bubble bubble) {
        return store.findByBubble(bubble).orElseThrow(
                () -> new BaseException(ChatRoomExceptionCode.NOT_FOUND)
        );
    }

    public void deleteByBubble(Bubble bubble) {
        ChatRoom chatRoom = findByBubble(bubble);

        chatMemberService.deleteAllByChatRoom(chatRoom);
        chatMessageService.deleteAllByChatRoom(chatRoom);

        store.delete(chatRoom.getId());
    }

    public ChatMember join(User user, Bubble bubble) {
        return join(user, bubble, false);
    }

    public ChatMember join(User user, Bubble bubble, boolean approved) {
        ChatRoom chatRoom = findByBubble(bubble);

        synchronized (chatRoom) {
            if (!chatRoom.isActive()) {
                throw new BaseException(ChatRoomExceptionCode.ENDED);
            }

            if (!chatRoom.getBubble().isAlive()) {
                throw new BaseException(BubbleExceptionCode.EXPIRED);
            }

            if (!bubble.getIsOpen() && !approved) {
                throw new BaseException(ChatRoomExceptionCode.APPROVAL_REQUIRED);
            }

            if (chatRequestStore.findByBubbleAndRequesterAndStatus(
                    bubble, user, ChatRequestStatus.REJECTED
            ).isPresent()) {
                throw new BaseException(ChatRequestExceptionCode.NOT_ALLOWED);
            }

            if (chatMemberService.findByChatRoomAndUserAndIsActive(chatRoom, user) != null) {
                throw new BaseException(ChatMemberExceptionCode.ALREADY_JOINED);
            }

            if (chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom) >= bubble.getMaxMemberCnt()) {
                throw new BaseException(ChatRoomExceptionCode.FULL);
            }

            return chatMemberService.create(user, chatRoom);
        }
    }

    public ChatMember leave(User user, ChatRoom chatRoom) {
        synchronized (chatRoom) {
            ChatMember chatMember = chatMemberService.findByChatRoomAndUserAndIsActive(chatRoom, user);

            if(chatMember == null) {
                throw new BaseException(ChatMemberExceptionCode.NOT_JOINED);
            }

            if(chatRoom.getBubble().getOwner().equals(user)) {
                throw new BaseException(ChatMemberExceptionCode.CANNOT_LEFT_OWNER);
            }

            chatMember.leave();

            return chatMember;
        }
    }

    public ChatRoom end(User user, String chatRoomId) {
        ChatRoom chatRoom = findById(chatRoomId);

        if(!chatRoom.getBubble().getOwner().equals(user)) {
            throw new BaseException(ChatRoomExceptionCode.FORBIDDEN);
        }

        chatRoom.end();
        chatRoom.getBubble().close();

        return chatRoom;
    }
}
