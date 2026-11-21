package bubbling.server.mapper;

import bubbling.common.dto.chat.ChatRequestDto;
import bubbling.common.enums.ChatRequestRole;
import bubbling.server.domain.ChatRequest;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatRequestExceptionCode;

public class ChatRequestMapper {
    private ChatRequestMapper() {}

    public static ChatRequestDto toDto(
            ChatRequest chatRequest,
            User user
    ) {
        boolean isOwner = chatRequest.getBubble().getOwner().getId().equals(user.getId());
        boolean isRequester = chatRequest.getRequester().getId().equals(user.getId());

        if (!isOwner && !isRequester) {
            throw new BaseException(ChatRequestExceptionCode.FORBIDDEN);
        }

        return new ChatRequestDto(
                chatRequest.getId(),
                chatRequest.getBubble().getId(),
                chatRequest.getStatus(),
                isOwner ? chatRequest.getRequester().getNickname() : null,
                isOwner ? ChatRequestRole.OWNER : ChatRequestRole.REQUESTER,
                chatRequest.getCreatedAt().toString()
        );
    }
}
