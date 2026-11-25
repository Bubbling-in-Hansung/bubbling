package bubbling.client.ui.screen;

import bubbling.client.ui.drawing.BubbleArt;
import bubbling.client.ui.model.MessageText;
import bubbling.client.ui.model.RoomView;
import bubbling.client.ui.style.UiFonts;
import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import static bubbling.client.ui.style.Styles.*;

/** Presentation only; HomeScreen owns room actions and selection. */
final class RoomDetailPanel extends JPanel {
    private static final int ORB_SIZE = 180;
    private static final int ORB_PADDING = 16;
    private static final Color CATEGORY_BG = color("f1edff"), CATEGORY_INK = color("7052cb"),
            PRIVACY_BG = color("edf8f7"), PRIVACY_INK = color("418e7e"), VALUE_INK = color("585165"),
            ROW_SEPARATOR = color("f0edf5");
    private final JLabel category = badge("", CATEGORY_BG, CATEGORY_INK);
    private final JLabel privacy = badge("", PRIVACY_BG, PRIVACY_INK);
    private final JLabel mode = valueLabel();
    private final JLabel remaining = valueLabel();
    private final JLabel people = valueLabel();
    private final JLabel entry = valueLabel();
    private final JButton reaction;
    private final JPanel actions = column();
    private final Orb orb = new Orb();

    RoomDetailPanel(Runnable close, Runnable react) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(28, 30, 28, 30));
        JButton dismiss = closeButton("비눗방울 정보 닫기", close);
        JPanel heading = panel(new BorderLayout(8, 0));
        heading.add(title("상세 정보", 18), BorderLayout.CENTER);
        heading.add(dismiss, BorderLayout.EAST);
        fixedHeight(heading, heading.getPreferredSize().height);
        append(this, heading, 0);
        JPanel orbRow = panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        orbRow.add(orb);
        fixedHeight(orbRow, orb.getPreferredSize().height);
        append(this, orbRow, 0);
        JPanel tags = panel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        tags.add(category);
        tags.add(privacy);
        fixedHeight(tags, Math.max(25, category.getFontMetrics(category.getFont()).getHeight() + 10));
        append(this, tags, 6);
        reaction = reactionButton(react);
        JPanel reactionRow = panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        reactionRow.add(reaction);
        append(this, reactionRow, 14);
        JPanel information = card(Color.WHITE, 28);
        information.setLayout(new GridLayout(4, 1));
        information.add(informationRow("대화 방식", mode, false));
        information.add(informationRow("남은 시간", remaining, true));
        information.add(informationRow("참여 인원", people, true));
        information.add(informationRow("입장 방식", entry, true));
        fixedHeight(information, 160);
        append(this, information, 20);
        append(this, actions, 18);
        append(this, note("채팅방에 입장하면 계정 닉네임 대신"), 4);
        append(this, note("방마다 새로운 랜덤 닉네임이 자동 배정돼요."), 0);
    }

    void showRoom(RoomView view, int participantCount) {
        category.setText(view.category());
        privacy.setText(view.privateRoom() ? "비밀방" : "공개방");
        mode.setText(view.mode());
        people.setText(participantCount + " / " + view.capacity() + "명");
        entry.setText(view.privateRoom() ? "방장 승인 후 참여" : "누구나 바로 참여");
        orb.showRoom(view);
    }

    void updateRemaining(Instant expiresAt, Instant now) {
        remaining.setVisible(expiresAt != null);
        if (expiresAt == null) return;
        String text = "만료됨";
        if (expiresAt.isAfter(now)) {
            long seconds = Duration.between(now, expiresAt).plusNanos(999_999_999).getSeconds();
            long minutes = seconds / 60;
            String clock = minutes % 60 + "분 " + seconds % 60 + "초";
            text = minutes >= 60 ? minutes / 60 + "시간 " + clock : clock;
        }
        if (!text.equals(remaining.getText())) {
            remaining.setText(text);
        }
    }

    void updateReaction(int count, boolean enabled) {
        updateReactionButton(reaction, count, enabled);
    }

    void setActions(JPanel content) {
        actions.removeAll();
        append(actions, content, 0);
        actions.revalidate();
        actions.repaint();
    }

    static void addAction(JPanel target, JButton button, boolean primary, int gap) {
        int height = primary ? 44 : 35;
        button.setFont(UiFonts.body(primary ? 13 : 11, 700));
        button.setPreferredSize(new Dimension(0, height));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        // BorderLayout stretches the button without BoxLayout using its text width.
        JPanel row = panel(new BorderLayout());
        row.add(button);
        fixedHeight(row, height);
        append(target, row, gap);
    }

    private static JLabel valueLabel() {
        JLabel value = label("", 11, VALUE_INK);
        value.setFont(UiFonts.body(11, 600));
        return value;
    }

    private static JPanel informationRow(String name, JLabel value, boolean separator) {
        JPanel row = panel(new BorderLayout(8, 0));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(separator ? 1 : 0, 0, 0, 0, ROW_SEPARATOR),
                BorderFactory.createEmptyBorder(0, 16, 0, 16)));
        row.add(label(name, 11, MUTED), BorderLayout.WEST);
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
        badge.setFont(font(10, false));
        badge.setForeground(foreground);
        badge.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return badge;
    }

    private static JPanel note(String text) {
        JPanel row = panel(new BorderLayout());
        JLabel caption = label(text, 10, MUTED);
        caption.setHorizontalAlignment(SwingConstants.CENTER);
        row.add(caption);
        fixedHeight(row, caption.getPreferredSize().height);
        return row;
    }

    private static void fixedHeight(JComponent component, int height) {
        component.setPreferredSize(new Dimension(0, height));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        component.setMinimumSize(new Dimension(0, height));
    }

    private static final class Orb extends JComponent {
        private final WrappedTitle roomTitle = new WrappedTitle();
        private int tint;

        Orb() {
            setPreferredSize(new Dimension(ORB_SIZE + ORB_PADDING * 2, ORB_SIZE + ORB_PADDING * 2));
            setLayout(new GridBagLayout());
            add(roomTitle);
        }

        void showRoom(RoomView view) {
            tint = view.tint();
            roomTitle.setText(view.title());
            repaint();
        }

        @Override
        protected void paintComponent(Graphics source) {
            Graphics2D g = graphics(source);
            BubbleArt.paint(g, ORB_PADDING, ORB_PADDING, ORB_SIZE, tint, false);
            g.dispose();
        }
    }

    /** Wraps the title within the middle of the orb. */
    private static final class WrappedTitle extends JComponent {
        private static final int WIDTH = 132;
        private static final int MAX_HEIGHT = 120;
        private String text = "";

        WrappedTitle() {
            setFont(UiFonts.body(14, 600));
            setForeground(INK);
        }

        void setText(String value) {
            text = value;
            setToolTipText(value);
            revalidate();
            repaint();
        }

        private List<String> lines() {
            FontMetrics metrics = getFontMetrics(getFont());
            int maxLines = Math.max(1, MAX_HEIGHT / metrics.getHeight());
            return MessageText.wrapWithEllipsis(text, metrics::stringWidth, WIDTH, maxLines);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(WIDTH, lines().size() * getFontMetrics(getFont()).getHeight());
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
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
                y += metrics.getHeight();
            }
            g.dispose();
        }
    }
}
