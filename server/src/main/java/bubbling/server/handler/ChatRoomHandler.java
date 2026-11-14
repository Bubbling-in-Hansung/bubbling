package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.chat.*;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.ChatMember;
import bubbling.server.domain.ChatRoom;
import bubbling.server.domain.User;
import bubbling.server.mapper.ChatParticipantMapper;
import bubbling.server.mapper.ChatRoomMapper;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;
import bubbling.server.service.BubbleService;
import bubbling.server.service.ChatMemberService;
import bubbling.server.service.ChatRoomService;

import java.util.List;

public class ChatRoomHandler {

    private final BubbleService bubbleService;
    private final ChatRoomService chatRoomService;
    private final ChatMemberService chatMemberService;

    private final MessageSender messageSender;
    private final JsonCodec jsonCodec = new JsonCodec();

    public ChatRoomHandler(
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

    public void join(ClientConnection connection, MessageEnvelope message) {
        ChatJoinRequest request = jsonCodec.fromPayload(
                message.payload(),
                ChatJoinRequest.class
        );

        User user = connection.requireAuthenticatedUser();

        Bubble bubble = bubbleService.findById(request.bubbleId());

        ChatMember chatMember = chatRoomService.join(user, bubble);

        ChatRoom chatRoom = chatMember.getChatRoom();
        int count = chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom);

        // join response
        List<ChatParticipantDto> chatParticipants = chatMemberService.findAllActiveJoinMemberByChatRoom(chatRoom)
                .stream().map(ChatParticipantMapper::toDto).toList();
        ChatRoomDto payload = ChatRoomMapper.toDto(
                chatRoom, count, chatMember.getId(), chatParticipants
        );
        MessageEnvelope response = new MessageEnvelope(
                MessageType.CHAT_JOINED,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);

        // count event
        ChatMemberCountEvent eventPayload = new ChatMemberCountEvent(
                bubble.getId(),
                count
        );
        MessageEnvelope event = new MessageEnvelope(
                MessageType.CHAT_MEMBER_COUNT_CHANGED,
                null,
                jsonCodec.toPayload(eventPayload)
        );
        messageSender.broadcast(event);

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
        List<User> users = chatMemberService.findAllActiveJoinMemberByChatRoom(chatRoom).stream()
                .map(ChatMember::getUser)
                .filter(u -> !u.equals(user))
                .toList();
        messageSender.sendAllTarget(users, joinEvent);
    }

    public void leave(ClientConnection connection, MessageEnvelope message) {
        ChatLeaveRequest request = jsonCodec.fromPayload(
                message.payload(),
                ChatLeaveRequest.class
        );

        User user = connection.requireAuthenticatedUser();

        Bubble bubble = bubbleService.findById(request.bubbleId());
        ChatRoom chatRoom = chatRoomService.findByBubble(bubble);
        ChatMember chatMember = chatRoomService.leave(user, chatRoom);

        int count = chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom);

        // leave response
        ChatLeftResponse payload = new ChatLeftResponse(
                chatRoom.getId(),
                bubble.getId(),
                count
        );
        MessageEnvelope response = new MessageEnvelope(
                MessageType.CHAT_LEFT,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);

        // count event
        MessageEnvelope event = new MessageEnvelope(
                MessageType.CHAT_MEMBER_COUNT_CHANGED,
                null,
                jsonCodec.toPayload(
                        new ChatMemberCountEvent(
                                chatRoom.getBubble().getId(),
                                count
                        )
                )
        );
        messageSender.broadcast(event);

        // left event
        ChatMemberEvent dto = new ChatMemberEvent(
                chatRoom.getId(),
                ChatParticipantMapper.toDto(chatMember)
        );
        MessageEnvelope joinEvent = new MessageEnvelope(
                MessageType.CHAT_MEMBER_LEFT,
                null,
                jsonCodec.toPayload(dto)
        );
        List<User> users = chatMemberService.findAllActiveJoinMemberByChatRoom(chatRoom).stream()
                .map(ChatMember::getUser)
                .filter(u -> !u.equals(user))
                .toList();
        messageSender.sendAllTarget(users, joinEvent);
    }

    public void end(ClientConnection connection, MessageEnvelope message) {
        ChatEndRequest request = jsonCodec.fromPayload(
                message.payload(),
                ChatEndRequest.class
        );

        User user = connection.requireAuthenticatedUser();

        ChatRoom chatRoom = chatRoomService.end(user, request.roomId());

        ChatEndResponse payload = new ChatEndResponse(
                chatRoom.getId(),
                chatRoom.getBubble().getId()
        );

        // end response
        MessageEnvelope response = new MessageEnvelope(
                MessageType.CHAT_ENDED,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);

        // end event
        MessageEnvelope event = new MessageEnvelope(
                MessageType.CHAT_ENDED,
                null,
                jsonCodec.toPayload(payload)
        );
        messageSender.broadcastExcept(connection, event);
    }

    public void list(ClientConnection connection, MessageEnvelope message) {
        User user = connection.requireAuthenticatedUser();

        List<ChatMember> myMembers = chatMemberService.findAllActiveJoinMemberByUser(user);
        List<ChatRoomDto> payload = myMembers
                .stream()
                .map(myMember -> {
                    ChatRoom chatRoom = myMember.getChatRoom();
                    List<ChatMember> members = chatMemberService.findAllActiveJoinMemberByChatRoom(chatRoom);
                    List<ChatParticipantDto> participants = members.stream()
                            .map(ChatParticipantMapper::toDto)
                            .toList();
                    return ChatRoomMapper.toDto(
                            chatRoom,
                            members.size(),
                            myMember.getId(),
                            participants
                    );
                })
                .toList();
        MessageEnvelope response = new MessageEnvelope(
                MessageType.MY_CHAT_ROOM_LIST,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);
    }
}
