package bubbling.common.enums;

public enum BubbleCategory {
    DAILY("일상"),
    RELATIONSHIP("관계"),
    CAREER("진로"),
    STUDY("학업"),
    FAMILY("가족"),
    LOVE("연애"),
    WORK("직장"),
    EMOTION("감정"),
    ETC("기타");

    private final String label;

    BubbleCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
