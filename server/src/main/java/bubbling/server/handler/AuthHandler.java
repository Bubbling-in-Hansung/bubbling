package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.auth.LoginRequest;
import bubbling.common.dto.auth.LoginResponse;
import bubbling.common.dto.auth.RegisterRequest;
import bubbling.common.dto.auth.RegisterResponse;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.AuthExceptionCode;
import bubbling.server.mapper.UserMapper;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;
import bubbling.server.service.AuthService;

public class AuthHandler {

    private final AuthService authService;
    private final MessageSender messageSender;
    private final JsonCodec jsonCodec = new JsonCodec();

    public AuthHandler(AuthService authService, MessageSender messageSender) {
        this.authService = authService;
        this.messageSender = messageSender;
    }

    public void register(ClientConnection connection, MessageEnvelope message) {
        RegisterRequest request = jsonCodec.fromPayload(
                message.payload(),
                RegisterRequest.class
        );

        if (connection.isAuthenticated()) {
            throw new BaseException(AuthExceptionCode.ALREADY_LOGGED_IN);
        }

        User user = authService.register(
                request.loginId(),
                request.password(),
                request.nickname()
        );

        connection.authenticate(user);

        RegisterResponse payload = new RegisterResponse(UserMapper.toDto(user));
        MessageEnvelope response = new MessageEnvelope(
                MessageType.REGISTER_RESULT,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);
    }

    public void login(ClientConnection connection, MessageEnvelope message) {
        LoginRequest request = jsonCodec.fromPayload(
                message.payload(),
                LoginRequest.class
        );

        User user = authService.login(
                request.loginId(),
                request.password()
        );

        connection.authenticate(user);
        LoginResponse payload = new LoginResponse(UserMapper.toDto(user));
        MessageEnvelope response = new MessageEnvelope(
                MessageType.LOGIN_RESULT,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);
    }
}
