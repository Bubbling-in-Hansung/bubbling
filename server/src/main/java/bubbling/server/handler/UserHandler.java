package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.user.UserDto;
import bubbling.common.dto.user.UserUpdateRequest;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.domain.User;
import bubbling.server.mapper.UserMapper;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;
import bubbling.server.service.UserService;

public class UserHandler {

    private final UserService userService;

    private final MessageSender messageSender;
    private final JsonCodec jsonCodec = new JsonCodec();

    public UserHandler(UserService userService, MessageSender messageSender) {
        this.userService = userService;
        this.messageSender = messageSender;
    }

    public void update(ClientConnection connection, MessageEnvelope message) {
        UserUpdateRequest request = jsonCodec.fromPayload(
                message.payload(),
                UserUpdateRequest.class
        );

        User user = connection.requireAuthenticatedUser();

        userService.update(user, request.nickname(), request.profile());

        UserDto payload = UserMapper.toDto(user);
        MessageEnvelope response = new MessageEnvelope(
                MessageType.USER_UPDATED,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);
    }

    public void info(ClientConnection connection, MessageEnvelope message) {
        User user = connection.requireAuthenticatedUser();

        UserDto payload = UserMapper.toDto(user);
        MessageEnvelope response = new MessageEnvelope(
                MessageType.USER_INFO,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);
    }
}
