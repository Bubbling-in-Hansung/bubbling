package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.auth.LoginRequest;
import bubbling.common.dto.auth.LoginResponse;
import bubbling.common.dto.auth.RegisterRequest;
import bubbling.common.dto.auth.RegisterResponse;
import bubbling.common.dto.user.UserDto;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.domain.User;
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

        User user = authService.register(
                request.loginId(),
                request.password(),
                request.nickname()
        );

        RegisterResponse payload = new RegisterResponse(
                new UserDto(
                        user.getId(),
                        user.getLoginId(),
                        user.getNickname(),
                        user.getProfile(),
                        user.getCreatedAt().toString()
                )
        );

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

        LoginResponse payload = new LoginResponse(
                new UserDto(
                        user.getId(),
                        user.getLoginId(),
                        user.getNickname(),
                        user.getProfile(),
                        user.getCreatedAt().toString()
                )
        );

        MessageEnvelope response = new MessageEnvelope(
                MessageType.LOGIN_RESULT,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);
    }
}
