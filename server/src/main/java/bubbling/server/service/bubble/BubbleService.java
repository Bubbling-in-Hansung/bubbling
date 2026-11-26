package bubbling.server.service.bubble;

import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleMethod;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.BubbleExceptionCode;
import bubbling.server.service.chat.ChatRequestService;
import bubbling.server.service.chat.ChatRoomService;
import bubbling.server.store.BubbleStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BubbleService {

    private final BubbleStore store;
    private final BubbleReactionService bubbleReactionService;
    private final ChatRoomService chatRoomService;
    private final ChatRequestService chatRequestService;

    public BubbleService(
            BubbleStore bubbleStore,
            BubbleReactionService bubbleReactionService,
            ChatRoomService chatRoomService,
            ChatRequestService chatRequestService
    ) {
        this.store = bubbleStore;
        this.bubbleReactionService = bubbleReactionService;
        this.chatRoomService = chatRoomService;
        this.chatRequestService = chatRequestService;
    }

    public Bubble create(
            User owner,
            String title,
            BubbleCategory category,
            BubbleMethod method,
            int maxMemberCnt,
            boolean isOpen,
            int expireMinute
    ) {
        Bubble bubble = Bubble.create(
                owner,
                title,
                category,
                method,
                maxMemberCnt,
                isOpen,
                expireMinute
        );
        store.save(bubble);

        try {
            chatRoomService.create(bubble);
        } catch (RuntimeException e) {
            store.delete(bubble.getId());
            throw new BaseException(BubbleExceptionCode.FAILED_TO_CREATION);
        }

        return bubble;
    }

    public Bubble findById(String id) {
        return store.findById(id).orElseThrow(
                () -> new BaseException(BubbleExceptionCode.NOT_FOUND)
        );
    }

    public List<Bubble> findAllByAlive() {
        return store.findAll()
                .stream()
                .filter(Bubble::isAlive)
                .toList();
    }

    public Bubble update(
            User requester,
            String bubbleId,
            String title,
            BubbleCategory category,
            BubbleMethod method,
            int maxMemberCnt,
            boolean isOpen,
            int expireMinute
    ) {
        Bubble bubble = findById(bubbleId);

        if (!bubble.getOwner().equals(requester)) {
            throw new BaseException(BubbleExceptionCode.FORBIDDEN);
        }

        bubble.update(
                title,
                category,
                method,
                maxMemberCnt,
                isOpen,
                expireMinute
        );

        return bubble;
    }

    public List<Bubble> expireElapsed(LocalDateTime now) {
        List<Bubble> bubbles = new ArrayList<>();
        for (Bubble bubble : store.findAll()) {
            if(bubble.expireIfElapsed(now)) {
                bubbles.add(bubble);
                chatRequestService.cancel(bubble);
            }
        }

        return bubbles;
    }

    public Bubble react(User requester, String id) {
        Bubble bubble = findById(id);

        if(bubble.getOwner().equals(requester)) {
            throw new BaseException(BubbleExceptionCode.NOT_ALLOWED_TO_OWNER);
        }

        bubbleReactionService.react(requester, bubble);
        bubble.react();

        return bubble;
    }

    public Bubble delete(User user, String id) {
        Bubble bubble = findById(id);

        if(!bubble.getOwner().equals(user)) {
            throw new BaseException(BubbleExceptionCode.FORBIDDEN);
        }

        chatRoomService.deleteByBubble(bubble);
        chatRequestService.deleteAllByBubble(bubble);
        store.delete(bubble.getId());

        return bubble;
    }
}
