package bubbling.client.ui.screen;

import bubbling.client.ui.dialog.ActionDialog;
import bubbling.client.ui.dialog.NotificationCards;
import bubbling.client.ui.dialog.RoomEditor;
import bubbling.client.ui.drawing.RoomScene;
import bubbling.client.ui.drawing.MessageBubble;
import bubbling.client.ui.model.*;
import bubbling.client.ui.style.UiFonts;
import bubbling.common.dto.chat.ChatParticipantDto;
import bubbling.common.dto.chat.ChatPositionDto;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import static bubbling.client.ui.style.Styles.*;

public final class ChatScreen extends JPanel {
    private static final int SPEECH_REFRESH_MS = 200;
    private static final int ACTIVITY_REFRESH_MS = 1000;
    private static final int MAX_INPUT_WIDTH = 680;
    private static final Color HEADER_LINE = color("d8d5df"), TIMESTAMP_INK = color("938d9b"),
            INPUT_SHADOW = new Color(45, 36, 65, 28), INPUT_FILL = new Color(255, 255, 255, 224),
            INPUT_BORDER = new Color(255, 255, 255, 235), ORB_LIGHT = color("f4efff"),
            ORB_DARK = color("a98aea"), ORB_RIM = new Color(255, 255, 255, 145),
            NOTICE_INK = color("756d82");
    private final UiSession session;
    private final JLabel heading = title("대화 공간", 16), meta = label("", 11, MUTED);
    private final JPanel history = column(), speech = column();
    private final JTextField input = input("캐릭터에게 따뜻한 말을 건네보세요");
    private final ActivityIndicator activityIndicator = new ActivityIndicator();
    private final JButton send, end, leave, edit, reaction;
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
    private final JLabel historyHeading = title("", 14), historySubtitle = label("새 메시지가 실시간으로 표시돼요", 11, MUTED),
            emptyHistory = label("아직 대화 기록이 없어요.", 12, MUTED);
    private List<List<UiSession.Message>> visibleSpeech;
    private final List<JPanel> speechStacks = new java.util.ArrayList<>();
    private final java.util.Map<UiSession.Message, MessageBubble> speechBubbles = new java.util.IdentityHashMap<>();
    private static final DateTimeFormatter MESSAGE_TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault());
    public ChatScreen(UiData data, Runnable back) {
        super(new BorderLayout());
        session = data.session();
        JPanel header = panel(new BorderLayout(16, 0));
        header.setOpaque(true);
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, HEADER_LINE),
                BorderFactory.createEmptyBorder(12, 18, 4, 18)));
        header.setPreferredSize(new Dimension(0, 70));
        JButton backButton = button("홈으로", false, () -> {
            scene.resetOwnPosition();
            back.run();
        });
        backButton.setIcon(icon("chevron-left"));
        backButton.setFont(font(13, true));
        backButton.putClientProperty("flat", true);
        JPanel backSlot = panel(new GridBagLayout());
        GridBagConstraints backConstraints = new GridBagConstraints();
        backConstraints.anchor = GridBagConstraints.WEST;
        backSlot.add(backButton, backConstraints);
        header.add(backSlot, BorderLayout.WEST);
        JPanel titleBox = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        reaction = reactionButton(() -> {
            if (current != null) session.react(current);
        });
        JPanel titleRow = panel(null);
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        heading.setMinimumSize(new Dimension(0, heading.getPreferredSize().height));
        titleRow.add(heading);
        titleRow.add(Box.createHorizontalStrut(4));
        titleRow.add(reaction);
        append(titleBox, titleRow, 3);
        meta.setMinimumSize(new Dimension(0, meta.getFontMetrics(meta.getFont()).getHeight()));
        append(titleBox, meta, 0);
        titleBox.setAlignmentY(Component.CENTER_ALIGNMENT);
        JPanel roomIdentity = panel(null);
        roomIdentity.setLayout(new BoxLayout(roomIdentity, BoxLayout.X_AXIS));
        roomIdentity.add(Box.createHorizontalGlue());
        roomIdentity.add(new RoomOrb());
        roomIdentity.add(Box.createHorizontalStrut(9));
        roomIdentity.add(titleBox);
        edit = button("비눗방울 수정", false, this::editBubble);
        edit.setIcon(penIcon());
        removeButtonOutline(edit);
        roomIdentity.add(Box.createHorizontalGlue());
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
                    Point scaled = scene.toScaledPosition(event.getPoint());
                    scene.moveOwnCharacter(event.getPoint());
                    session.moveCharacter(current, scaled.x, scaled.y);
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
        actionRow.add(edit);
        actionRow.add(historyToggle);
        end = button("대화 종료", false, () -> {
            UiSession.Room target = current;
            if (target != null && ActionDialog.confirm(this,
                    "대화를 종료할까요?\n\n모든 참여자의 대화가 끝나며 더 이상 메시지를 보낼 수 없어요.", "종료")
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
                g.setColor(INPUT_SHADOW);
                g.fillRoundRect(1, 4, getWidth() - 2, getHeight() - 5, 24, 24);
                g.setColor(INPUT_FILL);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 5, 24, 24);
                g.setColor(INPUT_BORDER);
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
                int width = Math.min(MAX_INPUT_WIDTH, (int) (scene.getWidth() * .78));
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
        timer = new javax.swing.Timer(SPEECH_REFRESH_MS, e -> renderSpeech());
        activityTimer = new javax.swing.Timer(ACTIVITY_REFRESH_MS, e -> {
            updateActivityIndicator();
            edit.setEnabled(canEditBubble());
            refreshReaction();
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

    public void updateCharacterPosition(ChatPositionDto position) {
        if (position == null || current == null || current.conversation == null
                || !current.conversation.chatRoomId().equals(position.chatRoomId()) || current.ended) {
            return;
        }
        scene.moveCharacter(position.chatMemberId(), position.x(), position.y());
        layoutSpeech(scene.getWidth(), scene.getHeight());
        speech.repaint();
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
                false))::stringWidth, MessageBubble.MAX_TEXT_WIDTH).size() > MessageText.MAX_LINES) {
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
        refreshReaction();
        updateActivityIndicator();
        renderHistory();
        renderSpeech();
    }

    private void refreshReaction() {
        reaction.setVisible(current != null);
        if (current != null) {
            updateReactionButton(reaction, current.reactionCount, session.canReact(current));
        }
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
        if (message.at() != null) author.add(label(" · " + MESSAGE_TIME.format(message.at()), 8, TIMESTAMP_INK));
        author.setMaximumSize(new Dimension(Integer.MAX_VALUE, author.getPreferredSize().height));
        return author;
    }

    private void renderSpeech() {
        List<List<UiSession.Message>> messages = List.of();
        if (current != null && current.conversation != null && !current.ended) {
            messages = current.participants().stream().limit(RoomScene.MAX_VISIBLE_PARTICIPANTS)
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
        speechStacks.clear();
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
                speechStacks.add(stack);
                speech.add(stack);
            }
        }
        speech.revalidate();
        layoutSpeech(speech.getWidth(), speech.getHeight());
        speech.repaint();
    }

    private void layoutSpeech(int width, int height) {
        speech.setBounds(0, 0, width, height);
        for (int i = 0; i < speechStacks.size(); i++) {
            JPanel stack = speechStacks.get(i);
            Point head = scene.participantHead(i);
            int below = scene.participantBottom(head) + 8;
            boolean placeBelow = head.y < 95 && height - below > head.y;
            int bubbleCount = (int) java.util.Arrays.stream(stack.getComponents()).filter(MessageBubble.class::isInstance).count();
            int space = placeBelow ? height - below : head.y - 20;
            int budget = Math.max(1, (space - Math.max(0, bubbleCount - 1) * 7) / Math.max(1, bubbleCount));
            for (Component child : stack.getComponents()) {
                if (child instanceof MessageBubble bubble) {
                    bubble.setAvailableHeight(budget);
                }
            }
            Dimension size = stack.getPreferredSize();
            int x = Math.clamp(head.x - size.width / 2, 0, Math.max(0, width - size.width));
            int y = placeBelow ? below : Math.max(0, head.y - size.height - 10);
            stack.setBounds(x, y, size.width, size.height);
            stack.doLayout();
            for (Component child : stack.getComponents()) {
                if (child instanceof MessageBubble bubble) {
                    bubble.doLayout();
                }
            }
        }
        List<Integer> order = scene.paintOrder();
        int z = 0;
        for (int j = order.size() - 1; j >= 0; j--) {
            int idx = order.get(j);
            if (idx >= 0 && idx < speechStacks.size()) {
                speech.setComponentZOrder(speechStacks.get(idx), z++);
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
        activityIndicator.setActive(session.hasRecentActivity(current));
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
            Dimension size = new Dimension(32, 32);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setAlignmentY(Component.CENTER_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics source) {
            Graphics2D g = graphics(source);
            g.setPaint(new GradientPaint(4, 3, ORB_LIGHT, 29, 29, ORB_DARK));
            g.fillOval(1, 1, 30, 30);
            g.setColor(ORB_RIM);
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
            paintStatusDot(source, 13, 4, 5, active);
        }
    }

    private static final class PrivacyNotice extends JPanel {
        PrivacyNotice() {
            super(new FlowLayout(FlowLayout.CENTER, 6, 6));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            JLabel lock = label("🔒", 11, NOTICE_INK);
            lock.setFont(UiFonts.scale(new Font("Segoe UI Emoji", Font.PLAIN, 11)));
            add(lock);
            add(label("계정 정보는 보이지 않아요. 이 방에서는 익명으로 대화해요.", 9, NOTICE_INK));
        }
        @Override
        protected void paintComponent(Graphics source) {
            Graphics2D g = graphics(source);
            g.setColor(INPUT_FILL);
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
