package bubbling.server.fixture;

import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleMethod;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.User;

public final class BubbleFixtures {

    private BubbleFixtures() {}

    private static Bubble createBubble(User owner, int maxMemberCnt, boolean isOpen) {
        return Bubble.create(
                owner,
                "테스트 비눗방울",
                BubbleCategory.CAREER,
                BubbleMethod.LISTEN,
                maxMemberCnt,
                isOpen,
                30
        );
    }

    public static Bubble createOpenBubble(User owner) {
        return createBubble(owner, 4, true);
    }

    public static Bubble createPrivateBubble(User owner) {
        return createBubble(owner, 4, false);
    }

    public static Bubble createBubbleWithMaxMemberCnt(User owner, int maxMemberCnt) {
        return createBubble(owner, maxMemberCnt, true);
    }
}
