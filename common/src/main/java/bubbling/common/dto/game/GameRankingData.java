package bubbling.common.dto.game;

import bubbling.common.dto.user.UserDto;
import bubbling.common.enums.GameType;

import java.time.LocalDateTime;

public record GameRankingData(
        int rank,
        UserDto user,
        GameType type,
        int score,
        LocalDateTime createdAt
) {}
