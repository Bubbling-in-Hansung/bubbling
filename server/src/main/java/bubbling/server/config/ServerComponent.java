package bubbling.server.config;

import bubbling.common.protocol.MessageType;
import bubbling.server.handler.AuthHandler;
import bubbling.server.handler.BubbleHandler;
import bubbling.server.handler.ChatRoomHandler;
import bubbling.server.handler.PingHandler;
import bubbling.server.network.ClientRegistry;
import bubbling.server.network.MessageDispatcher;
import bubbling.server.network.MessageSender;
import bubbling.server.notifier.OnlineStatusNotifier;
import bubbling.server.scheduler.BubbleExpirationScheduler;
import bubbling.server.security.PasswordService;
import bubbling.server.service.*;
import bubbling.server.store.*;

public class ServerComponent {

    private final MessageSender messageSender;
    private final ClientRegistry clientRegistry;
    private final MessageDispatcher messageDispatcher;

    private final OnlineStatusNotifier onlineStatusNotifier;

    private final BubbleExpirationScheduler bubbleExpirationScheduler;

    private final UserService userService;
    private final AuthService authService;
    private final BubbleService bubbleService;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatMemberService chatMemberService;
    private final ChatRequestService chatRequestService;

    public ServerComponent() {
        this.clientRegistry = new ClientRegistry();
        this.messageSender = new MessageSender(clientRegistry);
        this.messageDispatcher = new MessageDispatcher(messageSender);

        this.onlineStatusNotifier = new OnlineStatusNotifier(clientRegistry, messageSender);

        UserStore userStore = new UserStore();
        BubbleStore bubbleStore = new BubbleStore();
        ChatRoomStore chatRoomStore = new ChatRoomStore();
        ChatMessageStore chatMessageStore = new ChatMessageStore();
        ChatMemberStore chatMemberStore = new ChatMemberStore();
        ChatRequestStore chatRequestStore = new ChatRequestStore();

        PasswordService passwordService = new PasswordService();

        this.userService = new UserService(userStore);
        this.authService = new AuthService(userService, passwordService);
        this.chatMemberService = new ChatMemberService(chatMemberStore);
        this.chatMessageService = new ChatMessageService(chatMessageStore);
        this.chatRoomService = new ChatRoomService(chatRoomStore, chatMessageService, chatMemberService);
        this.bubbleService = new BubbleService(bubbleStore, chatRoomService);
        this.chatRequestService = new ChatRequestService(chatRequestStore, chatRoomService);

        this.bubbleExpirationScheduler = new BubbleExpirationScheduler(bubbleService, messageSender);

        registerNotifiers();
        registerHandlers();
        startSchedulers();
    }

    private void registerNotifiers() {
        clientRegistry.setOnlineStatusChangedListener(onlineStatusNotifier::notifyChanged);
    }

    private void registerHandlers() {
        registerPingHandlers();
        registerAuthHandlers();
        registerBubbleHandlers();
    }

    private void registerPingHandlers() {
        PingHandler pingHandler = new PingHandler(messageSender);
        messageDispatcher.register(
                MessageType.PING,
                false,
                pingHandler::ping
        );
    }

    private void registerAuthHandlers() {
        AuthHandler handler = new AuthHandler(
                authService,
                messageSender
        );

        messageDispatcher.register(
                MessageType.REGISTER,
                false,
                handler::register
        );

        messageDispatcher.register(
                MessageType.LOGIN,
                false,
                handler::login
        );
    }

    private void registerBubbleHandlers() {
        BubbleHandler handler = new BubbleHandler(
                bubbleService,
                chatRoomService,
                chatMemberService,
                messageSender
        );

        messageDispatcher.register(
                MessageType.BUBBLE_CREATE,
                true,
                handler::create
        );

        messageDispatcher.register(
                MessageType.BUBBLE_UPDATE,
                true,
                handler::update
        );

        messageDispatcher.register(
                MessageType.BUBBLE_DELETE,
                true,
                handler::delete
        );

        messageDispatcher.register(
                MessageType.BUBBLE_REACT,
                true,
                handler::react
        );
    }

    private void registerChatRoomHandlers() {
        ChatRoomHandler handler = new ChatRoomHandler(
                chatRoomService,
                messageSender
        );

        messageDispatcher.register(
                MessageType.CHAT_JOIN,
                true,
                handler::join
        );

        messageDispatcher.register(
                MessageType.CHAT_LEAVE,
                true,
                handler::leave
        );

        messageDispatcher.register(
                MessageType.CHAT_END,
                true,
                handler::end
        );
    }

    public ClientRegistry getClientRegistry() {
        return clientRegistry;
    }

    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public void startSchedulers() {
        bubbleExpirationScheduler.start();
    }

    public void stopSchedulers() {
        bubbleExpirationScheduler.stop();
    }
}
