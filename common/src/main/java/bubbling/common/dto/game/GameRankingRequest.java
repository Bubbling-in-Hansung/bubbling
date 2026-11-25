package bubbling.common.dto.game;

import bubbling.common.enums.GameRankingType;
import bubbling.common.enums.GameType;

public record GameRankingRequest(
        GameRankingType rankingType,
        GameType gameType,
        int limit
) {}
