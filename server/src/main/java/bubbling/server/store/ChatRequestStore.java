package bubbling.server.store;

import bubbling.common.enums.ChatRequestStatus;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.ChatRequest;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatRequestExceptionCode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatRequestStore {

    private final ConcurrentMap<String, ChatRequest> requests = new ConcurrentHashMap<>();

    public synchronized ChatRequest save(ChatRequest request) {
        Objects.requireNonNull(request, "참여 요청은 필수입니다.");

        if (findPendingByBubbleAndRequester(request.getBubble(), request.getRequester()).isPresent()) {
            throw new BaseException(ChatRequestExceptionCode.ALREADY_PENDING);
        }

        requests.put(request.getId(), request);

        return request;
    }

    public Optional<ChatRequest> findById(String id) {
        return Optional.ofNullable(requests.get(id));
    }

    public Optional<ChatRequest> findPendingByBubbleAndRequester(Bubble bubble, User requester) {
        return requests.values().stream()
                .filter(request -> request.getBubble().getId().equals(bubble.getId()))
                .filter(request -> request.getRequester().getId().equals(requester.getId()))
                .filter(request -> request.getStatus() == ChatRequestStatus.PENDING)
                .findFirst();
    }

    public List<ChatRequest> findAllByBubble(Bubble bubble) {
        return requests.values().stream()
                .filter(request -> request.getBubble().getId().equals(bubble.getId()))
                .toList();
    }

    public synchronized void deleteAllByBubble(Bubble bubble) {
        for (ChatRequest request : findAllByBubble(bubble)) {
            requests.remove(request.getId());
        }
    }
}