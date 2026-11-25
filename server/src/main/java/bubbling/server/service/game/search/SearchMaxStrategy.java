package bubbling.server.service.game.search;

import bubbling.common.dto.game.GameRankingData;
import bubbling.common.enums.GameRankingType;
import bubbling.common.enums.GameType;
import bubbling.server.domain.GameResult;
import bubbling.server.mapper.UserMapper;
import bubbling.server.store.GameResultStore;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SearchMaxStrategy implements GameResultSearchStrategy {

    private final GameResultStore store;

    public SearchMaxStrategy(GameResultStore store) {
        this.store = store;
    }

    @Override
    public boolean supports(GameRankingType type) {
        return type == GameRankingType.MAX;
    }

    @Override
    public List<GameRankingData> aggregate(GameType type) {
        Objects.requireNonNull(type, "게임 종류는 필수입니다.");
        Map<String, GameResult> group = store.findAll().stream()
                .filter(gr -> gr.getType() == type)
                .collect(Collectors.toMap(
                        gr -> gr.getUser().getId(),
                        Function.identity(),
                        (a, b) -> a.getScore() >= b.getScore() ? a : b
                ));

        List<GameResult> results = group.values().stream()
                .sorted(Comparator.comparingInt(GameResult::getScore).reversed())
                .toList();

        List<GameRankingData> rankings = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            GameResult result = results.get(i);

            rankings.add(new GameRankingData(
                    i + 1,
                    UserMapper.toDto(result.getUser()),
                    result.getType(),
                    result.getScore(),
                    result.getCreatedAt()
            ));
        }

        return rankings;
    }
}
