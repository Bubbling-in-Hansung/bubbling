/*
 * Ported from Chromium's T-Rex Runner (offline.js / wayou/t-rex-runner).
 * Copyright (c) 2014 The Chromium Authors. All rights reserved.
 * Governed by a BSD-style license; see /game/THIRD_PARTY_LICENSE_TREX.txt.
 */
package bubbling.client.ui.game;

import bubbling.client.ui.model.UiSession;
import bubbling.common.enums.GameType;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static bubbling.client.ui.style.Styles.*;

/** Swing port of Chromium's official T-Rex Runner with sprite sheet animation and night cycle. */
public final class DinoRunGamePanel extends JPanel {
    private static final int STAGE_WIDTH = 600;
    private static final int STAGE_HEIGHT = 150;
    private static final int TICK_MS = 16;
    private static final double FPS = 60.0;
    private static final double MS_PER_FRAME = 1000.0 / FPS;

    private static final double START_SPEED = 6.0;
    private static final double MAX_SPEED = 13.0;
    private static final double ACCELERATION = 0.001;
    private static final double GAP_COEFFICIENT = 0.6;
    private static final double MAX_GAP_COEFFICIENT = 1.5;
    private static final int MAX_OBSTACLE_LENGTH = 3;
    private static final int MAX_OBSTACLE_DUPLICATION = 2;
    private static final double CLEAR_TIME_MS = 2000.0;
    private static final long GAMEOVER_CLEAR_TIME_MS = 750L;

    private static final int BOTTOM_PAD = 10;
    private static final int TREX_START_X = 50;
    private static final int TREX_GROUND_Y = STAGE_HEIGHT - DinoSprites.LOGICAL_TREX_HEIGHT - BOTTOM_PAD;
    private static final double TREX_GRAVITY = 0.6;
    private static final double TREX_INITIAL_JUMP_VELOCITY = -10.0;
    private static final double TREX_DROP_VELOCITY = -5.0;
    private static final double TREX_SPEED_DROP_COEFFICIENT = 3.0;
    private static final int TREX_MIN_JUMP_HEIGHT_OFFSET = 30;
    private static final int TREX_MAX_JUMP_Y = 30;

    private static final int INVERT_DISTANCE = 700;
    private static final double INVERT_FADE_DURATION_MS = 12_000.0;
    private static final float NIGHT_FADE_SPEED = 0.035f;
    private static final int HORIZON_Y = 127;

    private static final Color DAY_STAGE_BG = color("f7f7f7");
    private static final Color NIGHT_STAGE_BG = color("202124");

    private record BoxRect(int x, int y, int width, int height) {}

    private static final BoxRect[] TREX_RUNNING_BOXES = {
            new BoxRect(22, 0, 17, 16),
            new BoxRect(1, 18, 30, 9),
            new BoxRect(10, 35, 14, 8),
            new BoxRect(1, 24, 29, 5),
            new BoxRect(5, 30, 21, 4),
            new BoxRect(9, 34, 15, 4)
    };
    private static final BoxRect[] TREX_DUCKING_BOXES = {
            new BoxRect(1, 18, 55, 25)
    };

    private enum TrexState {
        RUNNING(new int[]{88, 132}, 1000.0 / 12.0),
        JUMPING(new int[]{0}, 1000.0 / 60.0),
        DUCKING(new int[]{264, 323}, 1000.0 / 8.0),
        CRASHED(new int[]{220}, 1000.0 / 60.0);

        final int[] frameOffsets;
        final double msPerFrame;

        TrexState(int[] frameOffsets, double msPerFrame) {
            this.frameOffsets = frameOffsets;
            this.msPerFrame = msPerFrame;
        }
    }

    private enum ObstacleType {
        CACTUS_SMALL(17, 35, new int[]{105}, 4.0, 120, 0.0, 1, 0.0, 0.0,
                new BoxRect[]{
                        new BoxRect(0, 7, 5, 27),
                        new BoxRect(4, 0, 6, 34),
                        new BoxRect(10, 4, 7, 14)
                }),
        CACTUS_LARGE(25, 50, new int[]{90}, 7.0, 120, 0.0, 1, 0.0, 0.0,
                new BoxRect[]{
                        new BoxRect(0, 12, 7, 38),
                        new BoxRect(8, 0, 7, 49),
                        new BoxRect(13, 10, 10, 38)
                }),
        PTERODACTYL(46, 40, new int[]{100, 75, 50}, 999.0, 150, 8.5, 2, 1000.0 / 6.0, 0.8,
                new BoxRect[]{
                        new BoxRect(15, 15, 16, 5),
                        new BoxRect(18, 21, 24, 6),
                        new BoxRect(2, 14, 4, 3),
                        new BoxRect(6, 10, 4, 7),
                        new BoxRect(10, 8, 6, 9)
                });

        final int baseWidth;
        final int height;
        final int[] yPositions;
        final double multipleSpeed;
        final int minGap;
        final double minSpeed;
        final int numFrames;
        final double frameRateMs;
        final double speedOffset;
        final BoxRect[] baseBoxes;

        ObstacleType(int baseWidth, int height, int[] yPositions, double multipleSpeed, int minGap,
                     double minSpeed, int numFrames, double frameRateMs, double speedOffset, BoxRect[] baseBoxes) {
            this.baseWidth = baseWidth;
            this.height = height;
            this.yPositions = yPositions;
            this.multipleSpeed = multipleSpeed;
            this.minGap = minGap;
            this.minSpeed = minSpeed;
            this.numFrames = numFrames;
            this.frameRateMs = frameRateMs;
            this.speedOffset = speedOffset;
            this.baseBoxes = baseBoxes;
        }
    }

    private static final class Obstacle {
        final ObstacleType type;
        final int size;
        final int width;
        final int yPos;
        final double speedOffset;
        final int gap;
        final List<BoxRect> collisionBoxes;
        double xPos = STAGE_WIDTH;
        boolean followingCreated;
        int currentFrame;
        double animTimer;

        Obstacle(ObstacleType type, int size, double speed, Random random) {
            this.type = type;
            this.size = size;
            this.width = type.baseWidth * size;
            this.yPos = type.yPositions[random.nextInt(type.yPositions.length)];
            this.speedOffset = type.speedOffset == 0.0 ? 0.0
                    : (random.nextBoolean() ? type.speedOffset : -type.speedOffset);
            int minGap = (int) Math.round(this.width * speed + type.minGap * GAP_COEFFICIENT);
            int maxGap = (int) Math.round(minGap * MAX_GAP_COEFFICIENT);
            this.gap = minGap + random.nextInt(Math.max(1, maxGap - minGap + 1));

            this.collisionBoxes = new ArrayList<>(type.baseBoxes.length);
            for (BoxRect box : type.baseBoxes) {
                this.collisionBoxes.add(box);
            }
            if (size > 1 && this.collisionBoxes.size() >= 3) {
                BoxRect left = this.collisionBoxes.get(0);
                BoxRect mid = this.collisionBoxes.get(1);
                BoxRect right = this.collisionBoxes.get(2);
                int midWidth = this.width - left.width() - right.width();
                int rightX = this.width - right.width();
                this.collisionBoxes.set(1, new BoxRect(mid.x(), mid.y(), Math.max(1, midWidth), mid.height()));
                this.collisionBoxes.set(2, new BoxRect(rightX, right.y(), right.width(), right.height()));
            }
        }
    }

    private static final class Cloud {
        double xPos = STAGE_WIDTH;
        final int yPos;
        final int cloudGap;

        Cloud(Random random) {
            this.yPos = 30 + random.nextInt(71 - 30 + 1);
            this.cloudGap = 100 + random.nextInt(400 - 100 + 1);
        }
    }

    private final UiSession session;
    private final Runnable onShowRanking;
    private final Runnable onBackToLobby;
    private final Random random = new Random();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Cloud> clouds = new ArrayList<>();
    private final Deque<ObstacleType> obstacleHistory = new ArrayDeque<>();
    private final Timer loop;

    private final JLabel scoreLabel = title("점수 0점", 14);
    private final JLabel bestLabel = label("최고 0점", 12, SUBTLE);
    private final JLabel statusLabel = label("Space · ↑ 점프 · ↓ 숙이기", 11, SUBTLE);
    private final JButton restartButton;
    private final PlayCanvas canvas = new PlayCanvas();

    private boolean running;
    private boolean gameOver;
    private boolean resultSubmitted;
    private long lastTickMs;
    private long crashedAtMs;

    private double currentSpeed = START_SPEED;
    private double distanceRan;
    private double runningTimeMs;
    private int score;
    private int bestScore;

    // T-Rex physics & animation state
    private TrexState trexState = TrexState.RUNNING;
    private double trexY = TREX_GROUND_Y;
    private double jumpVelocity;
    private boolean jumping;
    private boolean ducking;
    private boolean speedDrop;
    private boolean reachedMinHeight;
    private boolean duckKeyDown;
    private int trexFrame;
    private double trexAnimTimer;

    // Achievement blink state
    private boolean achievementFlashing;
    private int lastMilestone;
    private int flashIterations;
    private double flashTimerMs;

    // Horizon & Night mode state
    private final double[] horizonX = {0.0, STAGE_WIDTH};
    private final boolean[] horizonBumpy = {false, true};
    private boolean nightActive;
    private double invertTimerMs;
    private int lastInvertMilestone;
    private float nightOpacity;
    private int moonPhase;
    private double moonX = STAGE_WIDTH - 50.0;
    private final double[] starX = {180.0, 440.0};
    private final int[] starY = {24, 52};

    public DinoRunGamePanel(UiSession session, Runnable onShowRanking, Runnable onBackToLobby,
                            Runnable onCloseWindow) {
        super(new BorderLayout(0, 14));
        this.session = session;
        this.onShowRanking = onShowRanking;
        this.onBackToLobby = onBackToLobby;
        setBackground(BG);
        pad(this, 24);

        JPanel topBar = panel(new BorderLayout(12, 0));
        JPanel titleGroup = panel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleGroup.add(title("공룡 달리기", 18));
        topBar.add(titleGroup, BorderLayout.WEST);

        JPanel rightHud = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHud.add(bestLabel);
        rightHud.add(scoreLabel);
        rightHud.add(button("게임 선택", false, () -> {
            stopGame();
            this.onBackToLobby.run();
        }));
        rightHud.add(GameView.exitButton(() -> {
            stopGame();
            onCloseWindow.run();
        }));
        topBar.add(rightHud, BorderLayout.EAST);
        GameFrame.installDragHandle(topBar);
        add(topBar, BorderLayout.NORTH);

        JPanel boardFrame = card(Color.WHITE, 26);
        boardFrame.setLayout(new BorderLayout());
        pad(boardFrame, 12);
        boardFrame.add(canvas, BorderLayout.CENTER);
        add(boardFrame, BorderLayout.CENTER);

        JPanel footer = panel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        footer.add(statusLabel, BorderLayout.WEST);

        JPanel actions = panel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.add(button("랭킹 확인", false, this.onShowRanking));
        restartButton = button("다시 하기", true, this::startNewRound);
        restartButton.putClientProperty("gradient", true);
        restartButton.setPreferredSize(new Dimension(118, 38));
        actions.add(Box.createHorizontalStrut(2));
        actions.add(restartButton);
        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        loop = new Timer(TICK_MS, event -> step());
        installKeyBindings();
    }

    public void startNewRound() {
        loop.stop();
        obstacles.clear();
        clouds.clear();
        clouds.add(new Cloud(random));
        obstacleHistory.clear();

        currentSpeed = START_SPEED;
        distanceRan = 0.0;
        runningTimeMs = 0.0;
        score = 0;

        trexState = TrexState.RUNNING;
        trexY = TREX_GROUND_Y;
        jumpVelocity = 0.0;
        jumping = false;
        ducking = false;
        speedDrop = false;
        reachedMinHeight = false;
        duckKeyDown = false;
        trexFrame = 0;
        trexAnimTimer = 0.0;

        achievementFlashing = false;
        lastMilestone = 0;
        flashIterations = 0;
        flashTimerMs = 0.0;

        horizonX[0] = 0.0;
        horizonX[1] = STAGE_WIDTH;
        horizonBumpy[0] = false;
        horizonBumpy[1] = random.nextBoolean();

        nightActive = false;
        invertTimerMs = 0.0;
        lastInvertMilestone = 0;
        nightOpacity = 0.0f;
        moonX = STAGE_WIDTH - 50.0;
        placeStars();

        running = true;
        gameOver = false;
        resultSubmitted = false;
        crashedAtMs = 0L;
        lastTickMs = System.currentTimeMillis();

        restartButton.setEnabled(false);
        updateHud();
        statusLabel.setText("Space · ↑ 점프 · ↓ 숙이기");
        loop.start();
        canvas.repaint();
    }

    public void stopGame() {
        loop.stop();
        running = false;
        duckKeyDown = false;
    }

    public void markResultSaved() {
    }

    public boolean isRunning() {
        return running;
    }

    private void installKeyBindings() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        bindKey(inputMap, actionMap, "SPACE", "pressedJump", this::onJumpPressed);
        bindKey(inputMap, actionMap, "UP", "pressedJump", this::onJumpPressed);
        bindKey(inputMap, actionMap, "released SPACE", "releasedJump", this::onJumpReleased);
        bindKey(inputMap, actionMap, "released UP", "releasedJump", this::onJumpReleased);
        bindKey(inputMap, actionMap, "DOWN", "pressedDuck", this::onDuckPressed);
        bindKey(inputMap, actionMap, "released DOWN", "releasedDuck", this::onDuckReleased);
    }

    private void bindKey(InputMap inputMap, ActionMap actionMap, String stroke,
                         String actionName, Runnable handler) {
        inputMap.put(KeyStroke.getKeyStroke(stroke), actionName);
        if (actionMap.get(actionName) == null) {
            actionMap.put(actionName, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!isShowing()) return;
                    handler.run();
                }
            });
        }
    }

    private void onJumpPressed() {
        if (gameOver) {
            if (System.currentTimeMillis() - crashedAtMs >= GAMEOVER_CLEAR_TIME_MS) {
                startNewRound();
            }
            return;
        }
        if (!running) {
            startNewRound();
            return;
        }
        if (!jumping && !ducking) {
            setTrexState(TrexState.JUMPING);
            jumpVelocity = TREX_INITIAL_JUMP_VELOCITY - (currentSpeed / 10.0);
            jumping = true;
            reachedMinHeight = false;
            speedDrop = false;
        }
    }

    private void onJumpReleased() {
        if (!running || !jumping) return;
        if (reachedMinHeight && jumpVelocity < TREX_DROP_VELOCITY) {
            jumpVelocity = TREX_DROP_VELOCITY;
        }
    }

    private void onDuckPressed() {
        duckKeyDown = true;
        if (!running || gameOver) return;
        if (jumping && !speedDrop) {
            speedDrop = true;
            jumpVelocity = 1.0;
        } else if (!jumping && !ducking) {
            ducking = true;
            setTrexState(TrexState.DUCKING);
        }
    }

    private void onDuckReleased() {
        duckKeyDown = false;
        speedDrop = false;
        if (!running || gameOver) return;
        if (ducking) {
            ducking = false;
            setTrexState(jumping ? TrexState.JUMPING : TrexState.RUNNING);
        }
    }

    private void setTrexState(TrexState nextState) {
        if (trexState != nextState) {
            trexState = nextState;
            trexFrame = 0;
            trexAnimTimer = 0.0;
        }
    }

    private void step() {
        if (!running || gameOver) {
            loop.stop();
            return;
        }
        long nowMs = System.currentTimeMillis();
        double deltaTime = Math.max(1.0, Math.min(40.0, (double) (nowMs - lastTickMs)));
        lastTickMs = nowMs;

        runningTimeMs += deltaTime;
        if (jumping) {
            updateJump(deltaTime);
        }
        updateTrexAnimation(deltaTime);

        updateHorizon(deltaTime);
        updateClouds(deltaTime);

        boolean hasObstacles = runningTimeMs > CLEAR_TIME_MS;
        if (hasObstacles) {
            updateObstacles(deltaTime);
        }

        if (hasObstacles && detectAnyCollision()) {
            finishRound();
            canvas.repaint();
            return;
        }

        distanceRan += currentSpeed * deltaTime / MS_PER_FRAME;
        if (currentSpeed < MAX_SPEED) {
            currentSpeed = Math.min(MAX_SPEED, currentSpeed + ACCELERATION * (deltaTime / MS_PER_FRAME));
        }

        updateDistanceScore(deltaTime);
        updateNightMode(deltaTime);
        canvas.repaint();
    }

    private void updateJump(double deltaTime) {
        double framesElapsed = deltaTime / trexState.msPerFrame;
        if (speedDrop) {
            trexY += jumpVelocity * TREX_SPEED_DROP_COEFFICIENT * framesElapsed;
        } else {
            trexY += jumpVelocity * framesElapsed;
        }
        jumpVelocity += TREX_GRAVITY * framesElapsed;

        if (trexY < (TREX_GROUND_Y - TREX_MIN_JUMP_HEIGHT_OFFSET) || speedDrop) {
            reachedMinHeight = true;
        }
        if (trexY < TREX_MAX_JUMP_Y || speedDrop) {
            if (reachedMinHeight && jumpVelocity < TREX_DROP_VELOCITY) {
                jumpVelocity = TREX_DROP_VELOCITY;
            }
        }
        if (trexY >= TREX_GROUND_Y) {
            trexY = TREX_GROUND_Y;
            jumpVelocity = 0.0;
            jumping = false;
            boolean resumeDuck = speedDrop && duckKeyDown;
            speedDrop = false;
            ducking = resumeDuck;
            setTrexState(resumeDuck ? TrexState.DUCKING : TrexState.RUNNING);
        }
    }

    private void updateTrexAnimation(double deltaTime) {
        trexAnimTimer += deltaTime;
        if (trexAnimTimer >= trexState.msPerFrame) {
            trexFrame = (trexFrame + 1) % trexState.frameOffsets.length;
            trexAnimTimer = 0.0;
        }
    }

    private void updateHorizon(double deltaTime) {
        double increment = currentSpeed * (FPS / 1000.0) * deltaTime;
        horizonX[0] -= increment;
        horizonX[1] -= increment;
        if (horizonX[0] <= -STAGE_WIDTH) {
            horizonX[0] += STAGE_WIDTH * 2.0;
            horizonBumpy[0] = random.nextBoolean();
        }
        if (horizonX[1] <= -STAGE_WIDTH) {
            horizonX[1] += STAGE_WIDTH * 2.0;
            horizonBumpy[1] = random.nextBoolean();
        }
    }

    private void updateClouds(double deltaTime) {
        double cloudSpeed = 0.2 * (deltaTime / MS_PER_FRAME) * currentSpeed;
        Iterator<Cloud> iterator = clouds.iterator();
        while (iterator.hasNext()) {
            Cloud cloud = iterator.next();
            cloud.xPos -= cloudSpeed;
            if (cloud.xPos + DinoSprites.LOGICAL_CLOUD_W < 0) {
                iterator.remove();
            }
        }
        if (clouds.isEmpty()) {
            clouds.add(new Cloud(random));
        } else if (clouds.size() < 6) {
            Cloud last = clouds.getLast();
            if (STAGE_WIDTH - last.xPos > last.cloudGap && random.nextDouble() < 0.5) {
                clouds.add(new Cloud(random));
            }
        }
    }

    private void updateObstacles(double deltaTime) {
        double baseIncrement = (FPS / 1000.0) * deltaTime;
        Iterator<Obstacle> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Obstacle obstacle = iterator.next();
            double effectiveSpeed = currentSpeed + obstacle.speedOffset;
            obstacle.xPos -= effectiveSpeed * baseIncrement;
            if (obstacle.type.numFrames > 1) {
                obstacle.animTimer += deltaTime;
                if (obstacle.animTimer >= obstacle.type.frameRateMs) {
                    obstacle.currentFrame = (obstacle.currentFrame + 1) % obstacle.type.numFrames;
                    obstacle.animTimer = 0.0;
                }
            }
            if (obstacle.xPos + obstacle.width <= 0) {
                iterator.remove();
            }
        }

        if (obstacles.isEmpty()) {
            addNewObstacle();
        } else {
            Obstacle last = obstacles.getLast();
            if (!last.followingCreated && (last.xPos + last.width + last.gap) < STAGE_WIDTH) {
                addNewObstacle();
                last.followingCreated = true;
            }
        }
    }

    private void addNewObstacle() {
        ObstacleType[] types = ObstacleType.values();
        ObstacleType chosen = null;
        for (int attempt = 0; attempt < 16; attempt++) {
            ObstacleType candidate = types[random.nextInt(types.length)];
            if (currentSpeed >= candidate.minSpeed && !isDuplicated(candidate)) {
                chosen = candidate;
                break;
            }
        }
        if (chosen == null) {
            chosen = random.nextBoolean() ? ObstacleType.CACTUS_SMALL : ObstacleType.CACTUS_LARGE;
        }

        int size = random.nextInt(MAX_OBSTACLE_LENGTH) + 1;
        if (size > 1 && chosen.multipleSpeed > currentSpeed) {
            size = 1;
        }
        obstacles.add(new Obstacle(chosen, size, currentSpeed, random));
        obstacleHistory.addFirst(chosen);
        while (obstacleHistory.size() > MAX_OBSTACLE_DUPLICATION) {
            obstacleHistory.removeLast();
        }
    }

    private boolean isDuplicated(ObstacleType candidate) {
        if (obstacleHistory.size() < MAX_OBSTACLE_DUPLICATION) return false;
        for (ObstacleType recent : obstacleHistory) {
            if (recent != candidate) return false;
        }
        return true;
    }

    private boolean detectAnyCollision() {
        for (Obstacle obstacle : obstacles) {
            if (detectCollision(obstacle)) {
                return true;
            }
        }
        return false;
    }

    private boolean detectCollision(Obstacle obstacle) {
        int trexX = TREX_START_X + 1;
        int trexYInt = (int) Math.round(trexY) + 1;
        int trexWidth = (ducking ? DinoSprites.LOGICAL_TREX_DUCK_WIDTH : DinoSprites.LOGICAL_TREX_WIDTH) - 2;
        int trexHeight = DinoSprites.LOGICAL_TREX_HEIGHT - 2;

        int obsX = (int) Math.round(obstacle.xPos) + 1;
        int obsY = obstacle.yPos + 1;
        int obsWidth = obstacle.width - 2;
        int obsHeight = obstacle.type.height - 2;

        if (!boxesIntersect(trexX, trexYInt, trexWidth, trexHeight, obsX, obsY, obsWidth, obsHeight)) {
            return false;
        }

        BoxRect[] trexBoxes = ducking ? TREX_DUCKING_BOXES : TREX_RUNNING_BOXES;
        for (BoxRect tBox : trexBoxes) {
            int tx = trexX + tBox.x();
            int ty = trexYInt + tBox.y();
            for (BoxRect oBox : obstacle.collisionBoxes) {
                int ox = obsX + oBox.x();
                int oy = obsY + oBox.y();
                if (boxesIntersect(tx, ty, tBox.width(), tBox.height(), ox, oy, oBox.width(), oBox.height())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean boxesIntersect(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private void updateDistanceScore(double deltaTime) {
        int actualDistance = Math.max(0, (int) Math.round(distanceRan * 0.025));
        if (actualDistance != score) {
            score = actualDistance;
            bestScore = Math.max(bestScore, score);
            updateHud();
        }

        if (!achievementFlashing) {
            int milestone = actualDistance / 100;
            if (milestone > 0 && milestone > lastMilestone) {
                lastMilestone = milestone;
                achievementFlashing = true;
                flashTimerMs = 0.0;
                flashIterations = 0;
            }
        } else {
            if (flashIterations <= 3) {
                flashTimerMs += deltaTime;
                if (flashTimerMs > 500.0) {
                    flashTimerMs = 0.0;
                    flashIterations++;
                }
            } else {
                achievementFlashing = false;
                flashIterations = 0;
                flashTimerMs = 0.0;
            }
        }
    }

    private void updateNightMode(double deltaTime) {
        if (!nightActive) {
            int milestone = score / INVERT_DISTANCE;
            if (milestone > 0 && milestone > lastInvertMilestone) {
                lastInvertMilestone = milestone;
                nightActive = true;
                invertTimerMs = 0.0;
                moonPhase = (moonPhase + 1) % 7;
            }
        } else {
            invertTimerMs += deltaTime;
            if (invertTimerMs >= INVERT_FADE_DURATION_MS) {
                nightActive = false;
                invertTimerMs = 0.0;
            }
        }

        float step = NIGHT_FADE_SPEED * (float) (deltaTime / MS_PER_FRAME);
        if (nightActive) {
            nightOpacity = Math.min(1.0f, nightOpacity + step);
        } else {
            nightOpacity = Math.max(0.0f, nightOpacity - step);
        }

        if (nightOpacity > 0.0f) {
            moonX -= 0.25 * (deltaTime / MS_PER_FRAME);
            if (moonX < -DinoSprites.LOGICAL_MOON_W * 2) {
                moonX = STAGE_WIDTH;
            }
            for (int i = 0; i < starX.length; i++) {
                starX[i] -= 0.3 * (deltaTime / MS_PER_FRAME);
                if (starX[i] < -DinoSprites.LOGICAL_STAR_SIZE) {
                    starX[i] = STAGE_WIDTH;
                }
            }
        }
    }

    private void placeStars() {
        int segment = STAGE_WIDTH / starX.length;
        for (int i = 0; i < starX.length; i++) {
            starX[i] = segment * i + random.nextInt(Math.max(1, segment));
            starY[i] = random.nextInt(71);
        }
    }

    private void finishRound() {
        loop.stop();
        running = false;
        gameOver = true;
        ducking = false;
        setTrexState(TrexState.CRASHED);
        crashedAtMs = System.currentTimeMillis();
        bestScore = Math.max(bestScore, score);
        updateHud();
        restartButton.setEnabled(true);
        if (!resultSubmitted) {
            resultSubmitted = true;
            session.saveGameResult(GameType.DINOSAUR_RUN, score);
        }
    }

    private void updateHud() {
        scoreLabel.setText("점수 " + score + "점");
        bestLabel.setText("최고 " + bestScore + "점");
    }

    private final class PlayCanvas extends JPanel {
        PlayCanvas() {
            setOpaque(true);
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics source) {
            super.paintComponent(source);
            Graphics2D g = graphics(source);
            int pad = 12;
            double availW = Math.max(120, getWidth() - pad * 2);
            double availH = Math.max(60, getHeight() - pad * 2);
            double scale = Math.min(availW / STAGE_WIDTH, availH / STAGE_HEIGHT);
            double stageW = STAGE_WIDTH * scale;
            double stageH = STAGE_HEIGHT * scale;
            double originX = (getWidth() - stageW) / 2.0;
            double originY = (getHeight() - stageH) / 2.0;

            Shape previousClip = g.getClip();
            AffineTransform savedTransform = g.getTransform();
            RoundRectangle2D stageShape = new RoundRectangle2D.Double(originX, originY, stageW, stageH, 16, 16);
            g.setColor(blendColor(DAY_STAGE_BG, NIGHT_STAGE_BG, nightOpacity));
            g.fill(stageShape);

            g.clip(stageShape);
            g.translate(originX, originY);
            g.scale(scale, scale);

            boolean nightSprites = nightOpacity >= 0.5f;
            if (nightOpacity > 0.0f) {
                Composite oldComp = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, nightOpacity));
                for (int i = 0; i < starX.length; i++) {
                    DinoSprites.drawStar(g, nightSprites, i, starX[i], starY[i]);
                }
                DinoSprites.drawMoon(g, nightSprites, moonPhase, moonX, 30);
                g.setComposite(oldComp);
            }

            for (Cloud cloud : clouds) {
                DinoSprites.drawCloud(g, nightSprites, cloud.xPos, cloud.yPos);
            }

            DinoSprites.drawHorizon(g, nightSprites, horizonBumpy[0], horizonX[0], HORIZON_Y);
            DinoSprites.drawHorizon(g, nightSprites, horizonBumpy[1], horizonX[1], HORIZON_Y);

            for (Obstacle obstacle : obstacles) {
                switch (obstacle.type) {
                    case CACTUS_SMALL -> DinoSprites.drawCactusSmall(g, nightSprites, obstacle.size, obstacle.xPos, obstacle.yPos);
                    case CACTUS_LARGE -> DinoSprites.drawCactusLarge(g, nightSprites, obstacle.size, obstacle.xPos, obstacle.yPos);
                    case PTERODACTYL -> DinoSprites.drawPterodactyl(g, nightSprites, obstacle.currentFrame, obstacle.xPos, obstacle.yPos);
                }
            }

            int frameOffset = trexState.frameOffsets[Math.min(trexFrame, trexState.frameOffsets.length - 1)];
            DinoSprites.drawTrex(g, nightSprites, frameOffset, ducking && !gameOver, TREX_START_X, trexY);

            paintScoreMeters(g, nightSprites);

            if (gameOver) {
                double goX = (STAGE_WIDTH - DinoSprites.LOGICAL_GAME_OVER_W) / 2.0;
                double goY = (STAGE_HEIGHT - 25.0) / 3.0;
                DinoSprites.drawGameOverText(g, nightSprites, goX, goY);
            }

            g.setTransform(savedTransform);
            g.setClip(previousClip);
            g.setColor(LINE);
            g.draw(stageShape);
            g.dispose();
        }

        private void paintScoreMeters(Graphics2D g, boolean nightSprites) {
            int maxDigits = 5;
            double startX = STAGE_WIDTH - 11.0 * (maxDigits + 1);
            if (bestScore > 0) {
                Composite oldComp = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                double hiX = startX - 11.0 * (maxDigits * 2 + 1) + 10.0;
                DinoSprites.drawDigit(g, nightSprites, 10, hiX, 10);
                DinoSprites.drawDigit(g, nightSprites, 11, hiX + 11.0, 10);
                paintNumberDigits(g, nightSprites, bestScore, hiX + 33.0);
                g.setComposite(oldComp);
            }

            boolean flashVisible = !achievementFlashing || flashTimerMs >= 250.0;
            if (flashVisible) {
                int displayScore = achievementFlashing ? lastMilestone * 100 : score;
                paintNumberDigits(g, nightSprites, displayScore, startX);
            }
        }

        private void paintNumberDigits(Graphics2D g, boolean nightSprites, int value, double startX) {
            String padded = String.format("%05d", Math.min(99999, Math.max(0, value)));
            for (int i = 0; i < padded.length(); i++) {
                int digit = padded.charAt(i) - '0';
                DinoSprites.drawDigit(g, nightSprites, digit, startX + i * 11.0, 10);
            }
        }

        private Color blendColor(Color day, Color night, float ratio) {
            float clamped = Math.max(0.0f, Math.min(1.0f, ratio));
            int r = Math.round(day.getRed() * (1.0f - clamped) + night.getRed() * clamped);
            int gr = Math.round(day.getGreen() * (1.0f - clamped) + night.getGreen() * clamped);
            int b = Math.round(day.getBlue() * (1.0f - clamped) + night.getBlue() * clamped);
            return new Color(r, gr, b);
        }
    }
}
