package bubbling.client.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.system.ExceptionResponse;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static bubbling.common.protocol.MessageType.*;

public class ClientMessageDispatcher {
    @FunctionalInterface
    public interface Command {
        void execute(MessageEnvelope message);
    }

    private final JsonCodec codec = new JsonCodec();
    private final Map<MessageType, Command> handlers = new ConcurrentHashMap<>();
    private volatile Runnable disconnectHandler = () -> {};

    public ClientMessageDispatcher() {
        register(PONG, message -> System.out.println("[PONG] " + text(message)));
        register(TEST_BROADCAST, message -> System.out.println("[BROADCAST] " + text(message)));
        register(ERROR, message -> System.err.println("[ERROR] " + decodeError(message).message()));
    }

    public void register(MessageType type, Command handler) {
        handlers.put(
                Objects.requireNonNull(type, "메시지 타입은 필수입니다."),
                Objects.requireNonNull(handler, "핸들러는 필수입니다.")
        );
    }

    public void onDisconnect(Runnable handler) {
        disconnectHandler = Objects.requireNonNull(handler, "종료 핸들러는 필수입니다.");
    }

    void disconnected() {
        disconnectHandler.run();
    }

    public void dispatch(MessageEnvelope message) {
        Command handler = handlers.get(message.type());
        if (handler == null) {
            System.out.println("처리되지 않은 메시지: " + message.type());
            return;
        }
        try {
            handler.execute(message);
        } catch (RuntimeException failure) {
            reportFailure(message.type(), failure);
        }
    }

    public void reportFailure(MessageType type, RuntimeException failure) {
        System.err.println("메시지 처리 실패 (" + type + "): " + failure.getClass().getSimpleName());
    }

    public <T> T decode(MessageEnvelope message, Class<T> type) {
        Objects.requireNonNull(message, "메시지는 필수입니다.");
        Objects.requireNonNull(type, "대상 타입은 필수입니다.");
        return Objects.requireNonNull(codec.fromPayload(message.payload(), type),
                "메시지 페이로드가 비어 있습니다: " + message.type());
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
