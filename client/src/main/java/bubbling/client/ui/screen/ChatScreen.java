package bubbling.client.ui.screen;

import bubbling.client.ui.dialog.ActionDialog;
import bubbling.client.ui.dialog.NotificationCards;
import bubbling.client.ui.dialog.RoomEditor;
import bubbling.client.ui.drawing.RoomScene;
import bubbling.client.ui.drawing.MessageBubble;
import bubbling.client.ui.model.*;
import bubbling.common.dto.chat.ChatParticipantDto;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import static bubbling.client.ui.style.Styles.*;

public final class ChatScreen extends JPanel {
    private final UiSession session;
    private final JLabel heading = title("대화 공간", 13), meta = label("", 9, color("918a9d"));
    private final JPanel history = column(), speech = column();
    private final JTextField input = input("캐릭터에게 따뜻한 말을 건네보세요");
    private final ActivityIndicator activityIndicator = new ActivityIndicator();
    private final JButton send, end, leave, edit;
    private JButton historyToggle;
    private JScrollPane log;
    private final javax.swing.Timer timer;
    private final javax.swing.Timer activityTimer;
    private RoomScene scene;
    private RoomView room;
    private UiSession.Room current, historyRoom, speechRoom;
    private int historyCount;
    private int historyRevision;
    private boolean historyInitialized;
    private final JLabel historyHeading = title("", 12), historySubtitle = label("새 메시지가 실시간으로 표시돼요", 8, color("918a9d")),
            emptyHistory = label("아직 대화 기록이 없어요.", 10, MUTED);
    private List<List<UiSession.Message>> visibleSpeech;
    private final java.util.Map<UiSession.Message, MessageBubble> speechBubbles = new java.util.IdentityHashMap<>();
    private static final DateTimeFormatter MESSAGE_TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault());
    public ChatScreen(UiData data, Runnable back) {
        super(new BorderLayout());
        session = data.session();
        JPanel header = panel(new BorderLayout(16, 0));
        header.setOpaque(true);
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, color("d8d5df")),
                BorderFactory.createEmptyBorder(12, 18, 4, 18)));
        header.setPreferredSize(new Dimension(0, 70));
        JButton backButton = button("‹ 홈으로", false, back);
        backButton.putClientProperty("flat", true);
        JPanel backSlot = panel(new GridBagLayout());
        GridBagConstraints backConstraints = new GridBagConstraints();
        backConstraints.anchor = GridBagConstraints.WEST;
        backSlot.add(backButton, backConstraints);
        header.add(backSlot, BorderLayout.WEST);
        JPanel titleBox = column();
        append(titleBox, heading, 3);
        append(titleBox, meta, 0);
        titleBox.setMinimumSize(new Dimension(0, titleBox.getPreferredSize().height));
        JPanel roomIdentity = panel(new GridBagLayout());
        GridBagConstraints identityConstraints = new GridBagConstraints();
        identityConstraints.gridy = 0;
        identityConstraints.anchor = GridBagConstraints.CENTER;
        identityConstraints.insets = new Insets(0, 0, 0, 9);
        roomIdentity.add(new RoomOrb(), identityConstraints);
        identityConstraints.insets = new Insets(0, 0, 0, 0);
        roomIdentity.add(titleBox, identityConstraints);
        edit = button("비눗방울 수정", false, this::editBubble);
        edit.setIcon(penIcon());
        removeButtonOutline(edit);
        identityConstraints.insets = new Insets(0, 8, 0, 0);
        roomIdentity.add(edit, identityConstraints);
        header.add(roomIdentity, BorderLayout.CENTER);
        JPanel actions = panel(new GridBagLayout());
        JPanel actionRow = panel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        GridBagConstraints actionConstraints = new GridBagConstraints();
        actionConstraints.weightx = 1;
        actionConstraints.fill = GridBagConstraints.HORIZONTAL;
        actionConstraints.anchor = GridBagConstraints.EAST;
        actions.add(actionRow, actionConstraints);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        JPanel body = panel(new BorderLayout());
        JPanel world = panel(new BorderLayout());
        scene = new RoomScene();
        scene.setCharacter(session::character);
        scene.setLayout(null);
        speech.setLayout(null);
        scene.add(speech);
        PrivacyNotice privacyNotice = new PrivacyNotice();
        scene.add(privacyNotice);
        scene.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && current != null && current.joined && !current.ended) {
                    scene.moveOwnCharacter(event.getPoint());
                    layoutSpeech(scene.getWidth(), scene.getHeight());
                    speech.repaint();
                }
            }
        });
        scene.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                layoutSpeech(scene.getWidth(), scene.getHeight());
                Dimension noticeSize = privacyNotice.getPreferredSize();
                privacyNotice.setBounds((scene.getWidth() - noticeSize.width) / 2, 14, noticeSize.width, noticeSize.height);
            }
        });
        world.add(scene);
        body.add(world);
        history.setOpaque(true);
        history.setBackground(Color.WHITE);
        pad(history, 18);
        log = new JScrollPane(new HistoryBody(history));
        log.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        log.getVerticalScrollBar().setUnitIncrement(20);
        log.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        log.setWheelScrollingEnabled(false);
        log.addMouseWheelListener(this::scrollLogWithWheel);
        log.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, LINE));
        log.getViewport().setBackground(Color.WHITE);
        log.setPreferredSize(new Dimension(320, 0));
        body.add(log, BorderLayout.EAST);
        historyToggle = button("대화 내역 숨기기", false, () -> {
            boolean visible = log.isVisible();
            log.setVisible(!visible);
            historyToggle.setSelected(!visible);
            historyToggle.setText(visible ? "대화 내역 보기" : "대화 내역 숨기기");
            body.revalidate();
            if (!visible) {
                scrollLogToBottom();
            }
        });
        historyToggle.setSelected(true);
        actionRow.add(historyToggle);
        end = button("대화 종료", false, () -> {
            UiSession.Room target = current;
            if (target != null && ActionDialog.confirm(this, "대화를 종료할까요?", "종료")
                    && target == current && target.joined && !target.ended) {
                session.end(target);
            }
        });
        leave = button("퇴장", false, () -> {
            UiSession.Room target = current;
            if (target != null && ActionDialog.confirm(this, "대화방에서 나갈까요?", "퇴장")
                    && target == current && target.joined && !target.ended) {
                session.leave(target);
            }
        });
        actionRow.add(end);
        actionRow.add(leave);
        removeButtonOutline(backButton);
        for (Component component : actionRow.getComponents()) {
            if (component instanceof JButton action) {
                removeButtonOutline(action);
            }
        }
        int actionHeight = 0;
        for (Component component : actionRow.getComponents()) {
            if (component instanceof JButton action) {
                actionHeight = Math.max(actionHeight, action.getPreferredSize().height);
            }
        }
        Dimension backSize = backButton.getPreferredSize();
        backButton.setPreferredSize(new Dimension(backSize.width + 20, Math.max(backSize.height, actionHeight)));
        add(body);
        JPanel inputArea = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics source) {
                super.paintComponent(source);
                Graphics2D g = (Graphics2D) source.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(45, 36, 65, 28));
                g.fillRoundRect(1, 4, getWidth() - 2, getHeight() - 5, 24, 24);
                g.setColor(new Color(255, 255, 255, 224));
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 5, 24, 24);
                g.setColor(new Color(255, 255, 255, 235));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 5, 24, 24);
                g.dispose();
            }
        };
        inputArea.setOpaque(false);
        inputArea.setBorder(BorderFactory.createEmptyBorder(10, 14, 15, 14));
        JPanel controls = panel(new BorderLayout(10, 0));
        controls.setPreferredSize(new Dimension(0, 42));
        controls.add(input, BorderLayout.CENTER);
        send = button("전송", true, this::send);
        controls.add(send, BorderLayout.EAST);
        input.addActionListener(e -> send());
        inputArea.add(controls, BorderLayout.CENTER);
        scene.add(inputArea);
        scene.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                int width = Math.min(680, (int) (scene.getWidth() * .78));
                Dimension size = inputArea.getPreferredSize();
                inputArea.setBounds((scene.getWidth() - width) / 2, scene.getHeight() - size.height - 15, width, size.height);
            }
        });
        input.setOpaque(false);
        input.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        input.setToolTipText("공백 포함 최대 " + MessageText.MAX_LENGTH + "자 · " + MessageText.COLUMNS + "자씩 자동 줄바꿈");
        ((AbstractDocument) input.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass bypass, int offset, String value, AttributeSet attributes) throws BadLocationException {
                replace(bypass, offset, 0, value, attributes);
            }
            @Override
            public void replace(FilterBypass bypass, int offset, int length, String value, AttributeSet attributes) throws BadLocationException {
                String insertion = value == null ? "" : value;
                String original = bypass.getDocument().getText(0, bypass.getDocument().getLength());
                String candidate = original.substring(0, offset) + insertion + original.substring(offset + length);
                // Let the IME finish composing; enforce the limit on committed text and send.
                if (attributes != null && attributes.isDefined(StyleConstants.ComposedTextAttribute) || MessageText.acceptsInput(candidate)) {
                    bypass.replace(offset, length, insertion, attributes);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
        timer = new javax.swing.Timer(200, e -> renderSpeech());
        activityTimer = new javax.swing.Timer(1000, e -> {
            updateActivityIndicator();
            edit.setEnabled(canEditBubble());
        });
        addHierarchyListener(e -> {
            if (isShowing()) {
                timer.start();
                activityTimer.start();
            } else {
                timer.stop();
                activityTimer.stop();
            }
        });
        refresh();
    }

    public void setRoomTitle(String title) {
        heading.setText(title);
    }

    public void showRoom(RoomView selected) {
        UiSession.Room next = session.find(selected);
        if (next == null) {
            throw new IllegalArgumentException("Unknown room");
        }
        if (current != next) scene.resetOwnPosition();
        room = selected;
        current = next;
        input.setText("");
        refreshLive();
    }

    public void refreshLive() {
        if (current == null || current.conversation == null) return;
        var conversation = current.conversation;
        updateRoomHeading();
        scene.showParticipants(current.ended ? List.of() : current.participants(), conversation.myMemberId());
        refresh();
    }

    private void updateRoomHeading() {
        if (current == null) return;
        room = current.view;
        heading.setText(room.title());
        heading.setToolTipText(room.title());
        String count = current.people + " / " + room.capacity() + "명";
        meta.setText(room.mode() == null ? count : room.mode() + " · " + count + " · "
                + (room.privateRoom() ? "비공개" : "공개"));
    }

    private void editBubble() {
        if (!canEditBubble()) return;
        new RoomEditor(SwingUtilities.getWindowAncestor(this), current.view, session, () -> {
            updateRoomHeading();
            refresh();
        }).setVisible(true);
    }

    private boolean canEditBubble() {
        return current != null && current.view.owner() && !current.ended && !current.removed
                && (current.expiresAt == null || current.expiresAt.isAfter(session.now()));
    }

    public boolean isShowingRoom(String bubbleId) {
        return current != null && current.bubbleId.equals(bubbleId);
    }

    public void resetLive() {
        current = null;
        room = null;
        input.setText("");
        scene.resetOwnPosition();
        scene.showParticipants(List.of(), null);
        refresh();
    }

    private void send() {
        if (current == null || current.ended || !current.joined || input.getText().isBlank()) {
            return;
        }
        if (!MessageText.acceptsInput(input.getText()) || MessageText.lines(input.getText(), input.getFontMetrics(font(11,
                false))::stringWidth, MessageBubble.MAX_WIDTH - 24).size() > MessageText.MAX_LINES) {
            NotificationCards.info(this, "메시지 입력 안내",
                    "메시지는 공백 포함 " + MessageText.MAX_LENGTH + "자, 최대 " + MessageText.MAX_LINES
                    + "줄까지 입력할 수 있어요.");
            return;
        }
        session.send(current, input.getText());
        input.setText("");
        refresh();
    }

    private void refresh() {
        boolean enabled = current != null && !current.ended && current.joined;
        boolean owner = room != null && room.owner();
        send.setEnabled(enabled);
        input.setEnabled(enabled);
        leave.setVisible(!owner);
        leave.setEnabled(enabled && !owner);
        end.setVisible(owner);
        end.setEnabled(enabled && owner);
        edit.setVisible(owner);
        edit.setEnabled(canEditBubble());
        updateActivityIndicator();
        renderHistory();
        renderSpeech();
    }

    private void renderHistory() {
        int count = current == null ? 0 : current.messages.size();
        int revision = current == null ? 0 : current.historyRevision;
        boolean rebuild = !historyInitialized || historyRoom != current || historyRevision != revision || count < historyCount;
        boolean shouldScroll = count > historyCount || rebuild;
        if (rebuild) {
            history.removeAll();
            JPanel headingBox = column();
            headingBox.add(historyHeading);
            headingBox.add(Box.createVerticalStrut(3));
            headingBox.add(historySubtitle);
            JPanel headingRow = panel(new BorderLayout());
            headingRow.add(headingBox, BorderLayout.WEST);
            headingRow.add(activityIndicator, BorderLayout.EAST);
            headingRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, headingRow.getPreferredSize().height));
            append(history, headingRow, 16);
            // Only the space above the messages grows while the log is short.
            history.add(Box.createVerticalGlue());
            historyRoom = current;
            historyRevision = revision;
            historyCount = 0;
            historyInitialized = true;
            if (count == 0) {
                history.add(emptyHistory);
            }
        }
        historyHeading.setText(current != null && current.ended ? "종료된 대화 기록" : "실시간 채팅 로그");
        if (count > 0 && emptyHistory.getParent() == history) {
            history.remove(emptyHistory);
        }
        while (historyCount < count) {
            UiSession.Message message = current.messages.get(historyCount++);
            JPanel row = panel(new BorderLayout()), entry = column();
            JPanel author = historyAuthor(message);
            MessageBubble text = new MessageBubble(message.text(), message.mine(), true, () -> {
                entry.invalidate();
                row.invalidate();
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
                history.revalidate();
                history.repaint();
            });
            text.setAlignmentX(message.mine() ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
            append(entry, author, 4);
            append(entry, text, 0);
            entry.setMaximumSize(new Dimension(250, Integer.MAX_VALUE));
            row.add(entry, message.mine() ? BorderLayout.EAST : BorderLayout.WEST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            append(history, row, 10);
        }
        history.revalidate();
        history.repaint();
        if (shouldScroll) {
            scrollLogToBottom();
        }
    }

    private JPanel historyAuthor(UiSession.Message message) {
        FlowLayout layout = new FlowLayout(message.mine() ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0);
        layout.setAlignOnBaseline(true);
        JPanel author = panel(layout);
        author.add(label(message.sender(), 11, Color.BLACK));
        if (message.at() != null) author.add(label(" · " + MESSAGE_TIME.format(message.at()), 8, color("938d9b")));
        author.setMaximumSize(new Dimension(Integer.MAX_VALUE, author.getPreferredSize().height));
        return author;
    }

    private void renderSpeech() {
        List<List<UiSession.Message>> messages = List.of();
        if (current != null && current.conversation != null && !current.ended) {
            messages = current.participants().stream().limit(4)
                    .map(ChatParticipantDto::chatMemberId).map(id -> session.speech(current, id)).toList();
        }
        if (speechRoom == current && messages.equals(visibleSpeech)) {
            return;
        }
        speechRoom = current;
        visibleSpeech = messages;
        var visibleMessages = messages.stream().flatMap(List::stream).toList();
        speechBubbles.keySet().removeIf(message -> visibleMessages.stream().noneMatch(value -> value == message));
        speech.removeAll();
        if (current != null) {
            for (var senderMessages : messages) {
                JPanel stack = column();
                boolean first = true;
                for (var message : senderMessages) {
                    if (!first) {
                        stack.add(Box.createVerticalStrut(7));
                    }
                    MessageBubble text = speechBubbles.computeIfAbsent(message, value -> new MessageBubble(value.text(), value.mine(),
                            false, () -> {
                        layoutSpeech(scene.getWidth(), scene.getHeight());
                        speech.revalidate();
                        speech.repaint();
                    }));
                    stack.add(text);
                    text.setAlignmentX(Component.CENTER_ALIGNMENT);
                    first = false;
                }
                speech.add(stack);
            }
        }
        speech.revalidate();
        layoutSpeech(speech.getWidth(), speech.getHeight());
        speech.repaint();
    }

    private void layoutSpeech(int width, int height) {
        speech.setBounds(0, 0, width, height);
        for (int i = 0; i < speech.getComponentCount(); i++) {
            Component stack = speech.getComponent(i);
            Point head = scene.participantHead(i);
            int below = scene.participantBottom(head) + 8;
            boolean placeBelow = head.y < 95 && height - below > head.y;
            if (stack instanceof Container container) {
                int bubbleCount = (int) java.util.Arrays.stream(container.getComponents()).filter(MessageBubble.class::isInstance).count();
                int space = placeBelow ? height - below : head.y - 20;
                int budget = Math.max(1, (space - Math.max(0, bubbleCount - 1) * 7) / Math.max(1, bubbleCount));
                for (Component child : container.getComponents()) {
                    if (child instanceof MessageBubble bubble) {
                        bubble.setAvailableHeight(budget);
                    }
                }
            }
            Dimension size = stack.getPreferredSize();
            int x = Math.clamp(head.x - size.width / 2, 0, Math.max(0, width - size.width));
            int y = placeBelow ? below : Math.max(0, head.y - size.height - 10);
            stack.setBounds(x, y, size.width, size.height);
            if (stack instanceof Container container) {
                container.doLayout();
                for (Component child : container.getComponents()) {
                    if (child instanceof MessageBubble bubble) {
                        bubble.doLayout();
                    }
                }
            }
        }
    }

    private void scrollLogToBottom() {
        SwingUtilities.invokeLater(() -> {
            JViewport viewport = log.getViewport();
            int bottom = Math.max(0, viewport.getViewSize().height - viewport.getExtentSize().height);
            viewport.setViewPosition(new Point(0, bottom));
        });
    }

    private void scrollLogWithWheel(MouseWheelEvent event) {
        JViewport viewport = log.getViewport();
        int delta = event.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL ? (int) Math.round(event.getPreciseWheelRotation() * viewport.getExtentSize().height)
                : (int) Math.round(event.getPreciseWheelRotation() * Math.max(1, event.getScrollAmount()) * 20);
        int bottom = Math.max(0, viewport.getViewSize().height - viewport.getExtentSize().height);
        Point position = viewport.getViewPosition();
        position.y = Math.max(0, Math.min(bottom, position.y + delta));
        viewport.setViewPosition(position);
        event.consume();
    }

    private void updateActivityIndicator() {
        Instant now = session.now(), last = null;
        if (current != null) {
            for (UiSession.Message message : current.messages) {
                if (message.at() != null && (last == null || message.at().isAfter(last))) {
                    last = message.at();
                }
            }
        }
        activityIndicator.setActive(last != null && !last.isAfter(now) && !last.isBefore(now.minusSeconds(60)));
    }

    /** Fill a short viewport so the history's vertical glue can bottom-align messages. */
    private static final class HistoryBody extends JPanel implements Scrollable {
        HistoryBody(JPanel history) {
            super(new BorderLayout());
            setOpaque(false);
            add(history, BorderLayout.CENTER);
        }
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 20;
        }
        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(20, visible.height - 20);
        }
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && getPreferredSize().height <= viewport.getExtentSize().height;
        }
    }

    private static final class RoomOrb extends JComponent {
        RoomOrb() {
            setPreferredSize(new Dimension(32, 32));
            setMinimumSize(getPreferredSize());
            setMaximumSize(getPreferredSize());
        }
        @Override
        protected void paintComponent(Graphics source) {
            Graphics2D g = (Graphics2D) source.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(4, 3, color("f4efff"), 29, 29, color("a98aea")));
            g.fillOval(1, 1, 30, 30);
            g.setColor(new Color(255, 255, 255, 145));
            g.drawOval(1, 1, 29, 29);
            g.dispose();
        }
    }

    private static final class ActivityIndicator extends JComponent {
        private boolean active;
        ActivityIndicator() {
            setPreferredSize(new Dimension(14, 14));
            setToolTipText("최근 1분 이내 채팅 활동이 없어요.");
        }
        void setActive(boolean active) {
            if (this.active != active) {
                this.active = active;
                setToolTipText(active ? "최근 1분 이내 채팅 활동이 있어요." : "최근 1분 이내 채팅 활동이 없어요.");
                repaint();
            }
        }
        @Override
        protected void paintComponent(Graphics source) {
            Graphics2D g = (Graphics2D) source.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(active ? color("e7f9f3") : color("f1eef5"));
            g.fillOval(0, 0, 13, 13);
            g.setColor(active ? color("52d4aa") : color("aaa5b5"));
            g.fillOval(4, 4, 5, 5);
            g.dispose();
        }
    }

    private static final class PrivacyNotice extends JPanel {
        PrivacyNotice() {
            super(new FlowLayout(FlowLayout.CENTER, 6, 6));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            JLabel lock = label("🔒", 11, color("756d82"));
            lock.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
            add(lock);
            add(label("계정 정보는 보이지 않아요. 이 방에서는 익명으로 대화해요.", 9, color("756d82")));
        }
        @Override
        protected void paintComponent(Graphics source) {
            Graphics2D g = (Graphics2D) source.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(255, 255, 255, 224));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            g.dispose();
            super.paintComponent(source);
        }
    }

    @Override
    public void removeNotify() {
        timer.stop();
        activityTimer.stop();
        super.removeNotify();
    }
}
