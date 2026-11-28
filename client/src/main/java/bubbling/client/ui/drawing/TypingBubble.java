package bubbling.client.ui.drawing;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import static bubbling.client.ui.style.Styles.*;

/** Compact speech bubble showing an animated ○ ○ ○ typing indicator above a character. */
public final class TypingBubble extends JPanel {
    private static final int WIDTH = 96;
    private static final int CONTENT_HEIGHT = 18;
    private static final int VERTICAL_PADDING = 10;
    private static final double MAX_CAP_SCALE = 0.28;
    private static final int SLICE_BASE_WIDTH = 474, TOP_SLICE_HEIGHT = 70,
            BOTTOM_SLICE_HEIGHT = 102, BOTTOM_BODY_SLICE_HEIGHT = 67;
    private static final int DOT_COUNT = 3, DOT_DIAMETER = 8, DOT_GAP = 8, ACTIVE_LIFT = 2;
    private static final Color IDLE_RING = color("9b93ad"),
            ACTIVE_FILL = new Color(149, 117, 224, 55);
    private final BufferedImage topImage, bodyImage, bottomImage;
    private final int topHeight, centerHeight, bottomHeight, tailHeight;
    private int frame;

    public TypingBubble() {
        super(null);
        setOpaque(false);
        topImage = MessageBubble.slice("w_top.png");
        bodyImage = MessageBubble.slice("w_center.png");
        bottomImage = MessageBubble.slice("w_c_bottom.png");
        int topSourceWidth = topImage == null ? SLICE_BASE_WIDTH : topImage.getWidth();
        int topSourceHeight = topImage == null ? TOP_SLICE_HEIGHT : topImage.getHeight();
        int bottomSourceHeight = bottomImage == null ? BOTTOM_SLICE_HEIGHT : bottomImage.getHeight();
        int bottomBodySourceHeight = Math.min(BOTTOM_BODY_SLICE_HEIGHT, bottomSourceHeight);
        int tailSourceHeight = Math.max(1, bottomSourceHeight - bottomBodySourceHeight);

        int bodyHeight = CONTENT_HEIGHT + VERTICAL_PADDING * 2;
        double widthScale = (double) WIDTH / topSourceWidth;
        double capScale = Math.min(Math.min(widthScale, MAX_CAP_SCALE),
                (double) bodyHeight / (topSourceHeight + bottomBodySourceHeight));
        topHeight = Math.max(1, (int) Math.round(topSourceHeight * capScale));
        int bottomBodyHeight = Math.max(1, (int) Math.round(bottomBodySourceHeight * capScale));
        tailHeight = Math.max(1, (int) Math.ceil(tailSourceHeight * capScale));
        bottomHeight = bottomBodyHeight + tailHeight;
        centerHeight = Math.max(0, bodyHeight - topHeight - bottomBodyHeight);
        Dimension size = new Dimension(WIDTH, topHeight + centerHeight + bottomHeight);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        getAccessibleContext().setAccessibleName("입력 중");
    }

    public void tick() {
        frame = (frame + 1) % (DOT_COUNT * 2);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics source) {
        super.paintComponent(source);
        Graphics2D g = (Graphics2D) source.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        if (topImage != null && bodyImage != null && bottomImage != null) {
            g.drawImage(topImage, 0, 0, getWidth(), topHeight, null);
            if (centerHeight > 0) {
                g.drawImage(bodyImage, 0, topHeight, getWidth(), centerHeight, null);
            }
            g.drawImage(bottomImage, 0, topHeight + centerHeight, getWidth(), bottomHeight, null);
        } else {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            int bottom = getHeight() - tailHeight;
            g.fillRoundRect(0, 0, getWidth(), bottom, Math.min(18, topHeight), Math.min(18, topHeight));
            int x = getWidth() / 2;
            g.fillPolygon(new int[] {x - 5, x + 5, x}, new int[] {bottom - 1, bottom - 1, getHeight() - 1}, 3);
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int active = frame / 2;
        int totalWidth = DOT_COUNT * DOT_DIAMETER + (DOT_COUNT - 1) * DOT_GAP;
        int startX = (getWidth() - totalWidth) / 2;
        int baseY = (getHeight() - tailHeight - DOT_DIAMETER) / 2;
        for (int i = 0; i < DOT_COUNT; i++) {
            int x = startX + i * (DOT_DIAMETER + DOT_GAP);
            boolean isActive = i == active;
            int y = baseY - (isActive ? ACTIVE_LIFT : 0);
            if (isActive) {
                g.setColor(ACTIVE_FILL);
                g.fillOval(x, y, DOT_DIAMETER, DOT_DIAMETER);
                g.setColor(PURPLE);
                g.setStroke(new BasicStroke(1.7f));
            } else {
                g.setColor(IDLE_RING);
                g.setStroke(new BasicStroke(1.5f));
            }
            g.drawOval(x, y, DOT_DIAMETER, DOT_DIAMETER);
        }
        g.dispose();
    }
}
