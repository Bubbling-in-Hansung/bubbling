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
        if (findPendingByBubbleAndRequester(request.getBubble(), request.getRequester()).isPresent()) {
            throw new BaseException(ChatRequestExceptionCode.ALREADY_PENDING);
        }
    }

    public Optional<ChatRequest> findPendingByBubbleAndRequester(Bubble bubble, User requester) {
        return findAll().stream()
                .filter(request -> request.getBubble().getId().equals(bubble.getId()))
                .filter(request -> request.getRequester().getId().equals(requester.getId()))
                .filter(request -> request.getStatus() == ChatRequestStatus.PENDING)
                .findFirst();
    }

    public List<ChatRequest> findAllByBubble(Bubble bubble) {
        return findAll().stream()
                .filter(request -> request.getBubble().getId().equals(bubble.getId()))
                .toList();
    }
}