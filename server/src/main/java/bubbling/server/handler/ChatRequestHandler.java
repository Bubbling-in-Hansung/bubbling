package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.chat.*;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.domain.*;
import bubbling.server.mapper.ChatParticipantMapper;
import bubbling.server.mapper.ChatRequestMapper;
import bubbling.server.mapper.ChatRoomMapper;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;
import bubbling.server.service.BubbleService;
import bubbling.server.service.ChatMemberService;
import bubbling.server.service.ChatRequestService;

import java.util.List;

public class ChatRequestHandler {

    private final BubbleService bubbleService;
    private final ChatMemberService chatMemberService;
    private final ChatRequestService chatRequestService;

    private final MessageSender messageSender;
    private final JsonCodec jsonCodec = new JsonCodec();

    public ChatRequestHandler(
            BubbleService bubbleService,
            ChatMemberService chatMemberService,
            ChatRequestService chatRequestService,
            MessageSender messageSender
    ) {
        this.bubbleService = bubbleService;
        this.chatMemberService = chatMemberService;
        this.chatRequestService = chatRequestService;
        this.messageSender = messageSender;
    }

    public void request(ClientConnection connection, MessageEnvelope message) {
        ChatRequestCreateRequest request = jsonCodec.fromPayload(
                message.payload(),
                ChatRequestCreateRequest.class
        );

        User user = connection.requireAuthenticatedUser();

        Bubble bubble = bubbleService.findById(request.bubbleId());

        ChatRequest chatRequest = chatRequestService.create(user, bubble);

        // to requester
        MessageEnvelope response = new MessageEnvelope(
                MessageType.CHAT_REQUESTED,
                message.requestId(),
                jsonCodec.toPayload(ChatRequestMapper.toDto(chatRequest, user))
        );
        messageSender.send(connection, response);

        // to owner
        User owner = bubble.getOwner();
        MessageEnvelope eventResponse = new MessageEnvelope(
                MessageType.CHAT_REQUESTED,
                null,
                jsonCodec.toPayload(ChatRequestMapper.toDto(chatRequest, owner))
        );
        messageSender.sendTarget(owner, eventResponse);
    }

    public void accept(ClientConnection connection, MessageEnvelope message) {
        ChatRequestDecisionRequest request = jsonCodec.fromPayload(
                message.payload(),
                ChatRequestDecisionRequest.class
        );

        User user = connection.requireAuthenticatedUser();

        ChatMember chatMember = chatRequestService.accept(user, request.chatRequestId());
        ChatRequest chatRequest = chatRequestService.findById(request.chatRequestId());
        ChatRoom chatRoom = chatMember.getChatRoom();

        // accept response
        MessageEnvelope response = new MessageEnvelope(
                MessageType.CHAT_ACCEPTED,
                message.requestId(),
                jsonCodec.toPayload(ChatRequestMapper.toDto(chatRequest, user))
        );
        messageSender.send(connection, response);

        // requester response
        List<ChatMember> members = chatMemberService.findAllActiveJoinMemberByChatRoom(chatRoom);
        List<ChatParticipantDto> participants = members.stream()
                .map(ChatParticipantMapper::toDto)
                .toList();
        ChatRoomDto roomDto = ChatRoomMapper.toDto(
                chatRoom,
                members.size(),
                chatMember.getId(),
                participants
        );
        MessageEnvelope event = new MessageEnvelope(
                MessageType.CHAT_JOINED,
                null,
                jsonCodec.toPayload(roomDto)
        );
        messageSender.sendTarget(chatMember.getUser(), event);

        // count event
        MessageEnvelope countEvent = new MessageEnvelope(
                MessageType.CHAT_MEMBER_COUNT_CHANGED,
                null,
                jsonCodec.toPayload(
                        new ChatMemberCountEvent(
                            chatRoom.getBubble().getId(),
                            chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom)
                        )
                )
        );
        messageSender.broadcast(countEvent);

        // join event
        ChatMemberEvent joinPayload = new ChatMemberEvent(
                chatRoom.getId(),
                ChatParticipantMapper.toDto(chatMember)
        );
        MessageEnvelope joinEvent = new MessageEnvelope(
                MessageType.CHAT_MEMBER_JOINED,
                null,
                jsonCodec.toPayload(joinPayload)
        );
        List<User> users = members.stream()
                .map(ChatMember::getUser)
                .filter(u -> !u.equals(chatMember.getUser()))
                .toList();
        messageSender.sendAllTarget(users, joinEvent);
    }

    public void reject(ClientConnection connection, MessageEnvelope message) {
        ChatRequestDecisionRequest request = jsonCodec.fromPayload(
                message.payload(),
                ChatRequestDecisionRequest.class
        );

        User user = connection.requireAuthenticatedUser();

        ChatRequest chatRequest = chatRequestService.reject(user, request.chatRequestId());

        // reject response
        MessageEnvelope response = new MessageEnvelope(
                MessageType.CHAT_REJECTED,
                message.requestId(),
                jsonCodec.toPayload(ChatRequestMapper.toDto(chatRequest, user))
        );
        messageSender.send(connection, response);

        // requester response
        MessageEnvelope event = new MessageEnvelope(
                MessageType.CHAT_REJECTED,
                null,
                jsonCodec.toPayload(ChatRequestMapper.toDto(chatRequest, chatRequest.getRequester()))
        );
        messageSender.sendTarget(chatRequest.getRequester(), event);
    }
}
