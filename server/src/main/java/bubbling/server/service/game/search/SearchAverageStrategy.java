package bubbling.server.service.game.search;

import bubbling.common.dto.game.GameRankingData;
import bubbling.common.enums.GameRankingType;
import bubbling.common.enums.GameType;
import bubbling.server.domain.GameResult;
import bubbling.server.mapper.UserMapper;
import bubbling.server.store.GameResultStore;

import java.util.*;
import java.util.stream.Collectors;

public class SearchAverageStrategy implements GameResultSearchStrategy {

    private final GameResultStore store;

    public SearchAverageStrategy(GameResultStore store) {
        this.store = store;
    }

    @Override
    public boolean supports(GameRankingType type) {
        return type == GameRankingType.AVERAGE;
    }

    @Override
    public List<GameRankingData> aggregate(GameType type) {
        Objects.requireNonNull(type, "게임 종류는 필수입니다.");

        Map<String, List<GameResult>> group = store.findAll().stream()
                .filter(gr -> gr.getType() == type)
                .collect(Collectors.groupingBy(
                        gr -> gr.getUser().getId()
                ));

        List<GameRankingData> results = group.values().stream()
                .map(userResults -> {
                    GameResult latest = userResults.stream()
                            .max(Comparator.comparing(GameResult::getCreatedAt))
                            .orElseThrow();

                    int average = (int) Math.round(
                            userResults.stream()
                                    .mapToInt(GameResult::getScore)
                                    .average()
                                    .orElse(0)
                    );

                    return new GameRankingData(
                            0,
                            UserMapper.toDto(latest.getUser()),
                            type,
                            average,
                            latest.getCreatedAt().toString()
                    );
                })
                .sorted(Comparator.comparingInt(GameRankingData::score).reversed())
                .toList();

        List<GameRankingData> rankings = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            GameRankingData data = results.get(i);

            rankings.add(new GameRankingData(
                    i + 1,
                    data.user(),
                    data.type(),
                    data.score(),
                    data.createdAt()
            ));
        }

        return rankings;
    }
}
