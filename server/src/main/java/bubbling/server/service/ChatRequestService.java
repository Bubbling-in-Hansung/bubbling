package bubbling.server.service;

import bubbling.server.store.ChatRequestStore;

public class ChatRequestService {

    private final ChatRequestStore store;
    private final ChatRoomService chatRoomService;

    public ChatRequestService(
            ChatRequestStore store,
            ChatRoomService chatRoomService
    ) {
        this.store = store;
        this.chatRoomService = chatRoomService;
    }
}