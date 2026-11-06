package bubbling.server.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;

public class MessageDispatcher {

    private final MessageSender messageSender;
    private final JsonCodec jsonCodec = new JsonCodec();

    public MessageDispatcher(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public void dispatch(ClientConnection connection, MessageEnvelope message) {
        switch (message.type()) {
            case PING -> handlePing(connection, message);
            case TEST_MESSAGE -> handleTestMessage(
                    connection,
                    message
            );
            default -> sendError(
                    connection,
                    message.requestId(),
                    "지원하지 않는 메시지입니다."
            );
        }
    }

    private void handlePing(ClientConnection connection, MessageEnvelope message) {
        MessageEnvelope response = new MessageEnvelope(
                MessageType.PONG,
                message.requestId(),
                jsonCodec.toPayload("pong")
        );

        messageSender.send(connection, response);
    }

    private void handleTestMessage(ClientConnection connection, MessageEnvelope message) {
        String content;

        try {
            content = jsonCodec.fromPayload(message.payload(), String.class);
        } catch (IllegalArgumentException e) {
            sendError(
                    connection,
                    message.requestId(),
                    "잘못된 메시지 형식입니다."
            );
            return;
        }

        if (content == null || content.length() > 1000) {
            sendError(
                    connection,
                    message.requestId(),
                    "메시지 내용이 올바르지 않습니다."
            );
            return;
        }

        MessageEnvelope response = new MessageEnvelope(
                MessageType.TEST_BROADCAST,
                message.requestId(),
                jsonCodec.toPayload(content)
        );

        messageSender.broadcast(response);
    }

    private void sendError(
            ClientConnection connection,
            String requestId,
            String content
    ) {
        MessageEnvelope response = new MessageEnvelope(
                MessageType.ERROR,
                requestId,
                jsonCodec.toPayload(content)
        );

        messageSender.send(connection, response);
    }
}