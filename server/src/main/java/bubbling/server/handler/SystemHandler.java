package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.bubble.BubbleDto;
import bubbling.common.dto.system.InitialStateDto;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.ChatRoom;
import bubbling.server.domain.User;
import bubbling.server.mapper.BubbleMapper;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.ClientRegistry;
import bubbling.server.network.MessageSender;
import bubbling.server.service.BubbleService;
import bubbling.server.service.ChatMemberService;
import bubbling.server.service.ChatRoomService;

import java.util.List;

public class SystemHandler {

    private final BubbleService bubbleService;
    private final ChatRoomService chatRoomService;
    private final ChatMemberService chatMemberService;

    private final ClientRegistry clientRegistry;
    private final MessageSender messageSender;
    private final JsonCodec jsonCodec = new JsonCodec();

    public SystemHandler(
            BubbleService bubbleService,
            ChatRoomService chatRoomService,
            ChatMemberService chatMemberService,
            ClientRegistry clientRegistry,
            MessageSender messageSender
    ) {
        this.bubbleService = bubbleService;
        this.chatRoomService = chatRoomService;
        this.chatMemberService = chatMemberService;
        this.clientRegistry = clientRegistry;
        this.messageSender = messageSender;
    }

    public void init(ClientConnection connection, MessageEnvelope message) {
        User user = connection.requireAuthenticatedUser();

        List<Bubble> bubbles = bubbleService.findAllByAlive();
        List<BubbleDto> dtos = bubbles.stream()
                .map(b -> {
                    ChatRoom chatRoom = chatRoomService.findByBubble(b);
                    int count = chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom);
                    return BubbleMapper.toDto(b, count);
                })
                .toList();

        String myBubbleId = bubbles.stream()
                .filter(b -> b.getOwner().equals(user))
                .map(Bubble::getId)
                .findFirst()
                .orElse(null);

        InitialStateDto payload = new InitialStateDto(
                dtos,
                myBubbleId,
                clientRegistry.getOnlineCount()
        );
        MessageEnvelope response = new MessageEnvelope(
                MessageType.INITIAL_STATE,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);
    }
}
