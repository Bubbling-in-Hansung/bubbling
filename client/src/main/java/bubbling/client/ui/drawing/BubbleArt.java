package bubbling.client.ui.drawing;

import java.awt.*;
import java.awt.geom.*;
import static bubbling.client.ui.style.Styles.*;

/** Translucent CSS gradient, inset rim and diffuse shadow translated to Java2D. */
public final class BubbleArt {
    private static final String[][] COLORS = {
        {
            "dccfff", "a88bf5", "7d5bd6"
        }, {
            "ffdde9", "f6a8c4", "e183a9"
        }, {
            "d7efff", "94cfef", "71b2d9"
        }, {
            "daf9f1", "91ddca", "68beaa"
        }, {
            "ffebda", "f6be99", "e29d7b"
        }
    };
    private BubbleArt() {
    }

    private static Color alpha(String hex, int alpha) {
        Color c = color(hex);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public static void paint(Graphics2D g, int x, int y, int s, int tint, boolean selected) {
        String[] c = COLORS[Math.floorMod(tint, COLORS.length)];
        for (int k = 28; k > 0; k -= 2) {
            g.setColor(new Color(95, 72, 150, selected ? 2 : 1));
            g.fill(new Ellipse2D.Float(x - k / 2f, y + 10 - k / 2f, s + k, s + k));
        }
        Shape orb = new Ellipse2D.Float(x, y, s, s);
        g.setPaint(new RadialGradientPaint(x + s * .28f, y + s * .22f, s * .85f, new float[] {
            0, .04f, .34f, .76f, 1
        }, new Color[] {
            new Color(255, 255, 255, 242), new Color(255, 255, 255, 242), alpha(c[0], 212), alpha(c[1], 136), alpha(c[2],
                    94)
        }));
        g.fill(orb);
        Shape clip = g.getClip();
        g.clip(orb);
        g.setPaint(new RadialGradientPaint(x + s * .36f, y + s * .3f, s * .8f, new float[] {
            0, .72f, 1
        }, new Color[] {
            new Color(255, 255, 255, 0), new Color(255, 255, 255, 0), new Color(110, 80, 180, 18)
        }));
        g.fill(orb);
        g.setClip(clip);
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(255, 255, 255, 199));
        g.draw(orb);
        g.setColor(new Color(255, 255, 255, 87));
        g.drawOval(x + 5, y + 5, s - 10, s - 10);
        var transform = g.getTransform();
        g.rotate(Math.toRadians(- 25), x + s * .35, y + s * .195);
        g.setColor(new Color(255, 255, 255, 184));
        g.fill(new Ellipse2D.Double(x + s * .25, y + s * .14, s * .20, s * .11));
        g.setTransform(transform);
        if (selected) {
            g.setColor(new Color(124, 77, 255, 28));
            g.setStroke(new BasicStroke(3));
            g.drawOval(x - 2, y - 2, s + 4, s + 4);
        }
    }
}
