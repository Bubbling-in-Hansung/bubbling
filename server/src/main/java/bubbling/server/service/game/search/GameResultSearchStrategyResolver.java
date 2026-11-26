package bubbling.server.service.game.search;

import bubbling.common.enums.GameRankingType;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.GameExceptionCode;
import bubbling.server.store.GameResultStore;

import java.util.List;

public class GameResultSearchStrategyResolver {

    private final List<GameResultSearchStrategy> strategies;

    public GameResultSearchStrategyResolver(GameResultStore store) {
        strategies = List.of(
                new SearchAverageStrategy(store),
                new SearchMaxStrategy(store),
                new SearchSumStrategy(store)
        );
    }

    public GameResultSearchStrategy resolve(GameRankingType type) {
        return strategies
                .stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new BaseException(GameExceptionCode.INVALID_SEARCH_CONDITION));
    }
}
