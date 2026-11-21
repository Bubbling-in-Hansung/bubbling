package bubbling.server.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.system.ExceptionResponse;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.ExceptionCode;
import bubbling.server.exception.code.GlobalExceptionCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MessageDispatcher.class);

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
            throw new IllegalStateException("이미 등록된 메시지 타입입니다 -> " + type);
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
        } catch (BaseException e) {
            log.debug(
                    "요청 처리 실패: code={}, requestId={}",
                    e.getExceptionCode().getCode(),
                    message.requestId()
            );

            sendError(
                    connection,
                    message,
                    e.getExceptionCode()
            );
        } catch (RuntimeException e) {
            log.error(
                    "메시지 처리 중 내부 오류: type={}, requestId={}",
                    message.type(),
                    message.requestId(),
                    e
            );

            sendError(
                    connection,
                    message,
                    GlobalExceptionCode.INTERNAL_ERROR
            );
        }
    }

    private void sendError(
            ClientConnection connection,
            MessageEnvelope request,
            ExceptionCode exceptionCode
    ) {
        ExceptionResponse e = new ExceptionResponse(exceptionCode.getCode(), exceptionCode.getMessage());

        MessageEnvelope response = new MessageEnvelope(
                MessageType.ERROR,
                request.requestId(),
                jsonCodec.toPayload(e)
        );

        messageSender.send(connection, response);
    }
}