package bubbling.server.handler;

import bubbling.common.protocol.MessageEnvelope;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;
import bubbling.server.service.UserService;

public class UserHandler {

    private final UserService userService;
    private final MessageSender messageSender;

    public UserHandler(UserService userService, MessageSender messageSender) {
        this.userService = userService;
        this.messageSender = messageSender;
    }

    public void update(ClientConnection connection, MessageEnvelope message) {

    }

    public void info(ClientConnection connection, MessageEnvelope message) {

    }
}
