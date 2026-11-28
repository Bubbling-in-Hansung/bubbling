package bubbling.client.ui.game;

import bubbling.client.ui.dialog.NotificationCards;
import bubbling.client.ui.model.CharacterChoice;
import bubbling.client.ui.model.UiSession;
import bubbling.common.dto.game.GameRankingResponse;
import bubbling.common.enums.GameRankingType;
import bubbling.common.enums.GameType;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.util.Objects;

import static bubbling.client.ui.style.Styles.*;

/** Coordinates card navigation, active game timers, ranking views, and notifications inside GameFrame. */
public final class GameView extends JPanel {
    private static final String LOBBY = "LOBBY";
    private static final String GUIDE = "GUIDE";
    private static final String RANKING = "RANKING";
    private static final String DINO = "DINO";
    private static final String CATCH = "CATCH";

    private final CardLayout cards = new CardLayout();
    private final UiSession session;
    private final Runnable onCloseWindow;
    private final GameLobbyPanel lobbyPanel;
    private final GameGuidePanel guidePanel;
    private final GameRankingPanel rankingPanel;
    private final DinoRunGamePanel dinoPanel;
    private final CatchFavoriteGamePanel catchPanel;

    private CharacterChoice selectedCharacter;
    private GameType activeGame;

    public GameView(UiSession session) {
        this(session, () -> {});
    }

    public GameView(UiSession session, Runnable onCloseWindow) {
        this.session = Objects.requireNonNull(session);
        this.onCloseWindow = Objects.requireNonNull(onCloseWindow);
        setLayout(cards);
        selectedCharacter = CharacterChoice.at(session.character());

        lobbyPanel = new GameLobbyPanel(
                session,
                this::startGame,
                this::showGuide,
                this::showRanking,
                this::selectCharacter,
                this::closeWindow);
        guidePanel = new GameGuidePanel(this::showLobby, this::startGame, this::closeWindow);
        rankingPanel = new GameRankingPanel(session, this::showLobby, this::startGame, this::closeWindow);
        dinoPanel = new DinoRunGamePanel(session, () -> showRanking(GameType.DINOSAUR_RUN),
                this::showLobby, this::closeWindow);
        catchPanel = new CatchFavoriteGamePanel(session, this::selectCharacter,
                () -> showRanking(GameType.CATCH_FAVORITE), this::showLobby, this::closeWindow);

        lobbyPanel.setSelectedCharacter(selectedCharacter);
        catchPanel.setCharacter(selectedCharacter);

        add(lobbyPanel, LOBBY);
        add(guidePanel, GUIDE);
        add(rankingPanel, RANKING);
        add(dinoPanel, DINO);
        add(catchPanel, CATCH);
        cards.show(this, LOBBY);
    }

    static JButton exitButton(Runnable onClose) {
        JButton exit = button("게임 종료", false, onClose);
        exit.putClientProperty("flat", true);
        exit.putClientProperty("soft", true);
        exit.putClientProperty("softHover", color("f9dce5"));
        exit.setBackground(DANGER_BG);
        exit.setForeground(DANGER_INK);
        exit.getAccessibleContext().setAccessibleName("게임 종료");
        return exit;
    }

    public void closeWindow() {
        stopAllGames();
        onCloseWindow.run();
    }

    public void showLobby() {
        stopAllGames();
        activeGame = null;
        lobbyPanel.refreshBestScores();
        cards.show(this, LOBBY);
    }

    public void showGuide(GameType focusGame) {
        stopAllGames();
        activeGame = null;
        guidePanel.showGame(focusGame);
        cards.show(this, GUIDE);
    }

    public void showRanking(GameType focusGame) {
        stopAllGames();
        activeGame = null;
        cards.show(this, RANKING);
        rankingPanel.open(focusGame);
    }

    public void refreshBestScores() {
        session.requestGameRanking(GameRankingType.MAX, GameType.DINOSAUR_RUN, 1000);
        session.requestGameRanking(GameRankingType.MAX, GameType.CATCH_FAVORITE, 1000);
    }

    public void onRankingLoaded(GameType gameType, GameRankingResponse response) {
        lobbyPanel.refreshBestScores();
        dinoPanel.syncBestScore();
        catchPanel.syncBestScore();
        rankingPanel.applyRanking(gameType, response);
    }

    public void startGame(GameType gameType) {
        stopAllGames();
        activeGame = Objects.requireNonNull(gameType);
        if (gameType == GameType.DINOSAUR_RUN) {
            cards.show(this, DINO);
            dinoPanel.syncBestScore();
            dinoPanel.startNewRound();
        } else {
            cards.show(this, CATCH);
            catchPanel.syncBestScore();
            catchPanel.startNewRound(selectedCharacter);
        }
    }

    public void selectCharacter(CharacterChoice choice) {
        selectedCharacter = choice == null ? CharacterChoice.BUNNY : choice;
        lobbyPanel.setSelectedCharacter(selectedCharacter);
        catchPanel.setCharacter(selectedCharacter);
    }

    public void syncProfileCharacter(CharacterChoice choice) {
        if (!dinoPanel.isRunning() && !catchPanel.isRunning()) {
            selectCharacter(choice);
        }
    }

    public void onResultSaved() {
        lobbyPanel.refreshBestScores();
        if (activeGame == GameType.DINOSAUR_RUN) {
            dinoPanel.markResultSaved();
            dinoPanel.syncBestScore();
        } else if (activeGame == GameType.CATCH_FAVORITE) {
            catchPanel.markResultSaved();
            catchPanel.syncBestScore();
        }
        NotificationCards.info(this, "점수 저장 완료", "게임 결과가 서버에 기록되었어요.");
    }

    public void stopAllGames() {
        dinoPanel.stopGame();
        catchPanel.stopGame();
    }
}
