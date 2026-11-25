package bubbling.server.domain;

import bubbling.common.enums.GameType;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.GameExceptionCode;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class GameResult {
    private final String id = UUID.randomUUID().toString();
    private final User user;
    private final GameType type;
    private final int score;
    private final LocalDateTime createdAt = LocalDateTime.now();

    private GameResult(User user, GameType type, int score) {
        this.user = user;
        this.type = type;
        this.score = score;
    }

    public static GameResult create(User user, GameType type, int score) {
        Objects.requireNonNull(user, "사용자는 필수입니다.");
        Objects.requireNonNull(type, "게임 종류는 필수입니다.");
        validateScore(score);
        return new GameResult(user, type, score);
    }

    private static void validateScore(int score) {
        if (score < 0) {
            throw new BaseException(GameExceptionCode.NEGATIVE_SCORE);
        }
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public GameType getType() {
        return type;
    }

    public int getScore() {
        return score;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameResult that = (GameResult) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
