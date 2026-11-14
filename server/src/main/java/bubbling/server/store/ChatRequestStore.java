package bubbling.server.store;

import bubbling.common.enums.ChatRequestStatus;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.ChatRequest;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatRequestExceptionCode;

import java.util.List;
import java.util.Optional;

public class ChatRequestStore extends BaseStore<ChatRequest> {

    public ChatRequestStore() {
        super(ChatRequest::getId);
    }

    @Override
    protected void validateSave(ChatRequest request) {
        if (findByBubbleAndRequesterAndStatus(
                request.getBubble(), request.getRequester(), ChatRequestStatus.PENDING
        ).isPresent()) {
            throw new BaseException(ChatRequestExceptionCode.ALREADY_PENDING);
        }

        if (findByBubbleAndRequesterAndStatus(
                request.getBubble(),
                request.getRequester(),
                ChatRequestStatus.REJECTED
        ).isPresent()) {
            throw new BaseException(ChatRequestExceptionCode.NOT_ALLOWED);
        }
    }

    public Optional<ChatRequest> findByBubbleAndRequesterAndStatus(Bubble bubble, User requester, ChatRequestStatus status) {
        return findAll().stream()
                .filter(request -> request.getBubble().getId().equals(bubble.getId()))
                .filter(request -> request.getRequester().getId().equals(requester.getId()))
                .filter(request -> request.getStatus() == status)
                .findFirst();
    }

    public List<ChatRequest> findAllByBubble(Bubble bubble) {
        return findAll().stream()
                .filter(request -> request.getBubble().getId().equals(bubble.getId()))
                .toList();
    }
}