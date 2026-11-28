package bubbling.client.ui.game;

import bubbling.client.ui.model.CharacterChoice;
import bubbling.common.enums.ProfileCharacter;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/** Favorite collectible items mapped to each Bubbling character. */
public enum FavoriteItem {
    BEAR(ProfileCharacter.BEAR, "곰", "연어", new Color(255, 238, 232)),
    BUNNY(ProfileCharacter.BUNNY, "토끼", "당근", new Color(255, 241, 224)),
    CAT(ProfileCharacter.CAT, "고양이", "쥐", new Color(242, 241, 248)),
    FOX(ProfileCharacter.FOX, "여우", "포도", new Color(243, 234, 255)),
    FROG(ProfileCharacter.FROG, "개구리", "파리", new Color(235, 248, 242)),
    PENGUIN(ProfileCharacter.PENGUIN, "펭귄", "물고기", new Color(233, 245, 255));

    private static final Color SALMON_FILL = new Color(250, 128, 102);
    private static final Color SALMON_SKIN = new Color(210, 82, 60);
    private static final Color SALMON_STRIPE = new Color(255, 236, 228);
    private static final Color CARROT_ORANGE = new Color(255, 138, 43);
    private static final Color CARROT_SHADOW = new Color(224, 102, 20);
    private static final Color LEAF_GREEN = new Color(75, 181, 86);
    private static final Color MOUSE_GRAY = new Color(148, 154, 172);
    private static final Color MOUSE_EAR_PINK = new Color(244, 178, 196);
    private static final Color GRAPE_DARK = new Color(115, 66, 227);
    private static final Color GRAPE_LIGHT = new Color(149, 108, 246);
    private static final Color FLY_BODY = new Color(58, 61, 77);
    private static final Color FLY_WING = new Color(218, 241, 255, 210);
    private static final Color FLY_WING_LINE = new Color(132, 180, 212);
    private static final Color FLY_EYE = new Color(216, 76, 102);
    private static final Color FISH_BLUE = new Color(61, 169, 252);
    private static final Color FISH_LIGHT = new Color(132, 206, 255);
    private static final Color DARK_EYE = new Color(41, 38, 58);

    private final ProfileCharacter profile;
    private final String characterName;
    private final String itemName;
    private final Color tint;

    FavoriteItem(ProfileCharacter profile, String characterName, String itemName, Color tint) {
        this.profile = profile;
        this.characterName = characterName;
        this.itemName = itemName;
        this.tint = tint;
    }

    public ProfileCharacter profile() {
        return profile;
    }

    public String characterName() {
        return characterName;
    }

    public String itemName() {
        return itemName;
    }

    public Color tint() {
        return tint;
    }

    public static FavoriteItem forProfile(ProfileCharacter profile) {
        for (FavoriteItem item : values()) {
            if (item.profile == profile) {
                return item;
            }
        }
        return BUNNY;
    }

    public static FavoriteItem forChoice(CharacterChoice choice) {
        return choice == null ? BUNNY : forProfile(choice.profile());
    }

    public Icon icon(int size) {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                paint((Graphics2D) g, x, y, size);
            }
        };
    }

    public void paint(Graphics2D source, int x, int y, int size) {
        Graphics2D g = (Graphics2D) source.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x, y);
            double scale = size / 40.0;
            g.scale(scale, scale);
            switch (this) {
                case BEAR -> paintSalmon(g);
                case BUNNY -> paintCarrot(g);
                case CAT -> paintMouse(g);
                case FOX -> paintGrape(g);
                case FROG -> paintFly(g);
                case PENGUIN -> paintFish(g);
            }
        } finally {
            g.dispose();
        }
    }

    private static void paintSalmon(Graphics2D g) {
        Path2D fillet = new Path2D.Float();
        fillet.moveTo(6, 25);
        fillet.curveTo(6, 14, 17, 10, 32, 12);
        fillet.curveTo(35, 16, 35, 24, 30, 29);
        fillet.curveTo(20, 32, 9, 30, 6, 25);
        fillet.closePath();
        g.setColor(SALMON_FILL);
        g.fill(fillet);

        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(SALMON_SKIN);
        Path2D rim = new Path2D.Float();
        rim.moveTo(7, 26);
        rim.curveTo(14, 31, 24, 31, 31, 28);
        g.draw(rim);

        g.setColor(SALMON_STRIPE);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(13, 16, 17, 26);
        g.drawLine(20, 14, 24, 26);
        g.drawLine(27, 14, 29, 24);
    }

    private static void paintCarrot(Graphics2D g) {
        g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(LEAF_GREEN);
        g.drawLine(24, 14, 28, 7);
        g.drawLine(26, 15, 33, 11);
        g.drawLine(22, 13, 21, 6);

        Path2D root = new Path2D.Float();
        root.moveTo(21, 13);
        root.curveTo(26, 12, 30, 17, 27, 22);
        root.lineTo(11, 34);
        root.curveTo(8, 35, 7, 33, 8, 30);
        root.closePath();
        g.setColor(CARROT_ORANGE);
        g.fill(root);

        g.setColor(CARROT_SHADOW);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(19, 19, 22, 22);
        g.drawLine(14, 24, 17, 27);
    }

    private static void paintMouse(Graphics2D g) {
        g.setColor(MOUSE_GRAY);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D tail = new Path2D.Float();
        tail.moveTo(27, 25);
        tail.curveTo(34, 25, 36, 18, 31, 15);
        g.draw(tail);

        g.fillOval(11, 17, 18, 13);
        g.fillOval(9, 11, 7, 7);
        g.fillOval(16, 10, 7, 7);
        g.setColor(MOUSE_EAR_PINK);
        g.fillOval(11, 13, 3, 3);
        g.fillOval(18, 12, 3, 3);

        g.setColor(DARK_EYE);
        g.fillOval(11, 21, 2, 2);
        g.setColor(MOUSE_EAR_PINK);
        g.fillOval(7, 22, 3, 3);
    }

    private static void paintGrape(Graphics2D g) {
        g.setColor(LEAF_GREEN);
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(20, 7, 20, 12);
        g.fillOval(21, 7, 7, 4);

        int[][] berries = {
                {13, 12}, {19, 12}, {25, 12},
                {16, 17}, {22, 17},
                {19, 22},
                {19, 27}
        };
        for (int i = 0; i < berries.length; i++) {
            int bx = berries[i][0] - 4;
            int by = berries[i][1];
            g.setColor(i % 2 == 0 ? GRAPE_DARK : GRAPE_LIGHT);
            g.fillOval(bx, by, 8, 8);
            g.setColor(new Color(235, 224, 255, 170));
            g.fillOval(bx + 2, by + 2, 2, 2);
        }
    }

    private static void paintFly(Graphics2D g) {
        g.setColor(FLY_WING);
        g.fillOval(9, 10, 11, 8);
        g.fillOval(20, 10, 11, 8);
        g.setColor(FLY_WING_LINE);
        g.setStroke(new BasicStroke(1.3f));
        g.drawOval(9, 10, 11, 8);
        g.drawOval(20, 10, 11, 8);

        g.setColor(FLY_BODY);
        g.fillOval(14, 16, 12, 15);
        g.fillOval(15, 11, 10, 9);

        g.setColor(FLY_EYE);
        g.fillOval(16, 12, 3, 3);
        g.fillOval(21, 12, 3, 3);
    }

    private static void paintFish(Graphics2D g) {
        Path2D tail = new Path2D.Float();
        tail.moveTo(26, 20);
        tail.lineTo(35, 13);
        tail.lineTo(33, 20);
        tail.lineTo(35, 27);
        tail.closePath();
        g.setColor(FISH_BLUE);
        g.fill(tail);

        Path2D fin = new Path2D.Float();
        fin.moveTo(16, 13);
        fin.lineTo(22, 9);
        fin.lineTo(23, 14);
        fin.closePath();
        g.fill(fin);

        g.fillOval(7, 13, 22, 14);
        g.setColor(FISH_LIGHT);
        g.fillOval(10, 18, 15, 7);

        g.setColor(DARK_EYE);
        g.fillOval(11, 17, 3, 3);
    }
}
