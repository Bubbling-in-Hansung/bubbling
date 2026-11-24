package bubbling.client.ui.screen;

import bubbling.client.ui.model.*;
import bubbling.client.ui.dialog.UiErrors;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;
import static bubbling.client.ui.style.Styles.*;

public final class ConversationListScreen extends JPanel {
    private static final Icon NOTIFICATION_BADGE = new Icon() {
        public int getIconWidth() { return 16; }
        public int getIconHeight() { return 16; }

        public void paintIcon(Component source, Graphics sourceGraphics, int x, int y) {
            Graphics2D g = graphics(sourceGraphics);
            g.setColor(color("c93d60"));
            g.fillOval(x, y, getIconWidth(), getIconHeight());
            g.setFont(font(11, true));
            FontMetrics metrics = g.getFontMetrics();
            g.setColor(Color.WHITE);
            g.drawString("!", x + (getIconWidth() - metrics.stringWidth("!")) / 2,
                    y + (getIconHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    };
    private final UiSession session;
    private final Consumer<RoomView> enter;
    private final Function<UiSession.Room, String> notificationDescription;
    private final JPanel rows = column();
    private Filter filter = Filter.ALL;
    private enum Filter {
        ALL("전체"), ACTIVE("진행 중"), ENDED("종료됨");
        private final String label;
        Filter(String label) {
            this.label = label;
        }
        boolean includes(UiSession.Room room) {
            return this == ALL || this == ENDED && room.ended || this == ACTIVE && !room.ended;
        }
    }

    public ConversationListScreen(UiData data, Consumer<RoomView> enter,
            Function<UiSession.Room, String> notificationDescription) {
        super(new BorderLayout());
        session = data.session();
        this.enter = enter;
        this.notificationDescription = notificationDescription;
        setBackground(BG);
        JPanel content = column();
        pad(content, 32);
        JPanel heading = panel(new BorderLayout());
        heading.add(title("나의 대화", 22));
        JPanel tabs = panel(new FlowLayout(FlowLayout.RIGHT));
        for (Filter value : Filter.values()) {
            tabs.add(button(value.label, false, () -> {
                filter = value;
                refresh();
            }));
        }
        heading.add(tabs, BorderLayout.EAST);
        append(content, heading, 22);
        append(content, rows, 0);
        add(scroll(content));
        refresh();
    }

    public void refresh() {
        rows.removeAll();
        for (var r : session.rooms()) {
            if (!r.listed && !r.visited) {
                continue;
            }
            if (!filter.includes(r)) {
                continue;
            }
            append(rows, createRow(r), 10);
        }
        if (rows.getComponentCount() == 0) {
            append(rows, label("표시할 대화가 없어요.", 12, MUTED), 20);
        }
        rows.revalidate();
        rows.repaint();
    }

    private JPanel createRow(UiSession.Room r) {
        JPanel row = card(Color.WHITE, 28);
        row.setLayout(new BorderLayout(16, 0));
        pad(row, 18);
        JPanel copy = column();
        append(copy, label((r.ended ? "종료됨" : "진행 중") + (r.view.category() == null ? "" : " · " + r.view.category()), 11, PURPLE), 6);
        JLabel roomTitle = title(r.view.title(), 15);
        roomTitle.setToolTipText(r.view.title());
        String notification = notificationDescription.apply(r);
        JButton reaction = reactionButton(() -> session.react(r));
        updateReactionButton(reaction, r.reactionCount, session.canReact(r));
        JPanel titleRow = panel(null);
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        roomTitle.setMinimumSize(new Dimension(0, roomTitle.getPreferredSize().height));
        titleRow.add(roomTitle);
        if (notification != null) {
            JLabel badge = new JLabel(NOTIFICATION_BADGE);
            badge.setToolTipText(notification);
            badge.getAccessibleContext().setAccessibleName(notification);
            roomTitle.getAccessibleContext().setAccessibleDescription(notification);
            titleRow.add(Box.createHorizontalStrut(7));
            titleRow.add(badge);
        }
        titleRow.add(Box.createHorizontalStrut(4));
        titleRow.add(reaction);
        titleRow.add(Box.createHorizontalGlue());
        append(copy, titleRow, 6);
        append(copy, label((r.view.mode() == null ? "" : r.view.mode() + " · ") + r.people + " / " + r.view.capacity() + "명", 11, MUTED), 0);
        row.add(copy);
        JButton action = button(r.ended ? "기록 보기" : "대화로 이동", false,
                () -> UiErrors.perform(this, () -> enter.accept(r.view)));
        action.setEnabled(session.canOpen(r));
        row.add(action, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        return row;
    }
}
