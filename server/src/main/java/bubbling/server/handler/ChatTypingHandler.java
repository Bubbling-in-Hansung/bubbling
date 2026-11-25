package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.chat.ChatTypingDto;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.domain.ChatMember;
import bubbling.server.domain.ChatRoom;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatRoomExceptionCode;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;
import bubbling.server.service.chat.ChatMemberService;
import bubbling.server.service.chat.ChatRoomService;

import java.util.List;

public class ChatTypingHandler {

    private final ChatRoomService chatRoomService;
    private final ChatMemberService chatMemberService;

    private final MessageSender messageSender;
    private final JsonCodec jsonCodec = new JsonCodec();

    public ChatTypingHandler(
            ChatRoomService chatRoomService,
            ChatMemberService chatMemberService,
            MessageSender messageSender
    ) {
        this.chatRoomService = chatRoomService;
        this.chatMemberService = chatMemberService;
        this.messageSender = messageSender;
    }

    public void typing(ClientConnection connection, MessageEnvelope message) {
        ChatTypingDto dto = jsonCodec.fromPayload(
                message.payload(),
                ChatTypingDto.class
        );

        User user = connection.requireAuthenticatedUser();

        ChatRoom chatRoom = chatRoomService.findById(dto.chatRoomId());

        ChatMember chatMember = chatMemberService.findByChatRoomAndUserAndIsActive(chatRoom, user);

        if (!chatMember.getId().equals(dto.chatMemberId())) {
            throw new BaseException(ChatRoomExceptionCode.NOT_JOINED);
        }

        MessageEnvelope event = new MessageEnvelope(
                MessageType.CHAT_TYPING_EVENT,
                null,
                jsonCodec.toPayload(dto)
        );
        List<User> users = chatMemberService
                .findAllActiveJoinMemberByChatRoom(chatRoom)
                .stream()
                .map(ChatMember::getUser)
                .filter(u -> !u.equals(user))
                .toList();
        messageSender.sendAllTarget(users, event);
    }
}
