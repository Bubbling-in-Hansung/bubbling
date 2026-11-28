package bubbling.client.ui.game;

import bubbling.client.ui.drawing.CharacterArt;
import bubbling.client.ui.model.CharacterChoice;
import bubbling.client.ui.model.UiSession;
import bubbling.common.dto.game.GameRankingData;
import bubbling.common.dto.game.GameRankingResponse;
import bubbling.common.enums.GameRankingType;
import bubbling.common.enums.GameType;
import bubbling.common.enums.ProfileCharacter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static bubbling.client.ui.style.Styles.*;

/** Displays per-game leaderboards (MAX, SUM, AVERAGE) and the user's current standing. */
public final class GameRankingPanel extends JPanel {
    private static final int REQUEST_LIMIT = 1000;
    private static final int MAX_VISIBLE_ROWS = 50;
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    private static final Color GOLD_BG = color("fff4d6");
    private static final Color GOLD_INK = color("b47b00");
    private static final Color SILVER_BG = color("eff2f8");
    private static final Color SILVER_INK = color("59637a");
    private static final Color BRONZE_BG = color("fceee3");
    private static final Color BRONZE_INK = color("9e5a28");

    private final UiSession session;
    private final Consumer<GameType> onStartGame;
    private final Map<GameType, JButton> gameTabs = new EnumMap<>(GameType.class);
    private final Map<GameRankingType, JButton> rankingTabs = new EnumMap<>(GameRankingType.class);

    private final JLabel mySummaryTitle = title("내 기록", 13);
    private final JLabel mySummaryDetail = label("랭킹을 불러오는 중이에요...", 12, COPY);
    private final JLabel aggregateAtLabel = label("", 11, SUBTLE);
    private final JPanel myCharacterBadge = new JPanel() {
        {
            setOpaque(false);
            setPreferredSize(new Dimension(38, 44));
        }

        @Override
        protected void paintComponent(Graphics source) {
            super.paintComponent(source);
            Graphics2D g = graphics(source);
            CharacterArt.paint(g, 3, 2, 0.42, session.character());
            g.dispose();
        }
    };
    private final JPanel listBody = column();

    private GameType activeGame = GameType.DINOSAUR_RUN;
    private GameRankingType activeRankingType = GameRankingType.MAX;

    public GameRankingPanel(UiSession session,
                            Runnable onBackToLobby,
                            Consumer<GameType> onStartGame,
                            Runnable onCloseWindow) {
        super(new BorderLayout(0, 14));
        this.session = Objects.requireNonNull(session);
        this.onStartGame = Objects.requireNonNull(onStartGame);
        setBackground(BG);
        pad(this, 24);

        JPanel top = panel(new BorderLayout(0, 12));
        JPanel titleRow = panel(new BorderLayout());
        JPanel copy = column();
        append(copy, title("게임 랭킹", 22), 4);
        append(copy, label("게임별 최고·누적·평균 순위와 내 기록을 확인해 보세요.", 12, SUBTLE), 0);
        titleRow.add(copy, BorderLayout.CENTER);

        JPanel topActions = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.add(button("새로고침", false, this::requestCurrentRanking));
        topActions.add(button("게임 선택으로", false, onBackToLobby));
        topActions.add(GameView.exitButton(onCloseWindow));
        titleRow.add(topActions, BorderLayout.EAST);
        GameFrame.installDragHandle(titleRow);
        top.add(titleRow, BorderLayout.NORTH);

        JPanel filterBar = panel(new BorderLayout());
        JPanel gameTabGroup = panel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton dinoTab = categoryTab("공룡 달리기", () -> selectGame(GameType.DINOSAUR_RUN));
        JButton catchTab = categoryTab("좋아하는 아이템 잡기", () -> selectGame(GameType.CATCH_FAVORITE));
        gameTabs.put(GameType.DINOSAUR_RUN, dinoTab);
        gameTabs.put(GameType.CATCH_FAVORITE, catchTab);
        gameTabGroup.add(dinoTab);
        gameTabGroup.add(catchTab);

        JPanel typeTabGroup = panel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton maxTab = categoryTab("최고 점수", () -> selectRankingType(GameRankingType.MAX));
        JButton sumTab = categoryTab("누적 점수", () -> selectRankingType(GameRankingType.SUM));
        JButton avgTab = categoryTab("평균 점수", () -> selectRankingType(GameRankingType.AVERAGE));
        rankingTabs.put(GameRankingType.MAX, maxTab);
        rankingTabs.put(GameRankingType.SUM, sumTab);
        rankingTabs.put(GameRankingType.AVERAGE, avgTab);
        typeTabGroup.add(maxTab);
        typeTabGroup.add(sumTab);
        typeTabGroup.add(avgTab);

        filterBar.add(gameTabGroup, BorderLayout.WEST);
        filterBar.add(typeTabGroup, BorderLayout.EAST);
        top.add(filterBar, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel center = panel(new BorderLayout(0, 12));
        JPanel myBanner = card(SOFT, 20, false);
        myBanner.setLayout(new BorderLayout(12, 0));
        pad(myBanner, 12);
        JPanel myLeft = panel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        myLeft.add(myCharacterBadge);
        JPanel myCopy = column();
        append(myCopy, mySummaryTitle, 2);
        append(myCopy, mySummaryDetail, 0);
        myLeft.add(myCopy);
        myBanner.add(myLeft, BorderLayout.WEST);
        myBanner.add(aggregateAtLabel, BorderLayout.EAST);
        center.add(myBanner, BorderLayout.NORTH);

        listBody.setBorder(BorderFactory.createEmptyBorder(2, 0, 8, 0));
        center.add(scroll(listBody), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel footer = panel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.add(button("게임 선택으로", false, onBackToLobby));
        JButton playButton = button("이 게임 시작하기", true, () -> this.onStartGame.accept(activeGame));
        playButton.putClientProperty("gradient", true);
        playButton.setPreferredSize(new Dimension(148, 40));
        footer.add(playButton);
        add(footer, BorderLayout.SOUTH);

        updateTabSelection();
    }

    public void open(GameType focusGame) {
        activeGame = focusGame == null ? GameType.DINOSAUR_RUN : focusGame;
        updateTabSelection();
        requestCurrentRanking();
    }

    public GameType activeGame() {
        return activeGame;
    }

    public GameRankingType activeRankingType() {
        return activeRankingType;
    }

    public void requestCurrentRanking() {
        showLoadingState();
        session.requestGameRanking(activeRankingType, activeGame, REQUEST_LIMIT);
    }

    public void applyRanking(GameType gameType, GameRankingResponse response) {
        if (gameType != activeGame || response == null || response.rankingType() != activeRankingType) {
            return;
        }
        myCharacterBadge.repaint();
        mySummaryTitle.setText("내 기록 · " + metricLabel(activeRankingType));
        String formattedAgg = formatTimestamp(response.aggregateAt(), " 기준");
        aggregateAtLabel.setText(formattedAgg.isEmpty() ? "" : formattedAgg);

        List<GameRankingData> rankings = response.rankings() == null ? List.of() : response.rankings();
        String myUserId = session.currentUserId();
        GameRankingData mine = findMine(rankings, myUserId);
        if (mine == null) {
            mySummaryDetail.setText("아직 등록된 내 기록이 없어요.");
        } else {
            mySummaryDetail.setText(mine.rank() + "위 · " + mine.score() + "점");
        }

        listBody.removeAll();
        if (rankings.isEmpty()) {
            JPanel empty = card(Color.WHITE, 20);
            empty.setLayout(new BorderLayout());
            pad(empty, 24);
            JLabel emptyLabel = label("아직 등록된 기록이 없어요. 첫 번째 기록을 남겨보세요!", 12, SUBTLE);
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            empty.add(emptyLabel, BorderLayout.CENTER);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
            listBody.add(empty);
        } else {
            int limit = Math.min(MAX_VISIBLE_ROWS, rankings.size());
            for (int i = 0; i < limit; i++) {
                GameRankingData entry = rankings.get(i);
                boolean isMine = myUserId != null && entry.user() != null && myUserId.equals(entry.user().id());
                JPanel row = createRankingRow(entry, isMine);
                append(listBody, row, i < limit - 1 ? 8 : 0);
            }
        }
        listBody.revalidate();
        listBody.repaint();
    }

    private void selectGame(GameType gameType) {
        if (gameType == null) return;
        activeGame = gameType;
        updateTabSelection();
        requestCurrentRanking();
    }

    private void selectRankingType(GameRankingType rankingType) {
        if (rankingType == null) return;
        activeRankingType = rankingType;
        updateTabSelection();
        requestCurrentRanking();
    }

    private void updateTabSelection() {
        gameTabs.forEach((type, tab) -> selectCategoryTab(tab, type == activeGame));
        rankingTabs.forEach((type, tab) -> selectCategoryTab(tab, type == activeRankingType));
    }

    private void showLoadingState() {
        myCharacterBadge.repaint();
        mySummaryTitle.setText("내 기록 · " + metricLabel(activeRankingType));
        mySummaryDetail.setText("랭킹을 불러오는 중이에요...");
        aggregateAtLabel.setText("");
        listBody.removeAll();
        JPanel loadingCard = card(Color.WHITE, 20);
        loadingCard.setLayout(new BorderLayout());
        pad(loadingCard, 24);
        JLabel loadingLabel = label("랭킹 정보를 조회하고 있어요...", 12, SUBTLE);
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingCard.add(loadingLabel, BorderLayout.CENTER);
        loadingCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        loadingCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        listBody.add(loadingCard);
        listBody.revalidate();
        listBody.repaint();
    }

    private JPanel createRankingRow(GameRankingData entry, boolean isMine) {
        JPanel row = card(isMine ? SOFT : Color.WHITE, 18, true);
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 16));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JPanel left = panel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        JPanel rankBadge = createRankBadge(entry.rank());
        left.add(rankBadge);

        ProfileCharacter profile = entry.user() == null ? null : entry.user().profile();
        int charIndex = CharacterChoice.indexOf(profile);
        JPanel avatar = new JPanel() {
            {
                setOpaque(false);
                setPreferredSize(new Dimension(34, 42));
            }

            @Override
            protected void paintComponent(Graphics source) {
                super.paintComponent(source);
                Graphics2D g = graphics(source);
                CharacterArt.paint(g, 2, 2, 0.39, charIndex);
                g.dispose();
            }
        };
        left.add(avatar);

        String rawName = entry.user() == null || entry.user().nickname() == null || entry.user().nickname().isBlank()
                ? "알 수 없음" : entry.user().nickname();
        String displayName = isMine ? rawName + " (나)" : rawName;
        JPanel userCopy = column();
        JLabel nameLabel = title(displayName, 13);
        if (isMine) {
            nameLabel.setForeground(PURPLE);
        }
        append(userCopy, nameLabel, 2);
        String createdLabel = formatTimestamp(entry.createdAt(), "");
        if (!createdLabel.isEmpty()) {
            append(userCopy, label(createdLabel, 10, SUBTLE), 0);
        }
        left.add(userCopy);
        row.add(left, BorderLayout.WEST);

        JLabel scoreText = title(entry.score() + "점", 15);
        scoreText.setForeground(entry.rank() == 1 || isMine ? PURPLE : INK);
        row.add(scoreText, BorderLayout.EAST);
        return row;
    }

    private static JPanel createRankBadge(int rank) {
        Color bg = rank == 1 ? GOLD_BG : rank == 2 ? SILVER_BG : rank == 3 ? BRONZE_BG : SURFACE;
        Color fg = rank == 1 ? GOLD_INK : rank == 2 ? SILVER_INK : rank == 3 ? BRONZE_INK : SUBTLE;
        JPanel badge = card(bg, 14, false);
        badge.setLayout(new BorderLayout());
        badge.setPreferredSize(new Dimension(32, 32));
        JLabel num = title(Integer.toString(rank), 12);
        num.setForeground(fg);
        num.setHorizontalAlignment(SwingConstants.CENTER);
        badge.add(num, BorderLayout.CENTER);
        return badge;
    }

    private static GameRankingData findMine(List<GameRankingData> rankings, String myUserId) {
        if (myUserId == null || rankings == null) return null;
        for (GameRankingData entry : rankings) {
            if (entry != null && entry.user() != null && myUserId.equals(entry.user().id())) {
                return entry;
            }
        }
        return null;
    }

    private static String metricLabel(GameRankingType rankingType) {
        if (rankingType == null) return "최고 점수";
        return switch (rankingType) {
            case MAX -> "최고 점수";
            case SUM -> "누적 점수";
            case AVERAGE -> "평균 점수";
        };
    }

    private static String formatTimestamp(String raw, String suffix) {
        if (raw == null || raw.isBlank()) return "";
        try {
            return LocalDateTime.parse(raw).format(DISPLAY_TIME) + suffix;
        } catch (DateTimeParseException e) {
            return raw + suffix;
        }
    }
}
