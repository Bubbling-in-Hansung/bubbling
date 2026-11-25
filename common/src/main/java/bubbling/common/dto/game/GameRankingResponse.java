package bubbling.common.dto.game;

import bubbling.common.dto.user.UserDto;

public record GameRankingResponse(
        int rank,
        UserDto user,
        int score
) {}
