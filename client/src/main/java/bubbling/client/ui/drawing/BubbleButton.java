package bubbling.client.ui.drawing;

import bubbling.client.ui.model.UiSession;
import bubbling.client.ui.style.UiFonts;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import static bubbling.client.ui.style.Styles.*;

/** Bubble content uses the same centered stack and percentage anchors as prototype. */
public final class BubbleButton extends JButton {
    private static final Font TITLE_FONT = UiFonts.bodyAtSize(14.3f, 600);
    private static final Font LABEL_FONT = UiFonts.bodyAtSize(10.4f, 400);
    private static final Font OWNER_FONT = UiFonts.bodyAtSize(9.1f, 700);
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
    public String getToolTipText() {
        return room == null ? super.getToolTipText() : room.view.title();
    }

    @Override
    public boolean contains(int x, int y) {
        int s = room.bubbleDiameter();
        return new java.awt.geom.Ellipse2D.Double(BubbleGeometry.SHADOW_PADDING, BubbleGeometry.SHADOW_PADDING, s,
                s).contains(x, y);
    }

    @Override
    protected void paintComponent(Graphics source) {
        Graphics2D g = graphics(source);
        int s = room.bubbleDiameter(), o = BubbleGeometry.SHADOW_PADDING, cx = o + s / 2;
        BubbleArt.paint(g, o, o, s, room.view.tint(), getModel().isRollover() || isSelected() || hasFocus());
        g.setFont(TITLE_FONT);
        FontMetrics fm = g.getFontMetrics();
        var lines = new ArrayList<String>();
        String line = "";
        int width = s - 40;
        for (int codePoint : room.view.title().codePoints().toArray()) {
            String character = Character.toString(codePoint);
            if (!line.isEmpty() && fm.stringWidth(line + character) > width) {
                lines.add(line);
                line = "";
            }
            line += character;
        }
        if (!line.isEmpty()) {
            lines.add(line);
        }
        int my = o + (int) (s * .87);
        FontMetrics labelMetrics = g.getFontMetrics(LABEL_FONT);
        int contentTop = o + (int) (s * .27);
        int contentBottom = my - labelMetrics.getAscent() - 5;
        int categoryHeight = labelMetrics.getHeight() + 5;
        int lineHeight = fm.getHeight();
        int maxLines = Math.max(1, (contentBottom - contentTop - categoryHeight - 5) / lineHeight);
        if (lines.size() > maxLines) {
            lines.subList(maxLines, lines.size()).clear();
            String last = lines.getLast();
            while (!last.isEmpty() && fm.stringWidth(last + "…") > width) {
                last = last.substring(0, last.offsetByCodePoints(last.length(), -1));
            }
            lines.set(maxLines - 1, last + "…");
        }
        int groupHeight = categoryHeight + 5 + lines.size() * lineHeight;
        int top = contentTop + (contentBottom - contentTop - groupHeight) / 2;
        pill(g, room.view.category(), cx, top, LABEL_FONT, new Color(255, 255, 255, 107), new Color(78, 66, 103, 184));
        g.setFont(TITLE_FONT);
        g.setColor(color("4e4860"));
        int baseline = top + categoryHeight + 5 + fm.getAscent();
        for (String text : lines) {
            g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2, baseline);
            baseline += lineHeight;
        }
        g.setColor(new Color(77, 67, 98, 166));
        g.setFont(LABEL_FONT);
        String count = room.people + "/" + room.view.capacity();
        int tw = g.getFontMetrics().stringWidth(count), start = cx - (tw + 17) / 2;
        g.setStroke(new BasicStroke(.8f));
        g.drawOval(start + 2, my - 8, 4, 4);
        g.drawArc(start, my - 3, 8, 7, 0, 180);
        g.drawArc(start + 7, my - 3, 5, 6, 0, 100);
        g.drawString(count, start + 15, my);
        if (room.view.owner()) {
            pill(g, "내 방", o + (int) (s * .16) + 14, o + (int) (s * .14), OWNER_FONT, color("7952dc"), Color.WHITE);
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

    private static void pill(Graphics2D g, String text, int cx, int y, Font font, Color fill, Color ink) {
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        int width = metrics.stringWidth(text) + 14, height = metrics.getHeight() + 5;
        g.setColor(fill);
        g.fillRoundRect(cx - width / 2, y, width, height, 8, 8);
        g.setColor(ink);
        g.drawString(text, cx - width / 2 + 7, y + metrics.getAscent() + 2);
    }
}
