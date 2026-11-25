package bubbling.common.dto.game;

import bubbling.common.enums.GameType;

public record GameResultRequest(
        String userId,
        int score,
        GameType gameType
) {}
