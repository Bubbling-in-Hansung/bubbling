/*
 * Chromium T-Rex Runner Sprite Definitions (HDPI 2x sheet).
 * Copyright (c) 2014 The Chromium Authors. All rights reserved.
 * Extracted from Chromium neterror resources (wayou/t-rex-runner, BSD 3-Clause).
 * See /game/THIRD_PARTY_LICENSE_TREX.txt for full license text.
 */
package bubbling.client.ui.game;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/** Loads Chromium's official 2x offline T-Rex sprite sheet for day and night rendering. */
public final class DinoSprites {
    public static final int LOGICAL_TREX_WIDTH = 44;
    public static final int LOGICAL_TREX_DUCK_WIDTH = 59;
    public static final int LOGICAL_TREX_HEIGHT = 47;

    public static final int LOGICAL_CACTUS_SMALL_W = 17;
    public static final int LOGICAL_CACTUS_SMALL_H = 35;
    public static final int LOGICAL_CACTUS_LARGE_W = 25;
    public static final int LOGICAL_CACTUS_LARGE_H = 50;

    public static final int LOGICAL_PTERO_W = 46;
    public static final int LOGICAL_PTERO_H = 40;

    public static final int LOGICAL_CLOUD_W = 46;
    public static final int LOGICAL_CLOUD_H = 14;

    public static final int LOGICAL_HORIZON_W = 600;
    public static final int LOGICAL_HORIZON_H = 12;

    public static final int LOGICAL_MOON_W = 20;
    public static final int LOGICAL_MOON_H = 40;
    public static final int LOGICAL_STAR_SIZE = 9;

    public static final int LOGICAL_DIGIT_W = 10;
    public static final int LOGICAL_DIGIT_H = 13;
    public static final int LOGICAL_GAME_OVER_W = 191;
    public static final int LOGICAL_GAME_OVER_H = 11;
    public static final int LOGICAL_RESTART_W = 36;
    public static final int LOGICAL_RESTART_H = 32;

    private static final String SPRITE_PATH = "/game/200-offline-sprite.png";
    private static final int SCALE = 2;

    private static final int RESTART_X = 2, RESTART_Y = 2;
    private static final int CLOUD_X = 166, CLOUD_Y = 2;
    private static final int PTERO_X = 260, PTERO_Y = 2;
    private static final int CACTUS_SMALL_X = 446, CACTUS_SMALL_Y = 2;
    private static final int CACTUS_LARGE_X = 652, CACTUS_LARGE_Y = 2;
    private static final int MOON_X = 954, MOON_Y = 2;
    private static final int STAR_X = 1276, STAR_Y = 2;
    private static final int TEXT_X = 1294, TEXT_Y = 2;
    private static final int TREX_X = 1678, TREX_Y = 2;
    private static final int HORIZON_X = 2, HORIZON_Y = 104;

    private static final int[] MOON_PHASES = {140, 120, 100, 60, 40, 20, 0};

    private static final BufferedImage DAY_SHEET = loadSheet();
    private static final BufferedImage NIGHT_SHEET = DAY_SHEET == null ? null : invertColors(DAY_SHEET);

    private DinoSprites() {
    }

    public static boolean isLoaded() {
        return DAY_SHEET != null && NIGHT_SHEET != null;
    }

    public static void drawTrex(Graphics2D g, boolean night, int frameOffsetX, boolean ducking, double dx, double dy) {
        int logicalW = ducking ? LOGICAL_TREX_DUCK_WIDTH : LOGICAL_TREX_WIDTH;
        drawRegion(g, night,
                TREX_X + frameOffsetX * SCALE, TREX_Y,
                logicalW * SCALE, LOGICAL_TREX_HEIGHT * SCALE,
                dx, dy, logicalW, LOGICAL_TREX_HEIGHT);
    }

    public static void drawCactusSmall(Graphics2D g, boolean night, int size, double dx, double dy) {
        int clamped = Math.max(1, Math.min(3, size));
        int sw = LOGICAL_CACTUS_SMALL_W * SCALE * clamped;
        int sx = CACTUS_SMALL_X + (int) Math.round((LOGICAL_CACTUS_SMALL_W * SCALE * clamped) * (0.5 * (clamped - 1)));
        drawRegion(g, night, sx, CACTUS_SMALL_Y, sw, LOGICAL_CACTUS_SMALL_H * SCALE,
                dx, dy, LOGICAL_CACTUS_SMALL_W * clamped, LOGICAL_CACTUS_SMALL_H);
    }

    public static void drawCactusLarge(Graphics2D g, boolean night, int size, double dx, double dy) {
        int clamped = Math.max(1, Math.min(3, size));
        int sw = LOGICAL_CACTUS_LARGE_W * SCALE * clamped;
        int sx = CACTUS_LARGE_X + (int) Math.round((LOGICAL_CACTUS_LARGE_W * SCALE * clamped) * (0.5 * (clamped - 1)));
        drawRegion(g, night, sx, CACTUS_LARGE_Y, sw, LOGICAL_CACTUS_LARGE_H * SCALE,
                dx, dy, LOGICAL_CACTUS_LARGE_W * clamped, LOGICAL_CACTUS_LARGE_H);
    }

    public static void drawPterodactyl(Graphics2D g, boolean night, int frame, double dx, double dy) {
        int safeFrame = Math.floorMod(frame, 2);
        drawRegion(g, night,
                PTERO_X + safeFrame * LOGICAL_PTERO_W * SCALE, PTERO_Y,
                LOGICAL_PTERO_W * SCALE, LOGICAL_PTERO_H * SCALE,
                dx, dy, LOGICAL_PTERO_W, LOGICAL_PTERO_H);
    }

    public static void drawCloud(Graphics2D g, boolean night, double dx, double dy) {
        drawRegion(g, night, CLOUD_X, CLOUD_Y,
                LOGICAL_CLOUD_W * SCALE, LOGICAL_CLOUD_H * SCALE,
                dx, dy, LOGICAL_CLOUD_W, LOGICAL_CLOUD_H);
    }

    public static void drawHorizon(Graphics2D g, boolean night, boolean bumpy, double dx, double dy) {
        int offset = bumpy ? LOGICAL_HORIZON_W * SCALE : 0;
        drawRegion(g, night, HORIZON_X + offset, HORIZON_Y,
                LOGICAL_HORIZON_W * SCALE, LOGICAL_HORIZON_H * SCALE,
                dx, dy, LOGICAL_HORIZON_W, LOGICAL_HORIZON_H);
    }

    public static void drawMoon(Graphics2D g, boolean night, int phaseIndex, double dx, double dy) {
        int safePhase = Math.floorMod(phaseIndex, MOON_PHASES.length);
        int logicalW = safePhase == 3 ? LOGICAL_MOON_W * 2 : LOGICAL_MOON_W;
        int sx = MOON_X + MOON_PHASES[safePhase] * SCALE;
        drawRegion(g, night, sx, MOON_Y,
                logicalW * SCALE, LOGICAL_MOON_H * SCALE,
                dx, dy, logicalW, LOGICAL_MOON_H);
    }

    public static void drawStar(Graphics2D g, boolean night, int starIndex, double dx, double dy) {
        int safeIndex = Math.floorMod(starIndex, 2);
        int sy = STAR_Y + LOGICAL_STAR_SIZE * SCALE * safeIndex;
        drawRegion(g, night, STAR_X, sy,
                LOGICAL_STAR_SIZE * SCALE, LOGICAL_STAR_SIZE * SCALE,
                dx, dy, LOGICAL_STAR_SIZE, LOGICAL_STAR_SIZE);
    }

    public static void drawDigit(Graphics2D g, boolean night, int value, double dx, double dy) {
        int sx = TEXT_X + value * LOGICAL_DIGIT_W * SCALE;
        drawRegion(g, night, sx, TEXT_Y,
                LOGICAL_DIGIT_W * SCALE, LOGICAL_DIGIT_H * SCALE,
                dx, dy, LOGICAL_DIGIT_W, LOGICAL_DIGIT_H);
    }

    public static void drawGameOverText(Graphics2D g, boolean night, double dx, double dy) {
        drawRegion(g, night, TEXT_X, TEXT_Y + 13 * SCALE,
                LOGICAL_GAME_OVER_W * SCALE, LOGICAL_GAME_OVER_H * SCALE,
                dx, dy, LOGICAL_GAME_OVER_W, LOGICAL_GAME_OVER_H);
    }

    public static void drawRestartIcon(Graphics2D g, boolean night, double dx, double dy) {
        drawRegion(g, night, RESTART_X, RESTART_Y,
                LOGICAL_RESTART_W * SCALE, LOGICAL_RESTART_H * SCALE,
                dx, dy, LOGICAL_RESTART_W, LOGICAL_RESTART_H);
    }

    private static void drawRegion(Graphics2D g, boolean night,
                                   int sx, int sy, int sw, int sh,
                                   double dx, double dy, double dw, double dh) {
        BufferedImage sheet = night ? NIGHT_SHEET : DAY_SHEET;
        if (sheet == null) return;

        int maxSx = Math.max(0, Math.min(sheet.getWidth(), sx));
        int maxSy = Math.max(0, Math.min(sheet.getHeight(), sy));
        int sx2 = Math.min(sheet.getWidth(), maxSx + sw);
        int sy2 = Math.min(sheet.getHeight(), maxSy + sh);
        if (sx2 <= maxSx || sy2 <= maxSy) return;

        double actualDw = dw * ((double) (sx2 - maxSx) / sw);
        double actualDh = dh * ((double) (sy2 - maxSy) / sh);

        Graphics2D copy = (Graphics2D) g.create();
        try {
            copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            copy.drawImage(sheet,
                    (int) Math.round(dx), (int) Math.round(dy),
                    (int) Math.round(dx + actualDw), (int) Math.round(dy + actualDh),
                    maxSx, maxSy, sx2, sy2, null);
        } finally {
            copy.dispose();
        }
    }

    private static BufferedImage loadSheet() {
        try (InputStream in = DinoSprites.class.getResourceAsStream(SPRITE_PATH)) {
            if (in == null) {
                System.err.println("Missing sprite sheet: " + SPRITE_PATH);
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            System.err.println("Failed to load sprite sheet: " + e.getMessage());
            return null;
        }
    }

    private static BufferedImage invertColors(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage inverted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    inverted.setRGB(x, y, 0);
                    continue;
                }
                int r = 255 - ((argb >>> 16) & 0xFF);
                int g = 255 - ((argb >>> 8) & 0xFF);
                int b = 255 - (argb & 0xFF);
                inverted.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return inverted;
    }
}
