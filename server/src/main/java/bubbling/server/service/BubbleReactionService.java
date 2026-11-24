package bubbling.server.service;

import bubbling.server.domain.Bubble;
import bubbling.server.domain.BubbleReaction;
import bubbling.server.domain.User;
import bubbling.server.store.BubbleReactionStore;

import java.util.Optional;

public class BubbleReactionService {

    private final BubbleReactionStore store;

    public BubbleReactionService(BubbleReactionStore bubbleStore) {
        this.store = bubbleStore;
    }

    private BubbleReaction create(User user, Bubble bubble) {
        BubbleReaction bubbleReaction = BubbleReaction.create(bubble, user);
        store.save(bubbleReaction);
        return bubbleReaction;
    }

    public void react(User user, Bubble bubble) {
        Optional<BubbleReaction> bubbleReaction = store.findByUserAndBubble(user, bubble);

        if (bubbleReaction.isEmpty()) {
            create(user, bubble);
        } else {
            bubbleReaction.get().react();
        }
    }
}
