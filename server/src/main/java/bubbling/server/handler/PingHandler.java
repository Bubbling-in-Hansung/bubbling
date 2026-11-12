package bubbling.server.handler;

import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;

public class PingHandler {

    private final MessageSender messageSender;

    public PingHandler(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public void ping(ClientConnection connection, MessageEnvelope request) {
        MessageEnvelope response = new MessageEnvelope(
                MessageType.PONG,
                request.requestId(),
                null
        );

        messageSender.send(connection, response);
    }
}