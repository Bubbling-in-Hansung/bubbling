package bubbling.server.service;

import bubbling.server.store.BubbleReactionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BubbleReactionServiceTest {

    private BubbleReactionStore bubbleReactionStore;
    private BubbleReactionService bubbleReactionService;

    @BeforeEach
    void setUp() {
        bubbleReactionStore = new BubbleReactionStore();
        bubbleReactionService = new BubbleReactionService(bubbleReactionStore);
    }

    @Test
    @DisplayName("리액션 성공")
    void react() {

    }

    @Test
    @DisplayName("1분 이내에 리액션 요청")
    void reactInvalidPeriod() {

    }
}