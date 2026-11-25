package bubbling.server.service.game.search;

import bubbling.common.dto.game.GameRankingData;
import bubbling.common.enums.GameRankingType;
import bubbling.common.enums.GameType;

import java.util.List;

public interface GameResultSearchStrategy {

    boolean supports(GameRankingType type);

    List<GameRankingData> aggregate(GameType type);

}
