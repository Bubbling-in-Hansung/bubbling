package bubbling.server.service;

import bubbling.server.domain.Bubble;
import bubbling.server.domain.ChatRoom;
import bubbling.server.store.ChatRoomStore;

public class ChatRoomService {

    private final ChatRoomStore store;
    private final ChatMemberService chatMemberService;
    private final ChatMessageService chatMessageService;

    public ChatRoomService(
            ChatRoomStore store,
            ChatMessageService chatMessageService,
            ChatMemberService chatMemberService
    ) {
        this.store = store;
        this.chatMessageService = chatMessageService;
        this.chatMemberService = chatMemberService;
    }

    public ChatRoom create(Bubble bubble) {
        ChatRoom chatRoom = ChatRoom.create(bubble);

        chatMemberService.create(bubble.getOwner(), chatRoom);

        try {
            return store.save(chatRoom);
        } catch (RuntimeException e) {
            chatMemberService.deleteAllByChatRoom(chatRoom);
            throw e;
        }
    }

    public ChatRoom findByBubble(Bubble bubble) {
        return store.findByBubble(bubble)
                .orElseThrow(() -> new IllegalStateException("채팅방을 찾을 수 없습니다."));
    }

    public void deleteByBubble(Bubble bubble) {
        ChatRoom chatRoom = findByBubble(bubble);

        chatMemberService.deleteAllByChatRoom(chatRoom);
        chatMessageService.deleteAllByChatRoom(chatRoom);

        store.delete(chatRoom.getId());
    }
}
