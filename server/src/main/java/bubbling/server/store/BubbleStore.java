package bubbling.server.store;

import bubbling.common.enums.BubbleStatus;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.User;

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
            throw new IllegalStateException("사용자는 한 번에 최대 한 개의 비눗방울을 생성할 수 있습니다.");
        }

        if (findById(bubble.getId()).isPresent()) {
            throw new IllegalStateException("이미 존재하는 비눗방울 아이디입니다.");
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
