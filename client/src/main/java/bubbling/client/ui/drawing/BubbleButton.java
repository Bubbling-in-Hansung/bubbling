package bubbling.client.ui.drawing;

import bubbling.client.ui.model.UiSession;
import bubbling.client.ui.style.UiFonts;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import static bubbling.client.ui.style.Styles.*;

/** Bubble content uses the same centered stack and percentage anchors as prototype. */
public final class BubbleButton extends JButton {
    private final UiSession.Room room;
    public BubbleButton(UiSession.Room room) {
        super(room.view.title());
        this.room = room;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(room.view.title());
    }

    @Override
    public boolean contains(int x, int y) {
        int s = room.view.size();
        return new java.awt.geom.Ellipse2D.Double(BubbleGeometry.SHADOW_PADDING, BubbleGeometry.SHADOW_PADDING, s,
                s).contains(x, y);
    }

    @Override
    protected void paintComponent(Graphics source) {
        Graphics2D g = graphics(source);
        int s = room.view.size(), o = BubbleGeometry.SHADOW_PADDING, cx = o + s / 2;
        BubbleArt.paint(g, o, o, s, room.view.tint(), getModel().isRollover() || isSelected() || hasFocus());
        g.setFont(UiFonts.body(11, 600));
        FontMetrics fm = g.getFontMetrics();
        var lines = new ArrayList<String>();
        String line = "";
        int width = s - 40;
        for (char ch : room.view.title().toCharArray()) {
            if (!line.isEmpty() && fm.stringWidth(line + ch) > width) {
                lines.add(line);
                line = "";
            }
            line += ch;
        }
        if (!line.isEmpty()) {
            lines.add(line);
        }
        int lineHeight = 16, groupHeight = 17 + 5 + lines.size() * lineHeight, top = o + (s - groupHeight) / 2;
        pill(g, room.view.category(), cx, top, 8, new Color(255, 255, 255, 107), new Color(78, 66, 103, 184), false);
        g.setFont(UiFonts.body(11, 600));
        g.setColor(color("4e4860"));
        int baseline = top + 22 + fm.getAscent();
        for (String text : lines) {
            g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2, baseline);
            baseline += lineHeight;
        }
        int my = o + (int) (s * .87);
        g.setColor(new Color(77, 67, 98, 166));
        g.setFont(font(8, false));
        String count = room.people + "/" + room.view.capacity();
        int tw = g.getFontMetrics().stringWidth(count), start = cx - (tw + 17) / 2;
        g.setStroke(new BasicStroke(.8f));
        g.drawOval(start + 2, my - 8, 4, 4);
        g.drawArc(start, my - 3, 8, 7, 0, 180);
        g.drawArc(start + 7, my - 3, 5, 6, 0, 100);
        g.drawString(count, start + 15, my);
        if (room.view.owner()) {
            pill(g, "내 방", o + (int) (s * .16) + 14, o + (int) (s * .14), 7, color("7952dc"), Color.WHITE, true);
        }
        if (room.view.privateRoom()) {
            int x = o + (int) (s * .84) - 22, y = o + (int) (s * .17);
            g.setColor(new Color(255, 255, 255, 191));
            g.fillOval(x, y, 22, 22);
            g.setColor(color("6d51b9"));
            g.setStroke(new BasicStroke(1));
            g.drawRoundRect(x + 7, y + 10, 8, 7, 2, 2);
            g.drawArc(x + 9, y + 5, 4, 8, 0, 180);
            g.fillOval(x + 10, y + 12, 2, 2);
        }
        g.dispose();
    }

    private static void pill(Graphics2D g, String text, int cx, int y, int size, Color fill, Color ink, boolean bold) {
        g.setFont(font(size, bold));
        int width = g.getFontMetrics().stringWidth(text) + 14, height = size + 7;
        g.setColor(fill);
        g.fillRoundRect(cx - width / 2, y, width, height, 8, 8);
        g.setColor(ink);
        g.drawString(text, cx - width / 2 + 7, y + size + 2);
    }
}
