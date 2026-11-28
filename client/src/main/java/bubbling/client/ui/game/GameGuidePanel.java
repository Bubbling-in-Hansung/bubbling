package bubbling.client.ui.game;

import bubbling.client.ui.drawing.CharacterArt;
import bubbling.client.ui.model.CharacterChoice;
import bubbling.common.enums.GameType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.function.Consumer;

import static bubbling.client.ui.style.Styles.*;

/** Explains controls, scoring rules, and favorite-item mappings for both mini-games. */
public final class GameGuidePanel extends JPanel {
    private static final String DINO_CARD = GameType.DINOSAUR_RUN.name();
    private static final String CATCH_CARD = GameType.CATCH_FAVORITE.name();

    private final CardLayout guideCards = new CardLayout();
    private final JPanel guideDeck = panel(guideCards);
    private final JButton dinoTab;
    private final JButton catchTab;
    private GameType activeGame = GameType.DINOSAUR_RUN;

    public GameGuidePanel(Runnable onBackToLobby, Consumer<GameType> onStartGame, Runnable onCloseWindow) {
        super(new BorderLayout(0, 16));
        setBackground(BG);
        pad(this, 26);

        JPanel top = panel(new BorderLayout(0, 12));
        JPanel titleRow = panel(new BorderLayout());
        JPanel copy = column();
        append(copy, title("게임 방법", 22), 4);
        append(copy, label("각 미니 게임의 조작 방법과 점수 규칙을 확인해 보세요.", 12, SUBTLE), 0);
        titleRow.add(copy, BorderLayout.CENTER);
        JPanel topActions = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.add(button("게임 선택으로", false, onBackToLobby));
        topActions.add(GameView.exitButton(onCloseWindow));
        titleRow.add(topActions, BorderLayout.EAST);
        GameFrame.installDragHandle(titleRow);
        top.add(titleRow, BorderLayout.NORTH);

        JPanel tabs = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dinoTab = categoryTab("공룡 달리기", () -> showGame(GameType.DINOSAUR_RUN));
        catchTab = categoryTab("좋아하는 아이템 잡기", () -> showGame(GameType.CATCH_FAVORITE));
        tabs.add(dinoTab);
        tabs.add(catchTab);
        top.add(tabs, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        guideDeck.add(scroll(createDinoGuide()), DINO_CARD);
        guideDeck.add(scroll(createCatchGuide()), CATCH_CARD);
        add(guideDeck, BorderLayout.CENTER);

        JPanel footer = panel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.add(button("게임 선택으로", false, onBackToLobby));
        JButton startButton = button("이 게임 시작하기", true, () -> onStartGame.accept(activeGame));
        startButton.putClientProperty("gradient", true);
        startButton.setPreferredSize(new Dimension(148, 40));
        footer.add(startButton);
        add(footer, BorderLayout.SOUTH);

        showGame(GameType.DINOSAUR_RUN);
    }

    public void showGame(GameType gameType) {
        activeGame = gameType == null ? GameType.DINOSAUR_RUN : gameType;
        selectCategoryTab(dinoTab, activeGame == GameType.DINOSAUR_RUN);
        selectCategoryTab(catchTab, activeGame == GameType.CATCH_FAVORITE);
        guideCards.show(guideDeck, activeGame == GameType.DINOSAUR_RUN ? DINO_CARD : CATCH_CARD);
    }

    private JPanel createDinoGuide() {
        JPanel body = column();
        body.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

        JPanel cardPanel = card(Color.WHITE, 26);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        pad(cardPanel, 22);

        JPanel previewBanner = new JPanel() {
            {
                setOpaque(false);
                setPreferredSize(new Dimension(500, 96));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
            }

            @Override
            protected void paintComponent(Graphics source) {
                super.paintComponent(source);
                Graphics2D g = graphics(source);
                g.setColor(SOFT);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g.clip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
                DinoSprites.drawCloud(g, false, 70, 14);
                DinoSprites.drawCloud(g, false, Math.max(220, getWidth() - 190), 20);
                DinoSprites.drawHorizon(g, false, false, 16, 71);
                if (getWidth() > 616) {
                    DinoSprites.drawHorizon(g, false, true, 616, 71);
                }
                DinoSprites.drawTrex(g, false, 88, false, 44, 84 - DinoSprites.LOGICAL_TREX_HEIGHT);
                DinoSprites.drawCactusLarge(g, false, 2, 220, 84 - DinoSprites.LOGICAL_CACTUS_LARGE_H);
                DinoSprites.drawPterodactyl(g, false, 0, Math.max(300, getWidth() - 120), 22);
                g.dispose();
            }
        };
        previewBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(previewBanner);
        cardPanel.add(Box.createVerticalStrut(18));

        appendRule(cardPanel, "1. 조작 방법",
                "Space 키 또는 ↑ 방향키로 점프할 수 있어요. ↓ 방향키를 누르면 공중에서는 빠르게 하강하고, 지상에서는 몸을 숙여 중간 높이의 익룡을 피할 수 있어요.");
        appendRule(cardPanel, "2. 진행, 밤/낮 전환 및 점수",
                "달린 거리에 비례해 점수가 올라가고 100점마다 점수가 반짝여요. 700점마다 밤 모드로 전환되어 달과 별이 뜨고, 속도가 빨라지면 다중 선인장과 익룡이 등장해요.");
        appendRule(cardPanel, "3. 게임 종료 및 기록",
                "장애물에 부딪히면 게임이 종료되고 최종 점수가 서버로 자동 전송돼요. 게임 오버 화면에서 Space 키나 '다시 하기' 버튼으로 바로 재시작할 수 있어요.");

        body.add(cardPanel);
        return body;
    }

    private JPanel createCatchGuide() {
        JPanel body = column();
        body.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

        JPanel cardPanel = card(Color.WHITE, 26);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        pad(cardPanel, 22);

        appendRule(cardPanel, "1. 시간 및 점수 규칙",
                "제한 시간 30초 동안 보드에 나타나는 아이템 중 내 캐릭터가 좋아하는 아이템을 클릭하면 +10점, 다른 캐릭터의 아이템을 클릭하면 -5점이에요. 시간이 지날수록 아이템이 더 빠르고 많이 나타나며 더 빠르게 사라져요. (점수는 0점 미만으로 내려가지 않아요.)");
        appendRule(cardPanel, "2. 자유로운 캐릭터 선택",
                "기본으로 내 프로필 캐릭터가 선택되지만, 게임 선택 화면이나 게임 화면에서 원하는 다른 캐릭터로 바꿔 플레이할 수 있어요.");

        JLabel mapTitle = title("3. 캐릭터별 좋아하는 아이템", 13);
        mapTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(mapTitle);
        cardPanel.add(Box.createVerticalStrut(10));

        JPanel grid = panel(new GridLayout(2, 3, 10, 10));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 152));
        for (CharacterChoice choice : CharacterChoice.values()) {
            FavoriteItem item = FavoriteItem.forChoice(choice);
            JPanel tile = card(item.tint(), 18, false);
            tile.setLayout(new BorderLayout(10, 0));
            pad(tile, 10);

            JPanel charBadge = new JPanel() {
                {
                    setOpaque(false);
                    setPreferredSize(new Dimension(38, 48));
                }

                @Override
                protected void paintComponent(Graphics source) {
                    super.paintComponent(source);
                    Graphics2D g = graphics(source);
                    CharacterArt.paint(g, 2, 2, 0.45, choice.ordinal());
                    g.dispose();
                }
            };
            tile.add(charBadge, BorderLayout.WEST);

            JPanel info = column();
            append(info, title(item.characterName(), 12), 2);
            JLabel itemLabel = label("좋아함: " + item.itemName(), 11, COPY);
            itemLabel.setIcon(item.icon(20));
            itemLabel.setIconTextGap(5);
            append(info, itemLabel, 0);
            tile.add(info, BorderLayout.CENTER);
            grid.add(tile);
        }
        cardPanel.add(grid);

        body.add(cardPanel);
        return body;
    }

    private static void appendRule(JPanel container, String heading, String detail) {
        JLabel titleLabel = title(heading, 13);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(titleLabel);
        container.add(Box.createVerticalStrut(4));
        JTextArea bodyLabel = new JTextArea(detail);
        bodyLabel.setFont(font(11, false));
        bodyLabel.setForeground(COPY);
        bodyLabel.setOpaque(false);
        bodyLabel.setBorder(null);
        bodyLabel.setEditable(false);
        bodyLabel.setFocusable(false);
        bodyLabel.setLineWrap(true);
        bodyLabel.setWrapStyleWord(true);
        bodyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bodyLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));
        container.add(bodyLabel);
        container.add(Box.createVerticalStrut(14));
    }
}
