package bubbling.server.config;

import bubbling.common.protocol.MessageType;
import bubbling.server.handler.*;
import bubbling.server.network.ClientRegistry;
import bubbling.server.network.MessageDispatcher;
import bubbling.server.network.MessageSender;
import bubbling.server.notifier.OnlineStatusNotifier;
import bubbling.server.scheduler.BubbleExpirationScheduler;
import bubbling.server.seed.SeedBootstrap;
import bubbling.server.service.bubble.BubbleReactionService;
import bubbling.server.service.bubble.BubbleService;
import bubbling.server.service.chat.ChatMemberService;
import bubbling.server.service.chat.ChatMessageService;
import bubbling.server.service.chat.ChatRequestService;
import bubbling.server.service.chat.ChatRoomService;
import bubbling.server.service.game.GameResultService;
import bubbling.server.service.game.search.GameResultSearchStrategyResolver;
import bubbling.server.service.user.AuthService;
import bubbling.server.service.user.PasswordService;
import bubbling.server.service.user.UserService;
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
    private final BubbleReactionService bubbleReactionService;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatMemberService chatMemberService;
    private final ChatRequestService chatRequestService;
    private final GameResultService gameResultService;
    private final GameResultSearchStrategyResolver gameResultSearchStrategyResolver;

    public ServerComponent() {
        this.clientRegistry = new ClientRegistry();
        this.messageSender = new MessageSender(clientRegistry);
        this.messageDispatcher = new MessageDispatcher(messageSender);

        this.onlineStatusNotifier = new OnlineStatusNotifier(clientRegistry, messageSender);

        UserStore userStore = new UserStore();
        BubbleStore bubbleStore = new BubbleStore();
        BubbleReactionStore bubbleReactionStore = new BubbleReactionStore();
        ChatRoomStore chatRoomStore = new ChatRoomStore();
        ChatMessageStore chatMessageStore = new ChatMessageStore();
        ChatMemberStore chatMemberStore = new ChatMemberStore();
        ChatRequestStore chatRequestStore = new ChatRequestStore();
        GameResultStore gameResultStore = new GameResultStore();

        PasswordService passwordService = new PasswordService();

        this.userService = new UserService(userStore);
        this.authService = new AuthService(userService, passwordService);
        this.chatMemberService = new ChatMemberService(chatMemberStore);
        this.chatMessageService = new ChatMessageService(chatMessageStore);
        this.chatRoomService = new ChatRoomService(chatRoomStore, chatMessageService, chatMemberService, chatRequestStore);
        this.chatRequestService = new ChatRequestService(chatRequestStore, chatRoomService, chatMemberService);
        this.bubbleReactionService = new BubbleReactionService(bubbleReactionStore);
        this.bubbleService = new BubbleService(bubbleStore, bubbleReactionService, chatRoomService, chatRequestService);
        this.gameResultService = new GameResultService(gameResultStore);
        this.gameResultSearchStrategyResolver = new GameResultSearchStrategyResolver(gameResultStore);

        this.bubbleExpirationScheduler = new BubbleExpirationScheduler(bubbleService, messageSender);

        loadSeedData();
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
        registerChatTypingHandlers();
        registerGameResultHandlers();
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

        messageDispatcher.register(
                MessageType.LOGOUT,
                true,
                handler::logout
        );

        messageDispatcher.register(
                MessageType.PASSWORD_CHANGE_REQUEST,
                true,
                handler::changePassword
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
                MessageType.BUBBLE_DETAIL_REQUEST,
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

        messageDispatcher.register(
                MessageType.CHAT_POSITION_REQUEST,
                true,
                handler::position
        );

        messageDispatcher.register(
                MessageType.CHAT_MUSIC_SET,
                true,
                handler::music
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

        messageDispatcher.register(
                MessageType.CHAT_REQUEST_LIST_REQUEST,
                true,
                handler::list
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

    private void registerChatTypingHandlers() {
        ChatTypingHandler handler = new ChatTypingHandler(
                chatRoomService,
                chatMemberService,
                messageSender
        );

        messageDispatcher.register(
                MessageType.CHAT_TYPING,
                true,
                handler::typing
        );
    }

    private void registerGameResultHandlers() {
        GameResultHandler handler = new GameResultHandler(
                gameResultService,
                gameResultSearchStrategyResolver,
                messageSender
        );

        messageDispatcher.register(
                MessageType.GAME_RANKING_REQUEST,
                true,
                handler::ranking
        );

        messageDispatcher.register(
                MessageType.GAME_RESULT_REQUEST,
                true,
                handler::result
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

    private void loadSeedData() {
        new SeedBootstrap().load(
                userService,
                bubbleService,
                chatRoomService,
                chatMemberService,
                chatRequestService,
                chatMessageService
        );
    }
}
