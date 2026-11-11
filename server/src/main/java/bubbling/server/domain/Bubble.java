package bubbling.server.domain;

import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleMethod;
import bubbling.common.enums.BubbleStatus;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Bubble {
    private final String id = UUID.randomUUID().toString();
    private final User owner;
    private String title;
    private BubbleCategory category;
    private BubbleMethod method;
    private int maxMemberCnt;
    private boolean isOpen;
    private BubbleStatus status = BubbleStatus.ALIVE;
    private int reactionCnt = 0;
    private final LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt;

    private Bubble(User owner, String title, BubbleCategory category, BubbleMethod method, int maxMemberCnt, boolean isOpen, int expireMinute) {
        this.owner = owner;
        this.title = title;
        this.category = category;
        this.method = method;
        this.maxMemberCnt = maxMemberCnt;
        this.isOpen = isOpen;
        this.expiresAt = LocalDateTime.now().plusMinutes(expireMinute);
    }

    public static Bubble create(User owner, String title, BubbleCategory category, BubbleMethod method, int maxMemberCnt, boolean isOpen, int expireMinute) {
        validateTitle(title);
        Objects.requireNonNull(owner, "작성자는 필수입니다.");
        Objects.requireNonNull(category, "카테고리는 필수입니다.");
        Objects.requireNonNull(method, "대화 방식은 필수입니다.");
        validateMaxMemberCnt(maxMemberCnt);
        validateExpireMinute(expireMinute);
        return new Bubble(owner, title, category, method, maxMemberCnt, isOpen, expireMinute);
    }

    public synchronized void update(String title, BubbleCategory category, BubbleMethod method, int maxMemberCnt, boolean isOpen) {
        validateAlive();
        validateTitle(title);
        validateMaxMemberCnt(maxMemberCnt);
        Objects.requireNonNull(category, "카테고리는 필수입니다.");
        Objects.requireNonNull(method, "대화 방식은 필수입니다.");

        this.title = title;
        this.category = category;
        this.method = method;
        this.maxMemberCnt = maxMemberCnt;
        this.isOpen = isOpen;
    }

    public synchronized void react() {
        validateAlive();
        this.reactionCnt++;
    }

    public synchronized boolean expireIfElapsed(LocalDateTime now) {
        if(status != BubbleStatus.ALIVE) {
            return false;
        }
        if(expiresAt.isAfter(now)) {
            return false;
        }

        this.status = BubbleStatus.EXPIRED;
        return true;
    }

    private static void validateExpireMinute(int expireMinute) {
        if (expireMinute < 10 || expireMinute > 60) {
            throw new IllegalArgumentException("만료 시간은 10분 이상 60분 이하로 설정해야합니다.");
        }
    }

    private static void validateMaxMemberCnt(int maxMemberCnt) {
        if (maxMemberCnt < 2 || maxMemberCnt > 4) {
            throw new IllegalArgumentException("최대 인원은 2 ~ 4명 사이로 설정해야합니다.");
        }
    }

    private static void validateTitle(String title) {
        Objects.requireNonNull(title, "제목은 필수입니다.");
        if(title.isEmpty() || title.length() > 50) {
            throw new IllegalArgumentException("제목은 50자 이내로 작성해야합니다.");
        }
    }

    public synchronized boolean isAlive() {
        return status == BubbleStatus.ALIVE && LocalDateTime.now().isBefore(expiresAt);
    }

    private void validateAlive() {
        if (status != BubbleStatus.ALIVE || LocalDateTime.now().isAfter(expiresAt)) {
            throw new IllegalStateException("만료된 비눗방울입니다.");
        }
    }

    public String getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public BubbleCategory getCategory() {
        return category;
    }

    public BubbleMethod getMethod() {
        return method;
    }

    public int getMaxMemberCnt() {
        return maxMemberCnt;
    }

    public boolean getIsOpen() {
        return isOpen;
    }

    public BubbleStatus getStatus() {
        return status;
    }

    public int getReactionCnt() {
        return reactionCnt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
