package bubbling.client.ui.screen;

import bubbling.client.ui.drawing.BubbleArt;
import bubbling.client.ui.model.RoomView;
import bubbling.client.ui.style.UiFonts;
import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static bubbling.client.ui.style.Styles.*;

/** Presentation only; HomeScreen owns room actions and selection. */
final class RoomDetailPanel extends JPanel {
    private static final int ORB_SIZE = 76;
    private final JLabel category = badge("", color("f1edff"), color("7052cb"));
    private final JLabel privacy = badge("", color("edf8f7"), color("418e7e"));
    private final JLabel mode = valueLabel();
    private final JLabel remaining = valueLabel();
    private final JLabel people = valueLabel();
    private final JLabel entry = valueLabel();
    private final WrappedTitle roomTitle = new WrappedTitle();
    private final JPanel actions = column();
    private final Orb orb = new Orb();

    RoomDetailPanel(Runnable close) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(24, 22, 24, 22));
        JPanel heading = panel(new BorderLayout());
        JPanel copy = column();
        JLabel kicker = label("BUBBLE DETAIL", 9, color("8a70d7"));
        kicker.setFont(UiFonts.logo(9));
        append(copy, kicker, 5);
        append(copy, label("비눗방울 정보", 18, INK), 0);
        heading.add(copy, BorderLayout.CENTER);
        JButton dismiss = closeButton("비눗방울 정보 닫기", close);
        JPanel closeBox = panel(new BorderLayout());
        closeBox.add(dismiss, BorderLayout.NORTH);
        heading.add(closeBox, BorderLayout.EAST);
        fixedHeight(heading, 48);
        append(this, heading, 16);
        JPanel orbRow = panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        orbRow.add(orb);
        fixedHeight(orbRow, 94);
        append(this, orbRow, 0);
        JPanel tags = panel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        tags.add(category);
        tags.add(privacy);
        fixedHeight(tags, 25);
        append(this, tags, 16);
        append(this, roomTitle, 18);
        JPanel information = card(Color.WHITE, 28);
        information.setLayout(new GridLayout(4, 1));
        information.add(informationRow("대화 방식", mode, false));
        information.add(informationRow("남은 시간", remaining, true));
        information.add(informationRow("참여 인원", people, true));
        information.add(informationRow("입장 방식", entry, true));
        fixedHeight(information, 138);
        append(this, information, 17);
        append(this, actions, 14);
        append(this, note("채팅방에 입장하면 계정 닉네임 대신"), 2);
        append(this, note("방마다 새로운 랜덤 닉네임이 자동 배정돼요."), 0);
    }

    void showRoom(RoomView view, int participantCount) {
        category.setText(view.category());
        privacy.setText(view.privateRoom() ? "비밀방" : "공개방");
        mode.setText(view.mode());
        people.setText(participantCount + " / " + view.capacity() + "명");
        entry.setText(view.privateRoom() ? "방장 승인 후 참여" : "누구나 바로 참여");
        roomTitle.setText(view.title());
        orb.tint = view.tint();
        orb.privateRoom = view.privateRoom();
        orb.repaint();
    }

    void updateRemaining(Instant expiresAt, Instant now) {
        remaining.setVisible(expiresAt != null);
        if (expiresAt == null) return;
        long seconds = Math.max(0, Duration.between(now, expiresAt).getSeconds());
        long minutes = Math.max(1, (seconds + 59) / 60);
        String text = !expiresAt.isAfter(now) ? "만료됨" : minutes >= 60
                ? minutes / 60 + "시간 " + minutes % 60 + "분" : minutes + "분";
        if (!text.equals(remaining.getText())) {
            remaining.setText(text);
        }
    }

    void setActions(JPanel content) {
        actions.removeAll();
        append(actions, content, 0);
        actions.revalidate();
        actions.repaint();
    }

    static void addAction(JPanel target, JButton button, boolean primary, int gap) {
        int height = primary ? 44 : 35;
        button.setFont(UiFonts.body(primary ? 11 : 9, 700));
        button.setPreferredSize(new Dimension(0, height));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        // BorderLayout stretches the button without BoxLayout using its text width.
        JPanel row = panel(new BorderLayout());
        row.add(button);
        fixedHeight(row, height);
        append(target, row, gap);
    }

    private static JLabel valueLabel() {
        JLabel value = label("", 9, color("585165"));
        value.setFont(UiFonts.body(9, 600));
        return value;
    }

    private static JPanel informationRow(String name, JLabel value, boolean separator) {
        JPanel row = panel(new BorderLayout(8, 0));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(separator ? 1 : 0, 0, 0, 0, color("f0edf5")),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)));
        row.add(label(name, 9, color("9c96a7")), BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private static JLabel badge(String text, Color background, Color foreground) {
        JLabel badge = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics source) {
                Graphics2D g = graphics(source);
                g.setColor(background);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g.dispose();
                super.paintComponent(source);
            }
        };
        badge.setFont(font(9, false));
        badge.setForeground(foreground);
        badge.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return badge;
    }

    private static JPanel note(String text) {
        JPanel row = panel(new BorderLayout());
        JLabel caption = label(text, 8, color("aaa5b2"));
        caption.setHorizontalAlignment(SwingConstants.CENTER);
        row.add(caption);
        fixedHeight(row, 13);
        return row;
    }

    private static void fixedHeight(JComponent component, int height) {
        component.setPreferredSize(new Dimension(0, height));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        component.setMinimumSize(new Dimension(0, height));
    }

    private static final class Orb extends JComponent {
        private int tint;
        private boolean privateRoom;

        Orb() {
            setPreferredSize(new Dimension(100, 94));
        }

        @Override
        protected void paintComponent(Graphics source) {
            Graphics2D g = graphics(source);
            BubbleArt.paint(g, 12, 0, ORB_SIZE, tint, false);
            if (privateRoom) {
                g.setColor(color("7659c6"));
                g.setStroke(new BasicStroke(1.6f));
                g.drawRoundRect(42, 36, 16, 13, 3, 3);
                g.drawArc(46, 27, 8, 15, 0, 180);
            } else {
                icon("home").paintIcon(this, g, 39, 27);
            }
            g.dispose();
        }
    }

    /** Native centered multiline title; wrapping follows the available panel width. */
    private static final class WrappedTitle extends JComponent {
        private static final int LINE_HEIGHT = 23;
        private String text = "";

        WrappedTitle() {
            setFont(UiFonts.body(14, 400));
            setForeground(color("585165"));
            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent event) {
                    revalidate();
                }
            });
        }

        void setText(String value) {
            text = value;
            setToolTipText(value);
            revalidate();
            repaint();
        }

        private List<String> lines() {
            int available = Math.max(1, (getWidth() > 0 ? getWidth() : 280) - 10);
            FontMetrics metrics = getFontMetrics(getFont());
            List<String> result = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (int codePoint : text.codePoints().toArray()) {
                String next = new String(Character.toChars(codePoint));
                if (!line.isEmpty() && metrics.stringWidth(line + next) > available) {
                    result.add(line.toString());
                    line.setLength(0);
                }
                line.append(next);
            }
            result.add(line.toString());
            return result;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(0, lines().size() * LINE_HEIGHT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics source) {
            Graphics2D g = graphics(source);
            g.setFont(getFont());
            g.setColor(getForeground());
            FontMetrics metrics = g.getFontMetrics();
            int y = metrics.getAscent();
            for (String line : lines()) {
                g.drawString(line, (getWidth() - metrics.stringWidth(line)) / 2, y);
                y += LINE_HEIGHT;
            }
            g.dispose();
        }
    }
}
