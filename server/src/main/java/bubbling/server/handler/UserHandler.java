package bubbling.server.handler;

import bubbling.server.network.MessageSender;
import bubbling.server.service.UserService;

public class UserHandler {

    private final UserService userService;
    private final MessageSender messageSender;

    public UserHandler(UserService userService, MessageSender messageSender) {
        this.userService = userService;
        this.messageSender = messageSender;
    }
}
