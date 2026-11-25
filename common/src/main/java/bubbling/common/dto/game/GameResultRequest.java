package bubbling.common.dto.game;

import bubbling.common.enums.GameType;

public record GameResultRequest(
        GameType gameType,
        int score
) {}
