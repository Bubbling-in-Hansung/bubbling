package bubbling.client.ui.model;

import bubbling.common.enums.BubbleMethod;

/** Client labels for existing common methods; does not alter wire values. */
public enum ConversationMode {
    TOGETHER(BubbleMethod.TOGETHER, "함께 이야기해요", "편하게 의견을 주고받아요"),
    LISTEN(BubbleMethod.LISTEN, "들어만 주세요", "판단 없이 마음을 들어줘요"),
    SOLUTION(BubbleMethod.SOLUTION, "해결책 알려주세요", "구체적인 방법을 함께 찾아요");

    private final BubbleMethod method;
    private final String label;
    private final String description;

    ConversationMode(BubbleMethod method, String label, String description) {
        this.method = method;
        this.label = label;
        this.description = description;
    }

    public String description() {
        return description;
    }

    public BubbleMethod method() {
        return method;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
