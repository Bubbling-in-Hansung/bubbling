package bubbling.server.network;

import bubbling.common.protocol.MessageEnvelope;

import java.util.Collection;

public class MessageSender {

    private final ClientRegistry clientRegistry;

    public MessageSender(ClientRegistry clientRegistry) {
        this.clientRegistry = clientRegistry;
    }

    public void send(ClientConnection connection, MessageEnvelope message) {
        if (connection != null && !connection.isClosed()) {
            connection.send(message);
        }
    }

    public void sendAll(Collection<ClientConnection> connections, MessageEnvelope message) {
        for (ClientConnection connection : connections) {
            send(connection, message);
        }
    }

    public void broadcast(MessageEnvelope message) {
        sendAll(clientRegistry.getAll(), message);
    }

    public void broadcastExcept(ClientConnection excluded, MessageEnvelope message) {
        for (ClientConnection connection : clientRegistry.getAll()) {
            if (connection != excluded) {
                send(connection, message);
            }
        }
    }
}
