package bubbling.server.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.system.ErrorResponse;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MessageDispatcher {

    private final ConcurrentMap<MessageType, Route> routes = new ConcurrentHashMap<>();

    private record Route(
            MessageHandler handler,
            boolean authenticationRequired
    ) {}

    private final MessageSender messageSender;
    private final JsonCodec jsonCodec = new JsonCodec();

    public MessageDispatcher(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public void register(
            MessageType type,
            boolean authenticationRequired,
            MessageHandler handler
    ) {
        Objects.requireNonNull(type, "메시지 타입은 필수입니다.");
        Objects.requireNonNull(handler, "핸들러는 필수입니다.");

        Route route = new Route(handler, authenticationRequired);

        if (routes.putIfAbsent(type, route) != null) {
            throw new IllegalStateException(
                    "이미 등록된 메시지 타입입니다: " + type
            );
        }
    }

    public void dispatch(ClientConnection connection, MessageEnvelope message) {
        try {
            Route route = routes.get(message.type());

            if(route == null) {
                throw new IllegalArgumentException("지원하지 않는 메세지 타입입니다.");
            }

            if(route.authenticationRequired()) {
                connection.requireAuthenticatedUser();
            }

            route.handler().handle(connection, message);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendError(
                    connection,
                    message,
                    "REQUEST_FAILED",
                    e.getMessage()
            );
        } catch (RuntimeException e) {
            sendError(
                    connection,
                    message,
                    "INTERNAL_ERROR",
                    "요청 처리 중 오류가 발생했습니다."
            );
        }
    }

    private void sendError(
            ClientConnection connection,
            MessageEnvelope request,
            String code,
            String errorMessage
    ) {
        ErrorResponse error = new ErrorResponse(
                code,
                errorMessage
        );

        MessageEnvelope response = new MessageEnvelope(
                MessageType.ERROR,
                request.requestId(),
                jsonCodec.toPayload(error)
        );

        messageSender.send(connection, response);
    }
}