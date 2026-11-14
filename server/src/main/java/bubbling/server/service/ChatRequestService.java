package bubbling.server.service;

import bubbling.server.domain.*;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatRequestExceptionCode;
import bubbling.server.exception.code.ChatRoomExceptionCode;
import bubbling.server.store.ChatRequestStore;

public class ChatRequestService {

    private final ChatRequestStore store;
    private final ChatRoomService chatRoomService;
    private final ChatMemberService chatMemberService;

    public ChatRequestService(
            ChatRequestStore store,
            ChatRoomService chatRoomService,
            ChatMemberService chatMemberService
    ) {
        this.store = store;
        this.chatRoomService = chatRoomService;
        this.chatMemberService = chatMemberService;
    }

    public ChatRequest create(User user, Bubble bubble) {
        ChatRoom chatRoom = chatRoomService.findByBubble(bubble);

        synchronized (chatRoom) {
            ChatRequest chatRequest = ChatRequest.create(bubble, user);

            if (chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom) >= bubble.getMaxMemberCnt()) {
                throw new BaseException(ChatRoomExceptionCode.FULL);
            }

            return store.save(chatRequest);
        }
    }

    public ChatRequest findById(String id) {
        return store.findById(id).orElseThrow(
                () -> new BaseException(ChatRequestExceptionCode.NOT_FOUND)
        );
    }

    public ChatMember accept(User user, String id) {
        ChatRequest chatRequest = findById(id);
        synchronized (chatRequest) {
            chatRequest.validateAcceptable(user);

            ChatMember chatMember = chatRoomService.join(
                    chatRequest.getRequester(),
                    chatRequest.getBubble(),
                    true
            );

            chatRequest.accept(user);

            return chatMember;
        }
    }

    public ChatRequest reject(User user, String id) {
        ChatRequest chatRequest = findById(id);
        chatRequest.reject(user);
        return chatRequest;
    }
}