package bubbling.client.ui.drawing;

import javax.swing.*;
import java.awt.*;
import static bubbling.client.ui.style.Styles.*;

/** Overlays never consume a strip of the bubble field. All dimensions are logical pixels. */
public final class BubbleStage extends JPanel {
    private final JPanel field;
    private final JPanel heading = column();
    private final JButton requests, create;
    private final JLabel legend = label("•  비눗방울 위에 마우스를 올리면 잠시 멈춰요", 10, color("aaa6b4"));
    public BubbleStage(JPanel field, Runnable manage, Runnable write, boolean editable) {
        super(null);
        this.field = field;
        field.setOpaque(false);
        JLabel headline = label("오늘도 수많은 마음이 떠다니고 있어요", 14, Color.BLACK);
        headline.setFont(bubbling.client.ui.style.UiFonts.body(14, 600));
        append(heading, headline, 4);
        append(heading, label("마음이 가는 비눗방울을 눌러 이야기를 나눠보세요", 10, color("777388")), 0);
        requests = button("참여 요청 관리", false, manage);
        requests.putClientProperty("flat", true);
        requests.setFont(font(10, false));
        requests.setMargin(new Insets(6, 10, 6, 10));
        create = button("고민 띄우기", true, write);
        create.putClientProperty("floating", true);
        create.setFont(font(12, true));
        create.setIcon(penIcon());
        create.setIconTextGap(9);
        create.setMargin(new Insets(0, 20, 0, 20));
        create.setEnabled(editable);
        add(heading);
        add(requests);
        add(create);
        add(legend);
        add(field);
    }

    @Override
    public void doLayout() {
        field.setBounds(0, 0, getWidth(), getHeight());
        heading.setBounds(27, 22, 330, 48);
        requests.setBounds(Math.max(365, getWidth() - 142), 20, 114, 32);
        int writeWidth = Math.max(160, create.getPreferredSize().width + 12);
        create.setBounds(Math.max(0, getWidth() - 28 - writeWidth), Math.max(0, getHeight() - 72), writeWidth, 48);
        legend.setBounds(28, Math.max(0, getHeight() - 40), 380, 20);
    }

    @Override
    protected void paintComponent(Graphics source) {
        Graphics2D g = graphics(source);
        int w = getWidth(), h = getHeight();
        if (w > 0 && h > 0) {
            g.setPaint(new RadialGradientPaint(w * .48f, h * .38f, Math.max(w, h) * .72f, new float[] {
                0, .5f, 1
            }, new Color[] {
                Color.WHITE, color("fbfaff"), color("f7f5fc")
            }));
            g.fillRect(0, 0, w, h);
            glow(g, w * .24f, h * .18f, 420, color("eee8ff"));
            glow(g, w - 350, h - 260, 350, color("e5f8fa"));
        }
        if (create.isEnabled()) {
            Rectangle b = create.getBounds();
            for (int k = 24; k > 0; k -= 2) {
                g.setColor(new Color(121, 82, 222, 3));
                g.fillRoundRect(b.x - k / 2, b.y + 8 - k / 2, b.width + k, b.height + k, 48 + k, 48 + k);
            }
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
