package bubbling.server.store;

import bubbling.common.enums.GameType;
import bubbling.server.domain.GameResult;
import bubbling.server.domain.User;

import java.util.List;

public class GameResultStore extends BaseStore<GameResult> {

    public GameResultStore() {
        super(GameResult::getId);
    }

    public List<GameResult> findByUser(User user) {
        return findAll()
                .stream()
                .filter(gr -> gr.getUser().equals(user))
                .toList();
    }

    public List<GameResult> findByGameType(GameType gameType) {
        return findAll()
                .stream()
                .filter(gr -> gr.getType() == gameType)
                .toList();
    }
}