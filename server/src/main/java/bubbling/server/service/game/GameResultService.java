package bubbling.server.service.game;

import bubbling.common.enums.GameType;
import bubbling.server.domain.GameResult;
import bubbling.server.domain.User;
import bubbling.server.store.GameResultStore;

public class GameResultService {

    private final GameResultStore store;

    public GameResultService(GameResultStore gameResultStore) {
        this.store = gameResultStore;
    }

    public GameResult create(
            User user,
            GameType gameType,
            int score
    ) {
        GameResult gameResult = GameResult.create(user, gameType, score);

        return store.save(gameResult);
    }
}
