package bubbling.server.config;

import bubbling.common.protocol.MessageType;
import bubbling.server.handler.AuthHandler;
import bubbling.server.handler.BubbleHandler;
import bubbling.server.handler.PingHandler;
import bubbling.server.handler.UserHandler;
import bubbling.server.network.ClientRegistry;
import bubbling.server.network.MessageDispatcher;
import bubbling.server.network.MessageSender;
import bubbling.server.security.PasswordService;
import bubbling.server.service.AuthService;
import bubbling.server.service.BubbleService;
import bubbling.server.service.UserService;
import bubbling.server.store.BubbleStore;
import bubbling.server.store.UserStore;

public class ServerComponent {

    private final MessageSender messageSender;
    private final ClientRegistry clientRegistry;
    private final MessageDispatcher messageDispatcher;

    private final UserService userService;
    private final AuthService authService;
    private final BubbleService bubbleService;

    public ServerComponent() {
        this.clientRegistry = new ClientRegistry();
        this.messageSender = new MessageSender(clientRegistry);
        this.messageDispatcher = new MessageDispatcher(messageSender);

        UserStore userStore = new UserStore();
        this.userService = new UserService(userStore);
        PasswordService passwordService = new PasswordService();
        this.authService = new AuthService(userService, passwordService);
        BubbleStore bubbleStore = new BubbleStore();
        this.bubbleService = new BubbleService(bubbleStore);

        registerHandlers();
    }

    private void registerHandlers() {
        // PING
        PingHandler pingHandler = new PingHandler(messageSender);
        messageDispatcher.register(
                MessageType.PING,
                false,
                pingHandler::ping
        );

        // 인증
        AuthHandler authHandler = new AuthHandler(authService, messageSender);
        messageDispatcher.register(
                MessageType.REGISTER,
                false,
                authHandler::register
        );
        messageDispatcher.register(
                MessageType.LOGIN,
                false,
                authHandler::login
        );

        // 사용자
        UserHandler userHandler = new UserHandler(userService, messageSender);

        // 비눗방울
        BubbleHandler bubbleHandler = new BubbleHandler(bubbleService, messageSender);
        messageDispatcher.register(
                MessageType.BUBBLE_CREATE,
                true,
                bubbleHandler::create
        );
        messageDispatcher.register(
                MessageType.BUBBLE_UPDATE,
                true,
                bubbleHandler::update
        );
        messageDispatcher.register(
                MessageType.BUBBLE_DELETE,
                true,
                bubbleHandler::delete
        );
        messageDispatcher.register(
                MessageType.BUBBLE_REACT,
                true,
                bubbleHandler::react
        );
    }

    public ClientRegistry getClientRegistry() {
        return clientRegistry;
    }

    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }
}
