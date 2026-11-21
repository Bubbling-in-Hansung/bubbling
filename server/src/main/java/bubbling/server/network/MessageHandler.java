package bubbling.server.network;

import bubbling.common.protocol.MessageEnvelope;

@FunctionalInterface
public interface MessageHandler {
    void handle(
            ClientConnection connection,
            MessageEnvelope message
    );
}
