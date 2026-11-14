package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.chat.ChatMessageDto;
import bubbling.common.dto.chat.ChatMessageRequest;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.domain.ChatMember;
import bubbling.server.domain.ChatMessage;
import bubbling.server.domain.ChatRoom;
import bubbling.server.domain.User;
import bubbling.server.mapper.ChatMessageMapper;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;
import bubbling.server.service.ChatMemberService;
import bubbling.server.service.ChatMessageService;
import bubbling.server.service.ChatRoomService;

import java.util.List;

public class ChatMessageHandler {

    private final ChatRoomService chatRoomService;
    private final ChatMemberService chatMemberService;
    private final ChatMessageService chatMessageService;

    private final MessageSender messageSender;
    private final JsonCodec jsonCodec = new JsonCodec();

    public ChatMessageHandler(
            ChatRoomService chatRoomService,
            ChatMemberService chatMemberService,
            ChatMessageService chatMessageService,
            MessageSender messageSender
    ) {
        this.chatRoomService = chatRoomService;
        this.chatMemberService = chatMemberService;
        this.chatMessageService = chatMessageService;
        this.messageSender = messageSender;
    }

    public void message(ClientConnection connection, MessageEnvelope message) {
        ChatMessageRequest request = jsonCodec.fromPayload(
                message.payload(),
                ChatMessageRequest.class
        );

        User user = connection.requireAuthenticatedUser();

        ChatRoom chatRoom = chatRoomService.findById(request.chatRoomId());
        ChatMember chatMember = chatMemberService.findByChatRoomAndUserAndIsActive(chatRoom, user);
        ChatMessage chatMessage = chatMessageService.create(
                chatRoom,
                chatMember,
                request.content()
        );

        // message response
        ChatMessageDto payload = ChatMessageMapper.toDto(chatMessage);
        MessageEnvelope response = new MessageEnvelope(
                MessageType.CHAT_MESSAGE,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);

        // message event
        MessageEnvelope event = new MessageEnvelope(
                MessageType.CHAT_MESSAGE,
                null,
                jsonCodec.toPayload(payload)
        );
        List<User> users = chatMemberService.findAllActiveJoinMemberByChatRoom(chatRoom).stream()
                .map(ChatMember::getUser)
                .filter(u -> !u.equals(user))
                .toList();
        messageSender.sendAllTarget(users, event);
    }
}
