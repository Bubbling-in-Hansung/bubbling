package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.bubble.*;
import bubbling.common.enums.BubbleStatus;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.ChatRoom;
import bubbling.server.domain.User;
import bubbling.server.mapper.BubbleMapper;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;
import bubbling.server.service.BubbleService;
import bubbling.server.service.ChatMemberService;
import bubbling.server.service.ChatRoomService;

public class BubbleHandler {

    private final BubbleService bubbleService;
    private final ChatRoomService chatRoomService;
    private final ChatMemberService chatMemberService;
    private final MessageSender messageSender;
    private final JsonCodec jsonCodec = new JsonCodec();

    public BubbleHandler(
            BubbleService bubbleService,
            ChatRoomService chatRoomService,
            ChatMemberService chatMemberService,
            MessageSender messageSender
    ) {
        this.bubbleService = bubbleService;
        this.chatRoomService = chatRoomService;
        this.chatMemberService = chatMemberService;
        this.messageSender = messageSender;
    }

    public void create(ClientConnection connection, MessageEnvelope message) {
        BubbleCreateRequest request = jsonCodec.fromPayload(
                message.payload(),
                BubbleCreateRequest.class
        );

        User owner = connection.requireAuthenticatedUser();

        Bubble bubble = bubbleService.create(
                owner,
                request.title(),
                request.category(),
                request.method(),
                request.maxMemberCnt(),
                request.isOpen(),
                request.expireMinute()
        );

        BubbleDto payload = BubbleMapper.toDto(bubble, 1);
        MessageEnvelope response = new MessageEnvelope(
                MessageType.BUBBLE_CREATED,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);

        MessageEnvelope event = new MessageEnvelope(
                MessageType.BUBBLE_CREATED,
                null,
                jsonCodec.toPayload(payload)
        );
        messageSender.broadcastExcept(connection, event);
    }

    public void update(ClientConnection connection, MessageEnvelope message) {
        BubbleUpdateRequest request = jsonCodec.fromPayload(
                message.payload(),
                BubbleUpdateRequest.class
        );

        User owner = connection.requireAuthenticatedUser();
        Bubble bubble = bubbleService.update(
                owner,
                request.bubbleId(),
                request.title(),
                request.category(),
                request.method(),
                request.maxMemberCnt(),
                request.isOpen()
        );

        ChatRoom chatRoom = chatRoomService.findByBubble(bubble);
        int currentMemberCnt = chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom);

        BubbleDto payload = BubbleMapper.toDto(bubble, currentMemberCnt);
        MessageEnvelope response = new MessageEnvelope(
                MessageType.BUBBLE_UPDATED,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);

        MessageEnvelope event = new MessageEnvelope(
                MessageType.BUBBLE_UPDATED,
                null,
                jsonCodec.toPayload(payload)
        );
        messageSender.broadcastExcept(connection, event);
    }

    public void delete(ClientConnection connection, MessageEnvelope message) {
        BubbleDeleteRequest request = jsonCodec.fromPayload(
                message.payload(),
                BubbleDeleteRequest.class
        );

        User user = connection.requireAuthenticatedUser();

        Bubble bubble = bubbleService.delete(user, request.bubbleId());

        BubbleRemovedEvent payload = new BubbleRemovedEvent(bubble.getId(), BubbleStatus.DELETED);
        String jsonPayload = jsonCodec.toPayload(payload);

        MessageEnvelope response = new MessageEnvelope(
                MessageType.BUBBLE_REMOVED,
                message.requestId(),
                jsonPayload
        );
        messageSender.send(connection, response);

        MessageEnvelope event = new MessageEnvelope(
                MessageType.BUBBLE_REMOVED,
                null,
                jsonPayload
        );
        messageSender.broadcastExcept(connection, event);
    }

    public void react(ClientConnection connection, MessageEnvelope message) {
        BubbleReactionRequest request = jsonCodec.fromPayload(
                message.payload(),
                BubbleReactionRequest.class
        );

        connection.requireAuthenticatedUser();

        Bubble bubble = bubbleService.react(request.bubbleId());

        BubbleReactionEvent payload = new BubbleReactionEvent(bubble.getId(), bubble.getReactionCnt());

        MessageEnvelope response = new MessageEnvelope(
                MessageType.BUBBLE_REACTED,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);

        MessageEnvelope event = new MessageEnvelope(
                MessageType.BUBBLE_REACTED,
                null,
                jsonCodec.toPayload(payload)
        );
        messageSender.broadcastExcept(connection, event);
    }
}