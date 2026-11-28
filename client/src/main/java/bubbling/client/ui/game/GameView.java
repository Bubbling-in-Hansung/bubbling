package bubbling.client.ui.game;

import bubbling.client.ui.dialog.NotificationCards;
import bubbling.client.ui.dialog.UiErrors;
import bubbling.client.ui.model.CharacterChoice;
import bubbling.client.ui.model.UiSession;
import bubbling.common.dto.system.ExceptionResponse;
import bubbling.common.enums.GameType;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.util.Objects;

import static bubbling.client.ui.style.Styles.*;

/** Coordinates card navigation, active game timers, and notifications inside GameFrame. */
public final class GameView extends JPanel {
    private static final String LOBBY = "LOBBY";
    private static final String GUIDE = "GUIDE";
    private static final String DINO = "DINO";
    private static final String CATCH = "CATCH";

    private final CardLayout cards = new CardLayout();
    private final Runnable onCloseWindow;
    private final GameLobbyPanel lobbyPanel;
    private final GameGuidePanel guidePanel;
    private final DinoRunGamePanel dinoPanel;
    private final CatchFavoriteGamePanel catchPanel;

    private CharacterChoice selectedCharacter;
    private GameType activeGame;

    public GameView(UiSession session) {
        this(session, () -> {});
    }

    public GameView(UiSession session, Runnable onCloseWindow) {
        Objects.requireNonNull(session);
        this.onCloseWindow = Objects.requireNonNull(onCloseWindow);
        setLayout(cards);
        selectedCharacter = CharacterChoice.at(session.character());

        lobbyPanel = new GameLobbyPanel(
                this::startGame,
                this::showGuide,
                this::showRankingNotImplemented,
                this::selectCharacter,
                this::closeWindow);
        guidePanel = new GameGuidePanel(this::showLobby, this::startGame, this::closeWindow);
        dinoPanel = new DinoRunGamePanel(session, this::showRankingNotImplemented,
                this::showLobby, this::closeWindow);
        catchPanel = new CatchFavoriteGamePanel(session, this::selectCharacter,
                this::showRankingNotImplemented, this::showLobby, this::closeWindow);

        lobbyPanel.setSelectedCharacter(selectedCharacter);
        catchPanel.setCharacter(selectedCharacter);

        add(lobbyPanel, LOBBY);
        add(guidePanel, GUIDE);
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
        cards.show(this, LOBBY);
    }

    public void showGuide(GameType focusGame) {
        stopAllGames();
        activeGame = null;
        guidePanel.showGame(focusGame);
        cards.show(this, GUIDE);
    }

    public void startGame(GameType gameType) {
        stopAllGames();
        activeGame = Objects.requireNonNull(gameType);
        if (gameType == GameType.DINOSAUR_RUN) {
            cards.show(this, DINO);
            dinoPanel.startNewRound();
        } else {
            cards.show(this, CATCH);
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

    public void showRankingNotImplemented() {
        UiErrors.show(this, new ExceptionResponse(null, "미구현 된 내용입니다."));
    }

    public void onResultSaved() {
        if (activeGame == GameType.DINOSAUR_RUN) {
            dinoPanel.markResultSaved();
        } else if (activeGame == GameType.CATCH_FAVORITE) {
            catchPanel.markResultSaved();
        }
        NotificationCards.info(this, "점수 저장 완료", "게임 결과가 서버에 기록되었어요.");
    }

    public void stopAllGames() {
        dinoPanel.stopGame();
        catchPanel.stopGame();
    }
}
