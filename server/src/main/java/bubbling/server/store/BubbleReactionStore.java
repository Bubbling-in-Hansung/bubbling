package bubbling.server.store;

import bubbling.server.domain.Bubble;
import bubbling.server.domain.BubbleReaction;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.BubbleExceptionCode;

import java.util.Optional;

public class BubbleReactionStore extends BaseStore<BubbleReaction> {

    public BubbleReactionStore() {
        super(BubbleReaction::getId);
    }

    @Override
    protected void validateSave(BubbleReaction bubbleReaction) {
        if(findByUserAndBubble(bubbleReaction.getUser(), bubbleReaction.getBubble()).isPresent()) {
            throw new BaseException(BubbleExceptionCode.ALREADY_EXISTS_REACTION);
        }
    }

    public Optional<BubbleReaction> findByUserAndBubble(User user, Bubble bubble) {
        return findAll().stream()
                .filter(br -> br.getUser().equals(user))
                .filter(br -> br.getBubble().equals(bubble))
                .findFirst();
    }
}
