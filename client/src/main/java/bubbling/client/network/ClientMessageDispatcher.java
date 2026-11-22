package bubbling.client.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.system.ExceptionResponse;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static bubbling.common.protocol.MessageType.*;

public class ClientMessageDispatcher {
    private final JsonCodec codec = new JsonCodec();
    private final Map<MessageType, Consumer<MessageEnvelope>> handlers = new ConcurrentHashMap<>();
    private volatile Runnable disconnectHandler = () -> {};

    public ClientMessageDispatcher() {
        register(PONG, message -> System.out.println("[PONG] " + text(message)));
        register(TEST_BROADCAST, message -> System.out.println("[BROADCAST] " + text(message)));
        register(ERROR, message -> System.err.println("[ERROR] " + decodeError(message).message()));
    }

    public void register(MessageType type, Consumer<MessageEnvelope> handler) {
        handlers.put(type, handler);
    }

    public void onDisconnect(Runnable handler) {
        disconnectHandler = handler;
    }

    void disconnected() {
        disconnectHandler.run();
    }

    public void dispatch(MessageEnvelope message) {
        Consumer<MessageEnvelope> handler = handlers.get(message.type());
        if (handler == null) {
            System.out.println("처리되지 않은 메시지: " + message.type());
            return;
        }
        handler.accept(message);
    }

    public ExceptionResponse decodeError(MessageEnvelope message) {
        String payload = message.payload();
        if (payload != null && payload.stripLeading().startsWith("{")) {
            return codec.fromPayload(payload, ExceptionResponse.class);
        }
        return new ExceptionResponse(null, text(message));
    }

    private String text(MessageEnvelope message) {
        return codec.fromPayload(message.payload(), String.class);
    }
}
