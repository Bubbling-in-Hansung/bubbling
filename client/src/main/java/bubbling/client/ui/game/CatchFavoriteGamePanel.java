package bubbling.client.ui.game;

import bubbling.client.ui.drawing.CharacterArt;
import bubbling.client.ui.model.CharacterChoice;
import bubbling.client.ui.model.UiSession;
import bubbling.common.enums.GameType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import static bubbling.client.ui.style.Styles.*;

/** Timed mouse-click mini-game where players collect their character's favorite item. */
public final class CatchFavoriteGamePanel extends JPanel {
    private static final int TICK_MS = 16;
    private static final long ROUND_DURATION_MS = 30_000L;
    private static final int ITEM_RADIUS = 23;
    private static final int HIT_POINTS = 10;
    private static final int MISS_PENALTY = 5;
    private static final double MIN_SPAWN_SPACING = ITEM_RADIUS * 1.9;
    private static final double HARD_MIN_SPAWN_SPACING = ITEM_RADIUS * 1.45;
    private static final Color HIT_GREEN = color("1d9c72");
    private static final Color OVERLAY_BG = new Color(255, 255, 255, 236);

    private static final class SpawnedItem {
        final FavoriteItem item;
        final double centerX;
        final double centerY;
        final long bornAtMs;
        final long lifetimeMs;

        SpawnedItem(FavoriteItem item, double centerX, double centerY, long bornAtMs, long lifetimeMs) {
            this.item = item;
            this.centerX = centerX;
            this.centerY = centerY;
            this.bornAtMs = bornAtMs;
            this.lifetimeMs = lifetimeMs;
        }
    }

    private static final class FloatingFeedback {
        final String text;
        final Color color;
        final double x;
        final double y;
        final long createdAtMs;

        FloatingFeedback(String text, Color color, double x, double y, long createdAtMs) {
            this.text = text;
            this.color = color;
            this.x = x;
            this.y = y;
            this.createdAtMs = createdAtMs;
        }
    }

    private final UiSession session;
    private final Consumer<CharacterChoice> onSelectCharacter;
    private final Runnable onShowRanking;
    private final Runnable onBackToLobby;
    private final Random random = new Random();
    private final List<SpawnedItem> items = new ArrayList<>();
    private final List<FloatingFeedback> feedbacks = new ArrayList<>();
    private final Map<CharacterChoice, JButton> characterButtons = new EnumMap<>(CharacterChoice.class);
    private final Timer loop;

    private final JLabel targetLabel = title("", 13);
    private final JLabel targetRuleLabel = label("+10점 · 다른 아이템 -5점", 11, SUBTLE);
    private final JLabel timerLabel = title("30.0초", 14);
    private final JLabel scoreLabel = title("0점", 14);
    private final JButton restartButton;
    private final PlayBoard board = new PlayBoard();
    private final JPanel characterBadge = new JPanel() {
        {
            setOpaque(false);
            setPreferredSize(new Dimension(42, 48));
        }

        @Override
        protected void paintComponent(Graphics source) {
            super.paintComponent(source);
            Graphics2D g = graphics(source);
            CharacterArt.paint(g, 3, 4, 0.45, selectedCharacter.ordinal());
            g.dispose();
        }
    };
    private final JPanel targetFooterBadge = new JPanel() {
        {
            setOpaque(false);
            setPreferredSize(new Dimension(138, 48));
            setCursor(Cursor.getDefaultCursor());
        }

        @Override
        protected void paintComponent(Graphics source) {
            super.paintComponent(source);
            Graphics2D g = graphics(source);
            FavoriteItem fav = FavoriteItem.forChoice(selectedCharacter);
            g.setColor(fav.tint());
            g.fillRoundRect(0, 1, getWidth() - 1, getHeight() - 3, 22, 22);
            g.setColor(LINE);
            g.drawRoundRect(0, 1, getWidth() - 1, getHeight() - 3, 22, 22);

            CharacterArt.paint(g, 14, 5, 0.39, selectedCharacter.ordinal());
            icon("heart").paintIcon(this, g, 53, 13);

            int itemBgX = 86;
            int itemBgY = 5;
            int itemBgSize = 38;
            g.setColor(Color.WHITE);
            g.fillOval(itemBgX, itemBgY, itemBgSize, itemBgSize);
            g.setColor(LINE);
            g.drawOval(itemBgX, itemBgY, itemBgSize, itemBgSize);
            fav.paint(g, itemBgX + 3, itemBgY + 3, 32);
            g.dispose();
        }
    };

    private CharacterChoice selectedCharacter = CharacterChoice.BUNNY;
    private boolean running;
    private boolean finished;
    private boolean resultSubmitted;
    private long elapsedMs;
    private long lastSpawnAtMs;
    private int score;
    private int hitCount;
    private int missCount;

    public CatchFavoriteGamePanel(UiSession session,
                                  Consumer<CharacterChoice> onSelectCharacter,
                                  Runnable onShowRanking,
                                  Runnable onBackToLobby,
                                  Runnable onCloseWindow) {
        super(new BorderLayout(0, 12));
        this.session = session;
        this.onSelectCharacter = onSelectCharacter;
        this.onShowRanking = onShowRanking;
        this.onBackToLobby = onBackToLobby;
        setBackground(BG);
        pad(this, 24);

        JPanel northSection = panel(new BorderLayout(0, 10));

        JPanel hudBar = card(Color.WHITE, 22);
        hudBar.setLayout(new BorderLayout(12, 0));
        pad(hudBar, 12);

        JPanel targetGroup = panel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        targetGroup.add(characterBadge);
        JPanel targetCopy = column();
        append(targetCopy, targetLabel, 2);
        append(targetCopy, targetRuleLabel, 0);
        targetGroup.add(targetCopy);
        hudBar.add(targetGroup, BorderLayout.WEST);

        JPanel metrics = panel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        timerLabel.setForeground(PURPLE);
        metrics.add(timerLabel);
        metrics.add(scoreLabel);
        metrics.add(button("게임 선택", false, () -> {
            stopGame();
            this.onBackToLobby.run();
        }));
        metrics.add(GameView.exitButton(() -> {
            stopGame();
            onCloseWindow.run();
        }));
        hudBar.add(metrics, BorderLayout.EAST);
        GameFrame.installDragHandle(hudBar);
        northSection.add(hudBar, BorderLayout.NORTH);

        JPanel pickerBar = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pickerBar.add(label("캐릭터 변경:", 11, SUBTLE));
        for (CharacterChoice choice : CharacterChoice.values()) {
            FavoriteItem fav = FavoriteItem.forChoice(choice);
            JButton btn = button(choice.label(), false, () -> {
                if (running) return;
                setCharacter(choice);
                this.onSelectCharacter.accept(choice);
            });
            btn.setIcon(fav.icon(16));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setFont(font(10, true));
            btn.setMargin(new Insets(4, 8, 4, 8));
            btn.setToolTipText(choice.label() + " · 목표: " + fav.itemName());
            btn.getAccessibleContext().setAccessibleName("캐릭터 " + choice.label());
            characterButtons.put(choice, btn);
            pickerBar.add(btn);
        }
        northSection.add(pickerBar, BorderLayout.SOUTH);
        add(northSection, BorderLayout.NORTH);

        JPanel boardCard = card(Color.WHITE, 26);
        boardCard.setLayout(new BorderLayout());
        pad(boardCard, 12);
        boardCard.add(board, BorderLayout.CENTER);
        add(boardCard, BorderLayout.CENTER);

        JPanel footer = panel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        footer.add(targetFooterBadge, BorderLayout.WEST);

        JPanel actions = panel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        actions.add(button("랭킹 확인", false, this.onShowRanking));
        restartButton = button("다시 하기", true, () -> startNewRound(selectedCharacter));
        restartButton.putClientProperty("gradient", true);
        restartButton.setPreferredSize(new Dimension(118, 38));
        actions.add(Box.createHorizontalStrut(2));
        actions.add(restartButton);
        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        loop = new Timer(TICK_MS, event -> step());
        setCharacter(CharacterChoice.BUNNY);
    }

    public static long spawnIntervalForProgress(double progress) {
        return Math.round(180.0 - 120.0 * clampProgress(progress));
    }

    public static int maxItemsForProgress(double progress) {
        return (int) Math.round(14.0 + 18.0 * clampProgress(progress));
    }

    public static long minLifetimeForProgress(double progress) {
        return Math.round(800.0 - 480.0 * clampProgress(progress));
    }

    public static int lifetimeJitterForProgress(double progress) {
        return (int) Math.round(160.0 - 60.0 * clampProgress(progress));
    }

    private static double clampProgress(double progress) {
        return Math.min(1.0, Math.max(0.0, progress));
    }

    public void setCharacter(CharacterChoice choice) {
        selectedCharacter = choice == null ? CharacterChoice.BUNNY : choice;
        FavoriteItem fav = FavoriteItem.forChoice(selectedCharacter);
        targetLabel.setText(fav.characterName() + " · 목표: " + fav.itemName());
        targetLabel.setIcon(fav.icon(22));
        targetLabel.setIconTextGap(6);
        String badgeSummary = fav.characterName() + " ♥ " + fav.itemName();
        targetFooterBadge.setToolTipText(badgeSummary);
        targetFooterBadge.getAccessibleContext().setAccessibleName(badgeSummary);
        characterButtons.forEach((key, btn) -> btn.setSelected(key == selectedCharacter));
        characterBadge.repaint();
        targetFooterBadge.repaint();
        board.repaint();
    }

    public void startNewRound(CharacterChoice choice) {
        loop.stop();
        setCharacter(choice);
        items.clear();
        feedbacks.clear();
        elapsedMs = 0;
        lastSpawnAtMs = 0;
        score = 0;
        hitCount = 0;
        missCount = 0;
        running = true;
        finished = false;
        resultSubmitted = false;
        updateCharacterButtonsEnabled(false);
        restartButton.setEnabled(false);
        updateHud();
        spawnNextItem(0.0);
        loop.start();
        board.repaint();
    }

    public void stopGame() {
        loop.stop();
        running = false;
        updateCharacterButtonsEnabled(true);
    }

    public void markResultSaved() {
    }

    public boolean isRunning() {
        return running;
    }

    private void updateCharacterButtonsEnabled(boolean enabled) {
        String tip = enabled ? null : "라운드가 끝난 뒤 캐릭터를 바꿀 수 있어요.";
        for (var entry : characterButtons.entrySet()) {
            JButton btn = entry.getValue();
            btn.setEnabled(enabled);
            if (!enabled) {
                btn.setToolTipText(tip);
            } else {
                FavoriteItem fav = FavoriteItem.forChoice(entry.getKey());
                btn.setToolTipText(entry.getKey().label() + " · 목표: " + fav.itemName());
            }
        }
    }

    private void step() {
        if (!running) {
            loop.stop();
            return;
        }
        elapsedMs += TICK_MS;
        if (elapsedMs >= ROUND_DURATION_MS) {
            elapsedMs = ROUND_DURATION_MS;
            finishRound();
            board.repaint();
            return;
        }

        items.removeIf(item -> elapsedMs - item.bornAtMs >= item.lifetimeMs);
        feedbacks.removeIf(fb -> elapsedMs - fb.createdAtMs >= 520L);

        double progress = clampProgress((double) elapsedMs / ROUND_DURATION_MS);
        int maxItems = maxItemsForProgress(progress);
        long spawnInterval = spawnIntervalForProgress(progress);
        if (items.size() < maxItems && elapsedMs - lastSpawnAtMs >= spawnInterval) {
            if (spawnNextItem(progress)) {
                lastSpawnAtMs = elapsedMs;
            }
        }

        updateHud();
        board.repaint();
    }

    private boolean spawnNextItem(double progress) {
        int width = Math.max(480, board.getWidth());
        int height = Math.max(260, board.getHeight());
        int marginX = ITEM_RADIUS + 20;
        int minY = ITEM_RADIUS + 28;
        int usableW = Math.max(40, width - marginX * 2);
        int usableH = Math.max(40, height - minY - (ITEM_RADIUS + 18));

        double bestX = marginX + random.nextInt(usableW);
        double bestY = minY + random.nextInt(usableH);
        double bestDistSq = items.isEmpty() ? Double.MAX_VALUE : nearestDistanceSq(bestX, bestY);
        double preferredDistSq = MIN_SPAWN_SPACING * MIN_SPAWN_SPACING;

        if (!items.isEmpty() && bestDistSq < preferredDistSq) {
            for (int attempt = 0; attempt < 24; attempt++) {
                double cx = marginX + random.nextInt(usableW);
                double cy = minY + random.nextInt(usableH);
                double distSq = nearestDistanceSq(cx, cy);
                if (distSq > bestDistSq) {
                    bestDistSq = distSq;
                    bestX = cx;
                    bestY = cy;
                }
                if (bestDistSq >= preferredDistSq) {
                    break;
                }
            }
        }
        if (!items.isEmpty() && bestDistSq < HARD_MIN_SPAWN_SPACING * HARD_MIN_SPAWN_SPACING) {
            return false;
        }

        FavoriteItem target = FavoriteItem.forChoice(selectedCharacter);
        boolean hasTarget = false;
        for (SpawnedItem spawned : items) {
            if (spawned.item == target) {
                hasTarget = true;
                break;
            }
        }
        int targetChance = (int) Math.round(40.0 - 14.0 * clampProgress(progress));
        FavoriteItem chosen;
        if ((!hasTarget && items.size() >= 3) || random.nextInt(100) < targetChance) {
            chosen = target;
        } else {
            FavoriteItem[] all = FavoriteItem.values();
            do {
                chosen = all[random.nextInt(all.length)];
            } while (chosen == target);
        }
        int jitter = Math.max(1, lifetimeJitterForProgress(progress));
        long lifetime = minLifetimeForProgress(progress) + random.nextInt(jitter);
        items.add(new SpawnedItem(chosen, bestX, bestY, elapsedMs, lifetime));
        return true;
    }

    private double nearestDistanceSq(double x, double y) {
        double minSq = Double.MAX_VALUE;
        for (SpawnedItem item : items) {
            double dx = item.centerX - x;
            double dy = item.centerY - y;
            double distSq = dx * dx + dy * dy;
            if (distSq < minSq) {
                minSq = distSq;
            }
        }
        return minSq;
    }

    private void handleBoardClick(int x, int y) {
        if (!running || finished) return;
        FavoriteItem target = FavoriteItem.forChoice(selectedCharacter);
        for (int i = items.size() - 1; i >= 0; i--) {
            SpawnedItem candidate = items.get(i);
            double renderY = itemRenderY(candidate);
            double dx = x - candidate.centerX;
            double dy = y - renderY;
            if (dx * dx + dy * dy <= (double) (ITEM_RADIUS + 4) * (ITEM_RADIUS + 4)) {
                items.remove(i);
                if (candidate.item == target) {
                    score += HIT_POINTS;
                    hitCount++;
                    feedbacks.add(new FloatingFeedback("+" + HIT_POINTS, HIT_GREEN,
                            candidate.centerX, renderY - 12, elapsedMs));
                } else {
                    score = Math.max(0, score - MISS_PENALTY);
                    missCount++;
                    feedbacks.add(new FloatingFeedback("-" + MISS_PENALTY, DANGER_INK,
                            candidate.centerX, renderY - 12, elapsedMs));
                }
                updateHud();
                board.repaint();
                return;
            }
        }
    }

    private double itemRenderY(SpawnedItem item) {
        long age = elapsedMs - item.bornAtMs;
        return item.centerY + Math.sin(age / 190.0) * 4.0;
    }

    private void finishRound() {
        loop.stop();
        running = false;
        finished = true;
        items.clear();
        feedbacks.clear();
        updateCharacterButtonsEnabled(true);
        restartButton.setEnabled(true);
        updateHud();
        if (!resultSubmitted) {
            resultSubmitted = true;
            session.saveGameResult(GameType.CATCH_FAVORITE, score);
        }
    }

    private void updateHud() {
        long remainingMs = Math.max(0L, ROUND_DURATION_MS - elapsedMs);
        timerLabel.setText(String.format("%.1f초", remainingMs / 1000.0));
        timerLabel.setForeground(remainingMs <= 5_000L && running ? BADGE_RED : PURPLE);
        scoreLabel.setText(score + "점");
    }

    private final class PlayBoard extends JPanel {
        PlayBoard() {
            setOpaque(true);
            setBackground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleBoardClick(e.getX(), e.getY());
                }
            });
        }

        @Override
        protected void paintComponent(Graphics source) {
            super.paintComponent(source);
            Graphics2D g = graphics(source);
            int width = getWidth();
            int height = getHeight();

            g.setColor(SURFACE);
            g.fillRoundRect(0, 0, width, height, 18, 18);

            double ratio = running ? Math.max(0.0, 1.0 - (double) elapsedMs / ROUND_DURATION_MS) : (finished ? 0.0 : 1.0);
            g.setColor(LINE);
            g.fillRoundRect(16, 12, Math.max(0, width - 32), 6, 6, 6);
            g.setColor(ratio <= 0.2 && running ? BADGE_RED : PURPLE);
            g.fillRoundRect(16, 12, (int) Math.round(Math.max(0, width - 32) * ratio), 6, 6, 6);

            FavoriteItem target = FavoriteItem.forChoice(selectedCharacter);
            Composite prevComp = g.getComposite();
            for (SpawnedItem spawned : items) {
                int cy = (int) Math.round(itemRenderY(spawned));
                int cx = (int) Math.round(spawned.centerX);
                int r = ITEM_RADIUS;

                long age = elapsedMs - spawned.bornAtMs;
                double remainingRatio = spawned.lifetimeMs <= 0 ? 1.0
                        : Math.max(0.0, Math.min(1.0, 1.0 - (double) age / spawned.lifetimeMs));
                if (remainingRatio < 0.25) {
                    float alpha = (float) (0.42 + 0.58 * (remainingRatio / 0.25));
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                } else {
                    g.setComposite(prevComp);
                }

                g.setColor(spawned.item.tint());
                g.fillOval(cx - r, cy - r, r * 2, r * 2);
                g.setColor(spawned.item == target ? SOFT : LINE);
                g.setStroke(new BasicStroke(1.5f));
                g.drawOval(cx - r, cy - r, r * 2, r * 2);
                spawned.item.paint(g, cx - 15, cy - 15, 30);

                if (remainingRatio < 0.25) {
                    g.setColor(spawned.item == target ? PURPLE : SUBTLE);
                    g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawArc(cx - r, cy - r, r * 2, r * 2, 90, (int) Math.round(-360.0 * remainingRatio));
                }
            }
            g.setComposite(prevComp);

            for (FloatingFeedback fb : feedbacks) {
                double progress = Math.min(1.0, Math.max(0.0, (elapsedMs - fb.createdAtMs) / 520.0));
                int alpha = Math.max(0, (int) Math.round(255 * (1.0 - progress)));
                Color c = new Color(fb.color.getRed(), fb.color.getGreen(), fb.color.getBlue(), alpha);
                g.setFont(font(14, true));
                g.setColor(c);
                FontMetrics metrics = g.getFontMetrics();
                int fx = (int) Math.round(fb.x) - metrics.stringWidth(fb.text) / 2;
                int fy = (int) Math.round(fb.y - progress * 22);
                g.drawString(fb.text, fx, fy);
            }

            if (finished) {
                paintSummaryCard(g, width, height);
            }
            g.dispose();
        }

        private void paintSummaryCard(Graphics2D g, int width, int height) {
            int cardW = Math.min(360, width - 48);
            int cardH = 152;
            int cardX = (width - cardW) / 2;
            int cardY = Math.max(28, (height - cardH) / 2);

            g.setColor(OVERLAY_BG);
            g.fillRoundRect(cardX, cardY, cardW, cardH, 24, 24);
            g.setColor(PURPLE);
            g.drawRoundRect(cardX, cardY, cardW, cardH, 24, 24);

            paintCentered(g, "게임 종료!", cardX + cardW / 2, cardY + 38, 17, INK, true);
            paintCentered(g, "최종 점수 " + score + "점", cardX + cardW / 2, cardY + 68, 15, PURPLE, true);
            paintCentered(g, "적중 " + hitCount + "회 · 실수 " + missCount + "회",
                    cardX + cardW / 2, cardY + 98, 11, COPY, false);
            paintCentered(g, "다른 캐릭터로도 도전해 보세요!",
                    cardX + cardW / 2, cardY + 126, 10, SUBTLE, false);
        }

        private void paintCentered(Graphics2D g, String text, int centerX, int y, int size, Color color, boolean bold) {
            g.setFont(font(size, bold));
            g.setColor(color);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(text, centerX - metrics.stringWidth(text) / 2, y);
        }
    }
}
