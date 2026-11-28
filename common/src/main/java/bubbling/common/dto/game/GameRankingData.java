package bubbling.common.dto.game;

import bubbling.common.dto.user.UserDto;
import bubbling.common.enums.GameType;

public record GameRankingData(
        int rank,
        UserDto user,
        GameType type,
        int score,
        String createdAt
) {}
