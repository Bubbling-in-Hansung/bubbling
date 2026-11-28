package bubbling.client.ui.game;

import bubbling.client.ui.drawing.CharacterArt;
import bubbling.client.ui.model.CharacterChoice;
import bubbling.client.ui.model.UiSession;
import bubbling.common.enums.GameType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static bubbling.client.ui.style.Styles.*;

/** Game selection screen with entry points to play, check guide, or inspect rankings. */
public final class GameLobbyPanel extends JPanel {
    private static final Color CARD_HOVER = color("fcfaff");

    private final UiSession session;
    private final Consumer<GameType> onStartGame;
    private final Consumer<GameType> onShowGuide;
    private final Consumer<GameType> onShowRanking;
    private final Consumer<CharacterChoice> onSelectCharacter;

    private final Map<GameType, GameCard> cards = new EnumMap<>(GameType.class);
    private final Map<CharacterChoice, JButton> characterButtons = new EnumMap<>(CharacterChoice.class);
    private final JLabel selectedFavoriteLabel = label("", 11, SUBTLE);
    private final JLabel dinoBestLabel = label("", 11, PURPLE);
    private final JLabel catchBestLabel = label("", 11, PURPLE);
    private final JPanel characterPreview = new JPanel() {
        {
            setOpaque(false);
            setPreferredSize(new Dimension(112, 64));
        }

        @Override
        protected void paintComponent(Graphics source) {
            super.paintComponent(source);
            Graphics2D g = graphics(source);
            g.setColor(FavoriteItem.forChoice(selectedCharacter).tint());
            g.fillRoundRect(0, 4, getWidth(), 56, 20, 20);
            CharacterArt.paint(g, 14, 10, 0.48, selectedCharacter.ordinal());
            icon("heart").paintIcon(this, g, 48, 22);
            FavoriteItem.forChoice(selectedCharacter).paint(g, 70, 13, 36);
            g.dispose();
        }
    };

    private GameType selectedGame = GameType.DINOSAUR_RUN;
    private CharacterChoice selectedCharacter = CharacterChoice.BUNNY;

    public GameLobbyPanel(UiSession session,
                          Consumer<GameType> onStartGame,
                          Consumer<GameType> onShowGuide,
                          Consumer<GameType> onShowRanking,
                          Consumer<CharacterChoice> onSelectCharacter,
                          Runnable onCloseWindow) {
        super(new BorderLayout(0, 18));
        this.session = Objects.requireNonNull(session);
        this.onStartGame = onStartGame;
        this.onShowGuide = onShowGuide;
        this.onShowRanking = onShowRanking;
        this.onSelectCharacter = onSelectCharacter;
        dinoBestLabel.setFont(font(11, true));
        catchBestLabel.setFont(font(11, true));
        setBackground(BG);
        pad(this, 26);

        JPanel header = column();
        append(header, title("미니 게임", 22), 4);
        append(header, label("플레이할 게임을 고른 뒤 시작하거나 게임 방법·랭킹을 확인해 보세요.", 12, SUBTLE), 0);
        GameFrame.installDragHandle(header);
        add(header, BorderLayout.NORTH);

        JPanel cardRow = panel(new GridLayout(1, 2, 18, 0));
        GameCard dinoCard = createDinoCard();
        GameCard catchCard = createCatchCard();
        cards.put(GameType.DINOSAUR_RUN, dinoCard);
        cards.put(GameType.CATCH_FAVORITE, catchCard);
        cardRow.add(dinoCard);
        cardRow.add(catchCard);
        add(cardRow, BorderLayout.CENTER);

        JPanel footer = panel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JPanel leftActions = panel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton guideButton = button("게임 방법", false, () -> this.onShowGuide.accept(selectedGame));
        JButton rankingButton = button("랭킹 확인", false, () -> this.onShowRanking.accept(selectedGame));
        leftActions.add(guideButton);
        leftActions.add(rankingButton);

        JPanel rightActions = panel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton startButton = button("게임 시작", true, () -> this.onStartGame.accept(selectedGame));
        startButton.putClientProperty("gradient", true);
        startButton.setPreferredSize(new Dimension(136, 42));
        JButton exitButton = GameView.exitButton(onCloseWindow);
        exitButton.setPreferredSize(new Dimension(108, 42));
        rightActions.add(startButton);
        rightActions.add(exitButton);

        footer.add(leftActions, BorderLayout.WEST);
        footer.add(rightActions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        selectGame(GameType.DINOSAUR_RUN);
        setSelectedCharacter(CharacterChoice.BUNNY);
        refreshBestScores();
    }

    public void refreshBestScores() {
        dinoBestLabel.setText(bestScoreText(session.bestScore(GameType.DINOSAUR_RUN)));
        catchBestLabel.setText(bestScoreText(session.bestScore(GameType.CATCH_FAVORITE)));
    }

    private static String bestScoreText(int score) {
        return score > 0 ? "내 최고 점수: " + score + "점" : "내 최고 점수: 기록 없음";
    }

    public GameType selectedGame() {
        return selectedGame;
    }

    public CharacterChoice selectedCharacter() {
        return selectedCharacter;
    }

    public void setSelectedCharacter(CharacterChoice choice) {
        selectedCharacter = choice == null ? CharacterChoice.BUNNY : choice;
        FavoriteItem item = FavoriteItem.forChoice(selectedCharacter);
        selectedFavoriteLabel.setText("선택 캐릭터: " + item.characterName() + " · 목표: " + item.itemName());
        characterButtons.forEach((key, btn) -> btn.setSelected(key == selectedCharacter));
        characterPreview.repaint();
        repaint();
    }

    private void selectGame(GameType gameType) {
        selectedGame = gameType;
        cards.forEach((type, card) -> card.setSelected(type == gameType));
    }

    private GameCard createDinoCard() {
        GameCard card = new GameCard(() -> selectGame(GameType.DINOSAUR_RUN));
        JPanel preview = new JPanel() {
            {
                setOpaque(false);
                setPreferredSize(new Dimension(112, 64));
            }

            @Override
            protected void paintComponent(Graphics source) {
                super.paintComponent(source);
                Graphics2D g = graphics(source);
                g.setColor(SOFT);
                g.fillRoundRect(0, 4, getWidth(), 56, 20, 20);
                g.scale(0.8, 0.8);
                DinoSprites.drawTrex(g, false, 0, false, 18, 17);
                DinoSprites.drawCactusSmall(g, false, 1, 96, 29);
                g.setColor(LINE);
                g.drawLine(14, 65, (int) Math.round((getWidth() - 14) / 0.8), 65);
                g.dispose();
            }
        };
        card.addTopPreview("DINOSAUR RUN", "공룡 달리기", preview);

        JLabel desc = label("<html>다가오는 선인장과 익룡을 피해 최대한 멀리 달리는<br>도트 러너 게임이에요.</html>",
                11, COPY);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.body.add(desc);
        card.body.add(Box.createVerticalStrut(12));
        JLabel controls = label("조작: Space · ↑ 점프 · ↓ 숙이기", 10, SUBTLE);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.body.add(controls);
        card.body.add(Box.createVerticalGlue());
        dinoBestLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.body.add(dinoBestLabel);
        return card;
    }

    private GameCard createCatchCard() {
        GameCard card = new GameCard(() -> selectGame(GameType.CATCH_FAVORITE));
        card.addTopPreview("CATCH FAVORITE", "좋아하는 아이템 잡기", characterPreview);

        JLabel desc = label("<html>30초 동안 선택한 캐릭터가 좋아하는 아이템(+10점)만<br>마우스로 빠르게 클릭하세요! 다른 아이템은 -5점이에요.</html>",
                11, COPY);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.body.add(desc);
        card.body.add(Box.createVerticalStrut(10));

        selectedFavoriteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.body.add(selectedFavoriteLabel);
        card.body.add(Box.createVerticalStrut(8));

        JPanel picker = panel(new GridLayout(2, 3, 6, 6));
        picker.setAlignmentX(Component.LEFT_ALIGNMENT);
        picker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        for (CharacterChoice choice : CharacterChoice.values()) {
            FavoriteItem item = FavoriteItem.forChoice(choice);
            JButton btn = button(choice.label(), false, () -> {
                selectGame(GameType.CATCH_FAVORITE);
                setSelectedCharacter(choice);
                onSelectCharacter.accept(choice);
            });
            btn.setIcon(item.icon(18));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setFont(font(10, true));
            btn.setMargin(new Insets(4, 8, 4, 8));
            btn.setToolTipText(choice.label() + " · 좋아하는 아이템: " + item.itemName());
            btn.getAccessibleContext().setAccessibleName(choice.label());
            characterButtons.put(choice, btn);
            picker.add(btn);
        }
        card.body.add(picker);
        card.body.add(Box.createVerticalStrut(10));
        card.body.add(Box.createVerticalGlue());
        catchBestLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.body.add(catchBestLabel);
        return card;
    }

    private static final class GameCard extends JPanel {
        final JPanel body;
        private final JLabel selectedBadge = label("선택됨", 10, PURPLE);
        private boolean selected;
        private boolean hovered;

        GameCard(Runnable onClick) {
            super(new BorderLayout(0, 12));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pad(this, 20);
            body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            add(body, BorderLayout.CENTER);
            selectedBadge.setFont(font(10, true));

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onClick.run();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            };
            addMouseListener(adapter);
        }

        void addTopPreview(String eyebrow, String heading, JPanel preview) {
            JPanel top = panel(new BorderLayout(10, 0));
            JPanel copy = column();
            JPanel headRow = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            JLabel codeLabel = label(eyebrow, 10, PURPLE);
            codeLabel.setFont(font(10, true));
            headRow.add(codeLabel);
            headRow.add(selectedBadge);
            append(copy, headRow, 4);
            append(copy, title(heading, 17), 0);
            top.add(copy, BorderLayout.CENTER);
            top.add(preview, BorderLayout.EAST);
            top.setAlignmentX(Component.LEFT_ALIGNMENT);
            top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
            body.add(top);
            body.add(Box.createVerticalStrut(12));
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            selectedBadge.setVisible(selected);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics source) {
            Graphics2D g = graphics(source);
            g.setColor(selected ? Color.WHITE : (hovered ? CARD_HOVER : Color.WHITE));
            g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 28, 28);
            g.setColor(selected ? PURPLE : LINE);
            g.setStroke(new BasicStroke(selected ? 2.0f : 1.0f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 28, 28);
            g.dispose();
        }
    }
}
