package bubbling.common.dto.game;

import bubbling.common.enums.GameRankingType;

import java.time.LocalDateTime;
import java.util.List;

public record GameRankingResponse(
        GameRankingType rankingType,
        List<GameRankingData> rankings, // always descending order
        LocalDateTime aggregateAt
) {}
