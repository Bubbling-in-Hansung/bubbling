package bubbling.server.config;

import bubbling.common.protocol.MessageType;
import bubbling.server.handler.*;
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
        this.chatRoomService = new ChatRoomService(chatRoomStore, chatMessageService, chatMemberService, chatRequestStore);
        this.bubbleService = new BubbleService(bubbleStore, chatRoomService);
        this.chatRequestService = new ChatRequestService(chatRequestStore, chatRoomService, chatMemberService);

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
        registerUserHandlers();
        registerAuthHandlers();
        registerBubbleHandlers();
        registerChatRoomHandlers();
        registerChatRequestHandlers();
        registerChatMessageHandlers();
        registerSystemHandlers();
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

    private void registerUserHandlers() {
        UserHandler handler = new UserHandler(
                userService,
                messageSender
        );

        messageDispatcher.register(
                MessageType.USER_UPDATE,
                true,
                handler::update
        );

        messageDispatcher.register(
                MessageType.USER_INFO_REQUEST,
                true,
                handler::info
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

        messageDispatcher.register(
                MessageType.BUBBLE_DETAIL,
                true,
                handler::detail
        );
    }

    private void registerChatRoomHandlers() {
        ChatRoomHandler handler = new ChatRoomHandler(
                bubbleService,
                chatRoomService,
                chatMemberService,
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

        messageDispatcher.register(
                MessageType.MY_CHAT_ROOM_LIST_REQUEST,
                true,
                handler::list
        );
    }

    private void registerChatRequestHandlers() {
        ChatRequestHandler handler = new ChatRequestHandler(
                bubbleService,
                chatMemberService,
                chatRequestService,
                messageSender
        );

        messageDispatcher.register(
                MessageType.CHAT_REQUEST,
                true,
                handler::request
        );

        messageDispatcher.register(
                MessageType.CHAT_ACCEPT,
                true,
                handler::accept
        );

        messageDispatcher.register(
                MessageType.CHAT_REJECT,
                true,
                handler::reject
        );
    }

    private void registerChatMessageHandlers() {
        ChatMessageHandler handler = new ChatMessageHandler(
                chatRoomService,
                chatMemberService,
                chatMessageService,
                messageSender
        );

        messageDispatcher.register(
                MessageType.CHAT_MESSAGE,
                true,
                handler::send
        );

        messageDispatcher.register(
                MessageType.CHAT_HISTORY_REQUEST,
                true,
                handler::history
        );
    }

    private void registerSystemHandlers() {
        SystemHandler handler = new SystemHandler(
                bubbleService,
                chatRoomService,
                chatMemberService,
                clientRegistry,
                messageSender
        );

        messageDispatcher.register(
                MessageType.INITIAL_STATE_REQUEST,
                true,
                handler::init
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
