package bubbling.client.ui.drawing;

import javax.swing.*;
import java.awt.*;
import static bubbling.client.ui.style.Styles.*;

/** Overlays never consume a strip of the bubble field. All dimensions are logical pixels. */
public final class BubbleStage extends JPanel {
    private static final int WRITE_BASE_WIDTH = 176;
    private static final int WRITE_BASE_HEIGHT = 56;
    private static final int WRITE_HOVER_EXTRA_W = 10;
    private static final int WRITE_HOVER_EXTRA_H = 6;
    private static final Color SUBTITLE_INK = color("777388"), STAGE_MID = color("fbfaff"),
            STAGE_EDGE = color("f7f5fc"), GLOW_PURPLE = color("eee8ff"), GLOW_CYAN = color("e5f8fa");
    private final JPanel field;
    private final JPanel heading = column();
    private final JButton requests, create;
    private final JLabel legend = label("•  비눗방울 위에 마우스를 올리면 잠시 멈춰요", 12, MUTED);
    private boolean createHovered;
    private int writeBaseWidth = WRITE_BASE_WIDTH;
    private int writeBaseHeight = WRITE_BASE_HEIGHT;
    public BubbleStage(JPanel field, Runnable manage, Runnable write) {
        super(null);
        this.field = field;
        field.setOpaque(false);
        JLabel headline = label("오늘도 수많은 마음이 떠다니고 있어요", 16, Color.BLACK);
        headline.setFont(bubbling.client.ui.style.UiFonts.body(16, 600));
        append(heading, headline, 4);
        append(heading, label("마음이 가는 비눗방울을 눌러 이야기를 나눠보세요", 12, SUBTITLE_INK), 0);
        requests = button("참여 요청 관리", false, manage);
        requests.putClientProperty("flat", true);
        requests.putClientProperty("soft", true);
        requests.setBackground(SOFT);
        requests.setFont(font(12, false));
        requests.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        create = button("고민 띄우기", true, write);
        create.putClientProperty("floating", true);
        create.setFont(font(14, true));
        create.setIcon(penIcon());
        create.setIconTextGap(9);
        create.setBorder(BorderFactory.createEmptyBorder(14, 26, 14, 26));
        create.getModel().addChangeListener(e -> {
            boolean rollover = create.getModel().isRollover();
            if (createHovered != rollover) {
                createHovered = rollover;
                create.setFont(font(createHovered ? 15 : 14, true));
                revalidate();
                doLayout();
                repaint();
            }
        });
        add(heading);
        add(requests);
        add(create);
        add(legend);
        add(field);
    }

    @Override
    public void doLayout() {
        field.setBounds(0, 0, getWidth(), getHeight());
        heading.setBounds(27, 22, heading.getPreferredSize().width, heading.getPreferredSize().height);
        Dimension requestsSize = requests.getPreferredSize();
        int requestsWidth = Math.max(128, requestsSize.width);
        int requestsHeight = Math.max(40, requestsSize.height);
        requests.setBounds(Math.max(0, getWidth() - 28 - requestsWidth), 20, requestsWidth, requestsHeight);
        if (!createHovered) {
            Dimension writeSize = create.getPreferredSize();
            writeBaseWidth = Math.max(WRITE_BASE_WIDTH, writeSize.width);
            writeBaseHeight = Math.max(WRITE_BASE_HEIGHT, writeSize.height);
        }
        int extraW = createHovered ? WRITE_HOVER_EXTRA_W : 0;
        int extraH = createHovered ? WRITE_HOVER_EXTRA_H : 0;
        int x0 = Math.max(0, getWidth() - 28 - writeBaseWidth);
        int y0 = Math.max(0, getHeight() - 24 - writeBaseHeight);
        create.setBounds(Math.max(0, x0 - extraW / 2), Math.max(0, y0 - extraH / 2),
                writeBaseWidth + extraW, writeBaseHeight + extraH);
        Dimension legendSize = legend.getPreferredSize();
        legend.setBounds(28, Math.max(0, getHeight() - 40), legendSize.width, legendSize.height);
    }

    @Override
    protected void paintComponent(Graphics source) {
        Graphics2D g = graphics(source);
        int w = getWidth(), h = getHeight();
        if (w > 0 && h > 0) {
            g.setPaint(new RadialGradientPaint(w * .48f, h * .38f, Math.max(w, h) * .72f, new float[] {
                0, .5f, 1
            }, new Color[] {
                Color.WHITE, STAGE_MID, STAGE_EDGE
            }));
            g.fillRect(0, 0, w, h);
            glow(g, w * .24f, h * .18f, 420, GLOW_PURPLE);
            glow(g, w - 350, h - 260, 350, GLOW_CYAN);
        }
        g.dispose();
    }

    private void glow(Graphics2D g, float x, float y, int size, Color c) {
        g.setPaint(new RadialGradientPaint(x + size / 2f, y + size / 2f, size / 2f, new float[] {
            0, .69f, 1
        }, new Color[] {
            new Color(c.getRed(), c.getGreen(), c.getBlue(), 89), new Color(c.getRed(), c.getGreen(), c.getBlue(), 0),
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)
        }));
        g.fillOval((int) x, (int) y, size, size);
    }
}
