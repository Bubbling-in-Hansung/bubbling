package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;
import bubbling.server.service.ChatRoomService;

public class ChatRoomHandler {

    private final ChatRoomService chatRoomService;
    private final MessageSender messageSender;
    private final JsonCodec jsonCodec = new JsonCodec();

    public ChatRoomHandler(
            ChatRoomService chatRoomService,
            MessageSender messageSender
    ) {
        this.chatRoomService = chatRoomService;
        this.messageSender = messageSender;
    }

    public void join(ClientConnection connection, MessageEnvelope message) {

    }

    public void leave(ClientConnection connection, MessageEnvelope message) {

    }

    public void end(ClientConnection connection, MessageEnvelope message) {

    }
}
