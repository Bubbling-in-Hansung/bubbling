package bubbling.client.ui.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import static bubbling.client.ui.style.Styles.*;

/** Displays notices in arrival order above the current window's content. */
public final class NotificationCards {
    private static final String QUEUE_PROPERTY = NotificationCards.class.getName();
    private static final int EDGE = 16;
    private static final int MAIN_FRAME_TOP = 18;
    private static final int MAIN_FRAME_EDGE = 28;
    private static final int MAXIMIZED_TOP = 86;
    private static final int GAP = 8;
    private final JLayeredPane layer;
    private final Map<JPanel, Card> queue = new LinkedHashMap<>();

    private record Notice(String title, String message, boolean error) {}
    private record Card(JTextArea detail, JScrollPane scroll, Timer timer) {}

    private NotificationCards(JLayeredPane layer) {
        this.layer = layer;
        layer.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                placeCards();
            }
        });
        layer.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !layer.isShowing()) clear();
        });
    }

    public static void info(Component owner, String title, String message) {
        enqueue(owner, new Notice(title, message, false));
    }

    static void error(Component owner, String message) {
        enqueue(owner, new Notice("요청을 처리하지 못했어요", message, true));
    }

    private static void enqueue(Component owner, Notice notice) {
        JRootPane root = SwingUtilities.getRootPane(owner);
        Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        Window ownerWindow = owner instanceof Window window ? window : SwingUtilities.getWindowAncestor(owner);
        // Server responses can arrive after a dialog is disposed but before focus returns.
        if (active instanceof JDialog dialog && dialog.isModal() && dialog.isShowing()) {
            for (Window parent = dialog; parent != null; parent = parent.getOwner()) {
                if (parent == ownerWindow) {
                    root = dialog.getRootPane();
                    break;
                }
            }
        }
        if (root == null) return;
        var cards = (NotificationCards) root.getClientProperty(QUEUE_PROPERTY);
        if (cards == null) {
            cards = new NotificationCards(root.getLayeredPane());
            root.putClientProperty(QUEUE_PROPERTY, cards);
        }
        cards.addCard(notice);
    }

    public static void clear(Component owner) {
        JRootPane root = SwingUtilities.getRootPane(owner);
        if (root != null && root.getClientProperty(QUEUE_PROPERTY) instanceof NotificationCards cards) cards.clear();
    }

    private void addCard(Notice notice) {
        Color tint = color(notice.error() ? "fff0f4" : "f2efff");
        Color accent = color(notice.error() ? "a44660" : "6f4cd2");
        JPanel current = card(Color.WHITE, 28, false);
        current.setLayout(new BorderLayout(10, 0));
        current.setBorder(BorderFactory.createEmptyBorder(13, 13, 13, 11));
        JPanel badge = card(tint, 14);
        badge.setLayout(new GridBagLayout());
        badge.setPreferredSize(new Dimension(32, 32));
        JLabel mark = label(notice.error() ? "!" : "i", 16, accent);
        badge.add(mark);
        JPanel leading = panel(new BorderLayout());
        leading.add(badge, BorderLayout.NORTH);
        current.add(leading, BorderLayout.WEST);

        JPanel copy = panel(new BorderLayout(0, 4));
        copy.add(title(notice.title(), 12), BorderLayout.NORTH);
        String message = notice.message();
        JTextArea detail = new JTextArea(message == null || message.isBlank() ? "다시 시도해 주세요." : message);
        detail.setFont(font(11, false));
        detail.setForeground(color("625b70"));
        detail.setOpaque(false);
        detail.setBorder(null);
        detail.setEditable(false);
        detail.setCaretColor(new Color(0, 0, 0, 0));
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        JScrollPane scroll = ModalStyle.scrollContent(detail);
        copy.add(scroll, BorderLayout.CENTER);
        current.add(copy, BorderLayout.CENTER);

        JButton close = closeButton("알림 닫기", () -> dismiss(current));
        close.setMargin(new Insets(0, 0, 0, 0));
        close.setPreferredSize(new Dimension(34, 34));
        close.getAccessibleContext().setAccessibleName("알림 닫기");
        JPanel trailing = panel(new BorderLayout());
        trailing.add(close, BorderLayout.NORTH);
        current.add(trailing, BorderLayout.EAST);
        current.getAccessibleContext().setAccessibleName(notice.title());
        Timer timer = new Timer(5500, event -> dismiss(current));
        timer.setRepeats(false);
        queue.put(current, new Card(detail, scroll, timer));
        current.setVisible(false);
        layer.add(current, JLayeredPane.POPUP_LAYER);
        placeCards();
    }

    private void placeCards() {
        Window window = SwingUtilities.getWindowAncestor(layer);
        Rectangle bounds = new Rectangle(0, 0, layer.getWidth(), layer.getHeight());
        int topInset = EDGE;
        int edge = EDGE;
        if (window instanceof JFrame frame) {
            Container content = frame.getContentPane();
            bounds = SwingUtilities.convertRectangle(content,
                    new Rectangle(0, 0, content.getWidth(), content.getHeight()), layer);
            boolean maximized = (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH
                    || frame.getGraphicsConfiguration().getDevice().getFullScreenWindow() == frame;
            topInset = maximized ? MAXIMIZED_TOP : MAIN_FRAME_TOP;
            edge = MAIN_FRAME_EDGE;
        }
        int top = bounds.y + topInset;
        int width = Math.min(350, Math.max(1, bounds.width - edge * 2));
        int x = bounds.x + Math.max(0, bounds.width - width - edge);
        int bottom = bounds.y + bounds.height - edge;
        int y = top;
        boolean spaceAvailable = true;
        for (var entry : queue.entrySet()) {
            JPanel panel = entry.getKey();
            Card card = entry.getValue();
            card.detail().setSize(Math.max(1, width - 112), Short.MAX_VALUE);
            int textHeight = Math.min(120, Math.max(18, card.detail().getPreferredSize().height));
            card.scroll().setPreferredSize(new Dimension(0, textHeight));
            int height = panel.getPreferredSize().height;
            spaceAvailable = spaceAvailable && (y == top || y + height <= bottom);
            panel.setVisible(spaceAvailable);
            if (spaceAvailable) {
                panel.setBounds(x, y, width, height);
                panel.revalidate();
                y += height + GAP;
                card.timer().start();
            } else {
                card.timer().stop();
            }
        }
        layer.repaint();
    }

    private void dismiss(JPanel panel) {
        Card card = queue.remove(panel);
        if (card == null) return;
        card.timer().stop();
        layer.remove(panel);
        placeCards();
    }

    private void clear() {
        queue.forEach((panel, card) -> {
            card.timer().stop();
            layer.remove(panel);
        });
        queue.clear();
        layer.repaint();
    }
}
