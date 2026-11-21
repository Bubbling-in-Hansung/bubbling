package bubbling.server.store;

import bubbling.common.enums.BubbleStatus;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.BubbleExceptionCode;

import java.util.Optional;

public class BubbleStore extends BaseStore<Bubble> {

    public BubbleStore() {
        super(Bubble::getId);
    }

    @Override
    protected void validateSave(Bubble bubble) {
        if (findByUserAndStatus(
                bubble.getOwner(),
                BubbleStatus.ALIVE
        ).isPresent()) {
            throw new BaseException(
                    BubbleExceptionCode.ALREADY_EXISTS
            );
        }
    }

    public Optional<Bubble> findByUserAndStatus(User user, BubbleStatus status) {
        return findAll().stream()
                .filter(bubble -> bubble.getOwner().equals(user) && bubble.getStatus() == status)
                .findFirst();
    }
}
