package bubbling.server.store;

import bubbling.common.enums.BubbleStatus;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.BubbleExceptionCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BubbleStore {

    private final ConcurrentMap<String, Bubble> bubbles = new ConcurrentHashMap<>();

    public synchronized Bubble save(Bubble bubble) {
        Objects.requireNonNull(bubble, "비눗방울은 필수입니다.");

        if (findByUserAndStatus(bubble.getOwner(), BubbleStatus.ALIVE).isPresent()) {
            throw new BaseException(BubbleExceptionCode.ALREADY_EXISTS);
        }

        bubbles.put(bubble.getId(), bubble);

        return bubble;
    }

    public Optional<Bubble> findById(String id) {
        return Optional.ofNullable(bubbles.get(id));
    }

    public Optional<Bubble> findByUserAndStatus(User user, BubbleStatus status) {
        return bubbles.values().stream()
                .filter(b -> b.getOwner().equals(user) && b.getStatus() == status)
                .findFirst();
    }

    public List<Bubble> findAll() {
        return new ArrayList<>(bubbles.values());
    }

    public synchronized void delete(String id) {
        bubbles.remove(id);
    }
}
