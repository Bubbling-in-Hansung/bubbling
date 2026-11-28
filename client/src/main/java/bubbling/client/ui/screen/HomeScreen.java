package bubbling.client.ui.screen;

import bubbling.client.ui.model.*;
import bubbling.client.ui.dialog.*;
import bubbling.client.ui.drawing.*;
import bubbling.common.enums.ChatRequestStatus;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import static bubbling.client.ui.style.Styles.*;

public final class HomeScreen extends JPanel {
    private static final double MOTION_SPEED_MULTIPLIER = 10.0;
    private static final int DETAIL_WIDTH = 408;
    private static final int COMPACT_DETAIL_WIDTH = 384;
    private static final int COMPACT_SCREEN_WIDTH = 1028;
    private static final int REFRESH_INTERVAL_MS = 500;
    private static final int MOTION_INTERVAL_MS = 30;
    private static final Color DELETE_INK = color("c5667c");
    private final java.util.Map<String, BubbleButton> bubbleButtons = new java.util.LinkedHashMap<>();
    private final UiSession session;
    private final Consumer<RoomView> enter;
    private final Runnable requestsOpened;
    private final JPanel detail = panel(new BorderLayout()), field = new JPanel(null);
    private final RoomDetailPanel detailContent;
    private String category = "전체";
    private UiSession.Room selected;
    private boolean automatic;
    private final javax.swing.Timer timer;
    private String signature = "";
    private DetailState detailState;
    private record DetailState(String roomId, RoomView view, int people, boolean joined, boolean ended,
            boolean hidden, long retrySeconds, String requestId, ChatRequestStatus requestStatus) {
    }
    private final javax.swing.Timer motion = new javax.swing.Timer(MOTION_INTERVAL_MS,
            e -> BubbleMotion.advance(field, MOTION_SPEED_MULTIPLIER));
    public HomeScreen(UiData data, Consumer<RoomView> enter, Runnable requestsOpened) {
        super(new BorderLayout());
        session = data.session();
        detailContent = new RoomDetailPanel(this::closeDetail, () -> {
            if (selected != null) session.react(selected);
        });
        this.enter = enter;
        this.requestsOpened = requestsOpened;
        setBackground(BG);
        BubbleStage center = new BubbleStage(field, this::openRequestManager,
                () -> new RoomEditor(SwingUtilities.getWindowAncestor(this), null,
                session, this::refresh).setVisible(true));
        add(center);
        detail.setPreferredSize(new Dimension(DETAIL_WIDTH, 0));
        detail.setOpaque(true);
        detail.setBackground(Color.WHITE);
        detail.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, LINE));
        JScrollPane detailScroll = scroll(detailContent);
        detailScroll.getViewport().setBackground(Color.WHITE);
        detail.add(detailScroll);
        detail.setVisible(false);
        add(detail, BorderLayout.EAST);
        timer = new javax.swing.Timer(REFRESH_INTERVAL_MS, e -> refresh());
        addHierarchyListener(e -> {
            if (isShowing()) {
                refresh();
                timer.start();
                motion.start();
            } else {
                timer.stop();
                motion.stop();
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                int detailWidth = getWidth() <= COMPACT_SCREEN_WIDTH ? COMPACT_DETAIL_WIDTH : DETAIL_WIDTH;
                if (detail.getPreferredSize().width != detailWidth) {
                    detail.setPreferredSize(new Dimension(detailWidth, 0));
                    revalidate();
                }
            }
        });
        field.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                BubblePlacement.layout(field);
            }
        });
        refresh();
    }

    public void onEnter() {
        detailState = null;
        var recent = session.recent();
        if (recent != null) {
            selected = recent;
            automatic = true;
            detail.setVisible(true);
        }
        refresh();
    }

    public void filter(String value) {
        category = value;
        signature = "";
        refresh();
    }

    public void refresh() {
        var rooms = session.rooms();
        String key = category + rooms.stream()
                .map(r -> r.id + ":" + r.people + ":" + session.hidden(r) + ":" + r.bubbleDiameter() + ":" + r.view)
                .toList();
        if (!key.equals(signature)) {
            signature = key;
            java.util.Set<String> visible = new java.util.HashSet<>();
            for (var r : rooms) {
                if (session.hidden(r) || !category.equals("전체") && !category.equals(r.view.category())) {
                    continue;
                }
                visible.add(r.id);
                BubbleButton b = bubbleButtons.get(r.id);
                if (b == null) {
                    b = new BubbleButton(r);
                    int index = bubbleButtons.size();
                    b.setVelocity(new double[] {
                        .35 + (index % 3) * .12, .24 + (index % 2) * .1
                    });
                    b.addActionListener(e -> {
                        selected = r;
                        for (BubbleButton other : bubbleButtons.values()) {
                            other.setSelected(other == e.getSource());
                        }
                        automatic = false;
                        detailState = null;
                        detail.setVisible(true);
                        session.requestDetail(r);
                        renderDetail();
                    });
                    bubbleButtons.put(r.id, b);
                }
                b.setSelected(selected == r);
                if (b.getParent() != field) {
                    field.add(b);
                }
                b.repaint();
            }
            // Keep each room's button and motion state when filtered out. Never remove a
            // pressed/hovered button merely because counts or request state changed.
            for (var entry : bubbleButtons.entrySet()) {
                JButton b = entry.getValue();
                if (!visible.contains(entry.getKey()) && b.getParent() == field) {
                    field.remove(b);
                }
            }
            BubblePlacement.layout(field);
            field.revalidate();
            field.repaint();
        }
        if (selected != null) {
            if (session.hidden(selected) && !automatic) {
                detail.setVisible(false);
                selected = null;
                revalidate();
            } else {
                renderDetail();
            }
        }
    }

    private void renderDetail() {
        if (selected == null) {
            return;
        }
        var r = selected;
        var request = session.outgoing(r);
        detailContent.updateRemaining(r.expiresAt, session.now());
        detailContent.updateReaction(r.reactionCount, session.canReact(r));
        long remaining = session.retrySeconds(r);
        DetailState next = new DetailState(r.id, r.view, r.people, r.joined, r.ended, session.hidden(r), remaining,
                request == null ? null : request.id, request == null ? null : request.status);
        if (next.equals(detailState)) {
            return;
        }
        detailState = next;
        detailContent.showRoom(r.view, r.people);
        JPanel p = column();
        String text = r.ended ? "기록 보기" : r.joined ? "대화로 이동" : session.hidden(r) ? "현재 참여할 수 없어요" : remaining > 0
                ? remaining + "초 후 재요청" : request != null && request.status == ChatRequestStatus.PENDING ? "참여 요청 대기 중" : r.view.privateRoom()
                && !r.view.owner() ? "참여 요청하기" : "대화 참여하기";
        JButton join = button(text, true, () -> UiErrors.perform(this, () -> {
            if (!r.joined && !r.ended && r.view.privateRoom() && !r.view.owner()) {
                session.request(r);
            } else {
                enter.accept(r.view);
            }
            refresh();
        }));
        boolean entryEnabled = r.joined || r.ended || !session.hidden(r) && remaining == 0
                && (request == null || request.status != ChatRequestStatus.PENDING);
        join.setEnabled(entryEnabled);
        join.putClientProperty("gradient", true);
        RoomDetailPanel.addAction(p, join, true, 14);
        if (request != null && request.status == ChatRequestStatus.REJECTED) {
            append(p, label("참여 요청이 거절되었어요.", 11, MUTED), 12);
        }
        if (r.view.owner()) {
            if (r.view.privateRoom()) {
                JButton manage = button("참여 요청 확인", true, this::openRequestManager);
                RoomDetailPanel.addAction(p, manage, true, 8);
            }
            JButton edit = button("비눗방울 수정", false, () -> new RoomEditor(SwingUtilities.getWindowAncestor(this), r.view,
                    session, this::refresh).setVisible(true));
            edit.setEnabled(!r.removed);
            JButton remove = button("비눗방울 삭제", false, () -> {
                if (ActionDialog.confirm(this, "비눗방울을 삭제할까요?", "삭제")) {
                    session.remove(r);
                    refresh();
                }
            });
            remove.setEnabled(!r.deleted);
            JPanel ownerActions = panel(new GridLayout(1, 2, 7, 0));
            edit.setText("수정");
            edit.setIcon(penIcon());
            remove.setText("삭제");
            remove.setForeground(DELETE_INK);
            edit.setFont(font(11, false));
            remove.setFont(font(11, false));
            ownerActions.add(edit);
            ownerActions.add(remove);
            ownerActions.setPreferredSize(new Dimension(0, 35));
            ownerActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            append(p, ownerActions, 0);
        }
        detailContent.setActions(p);
        detail.revalidate();
        detail.repaint();
        revalidate();
    }

    public void resetLive() {
        closeDetail();
        bubbleButtons.clear();
        field.removeAll();
        signature = "";
        refresh();
    }

    private void openRequestManager() {
        session.requestIncomingRequests();
        RequestManagerDialog dialog = new RequestManagerDialog(SwingUtilities.getWindowAncestor(this),
                session, this::refresh);
        requestsOpened.run();
        dialog.setVisible(true);
    }

    private void closeDetail() {
        selected = null;
        detailState = null;
        for (JButton button : bubbleButtons.values()) {
            button.setSelected(false);
        }
        automatic = false;
        detail.setVisible(false);
        revalidate();
        repaint();
    }

    @Override
    public void removeNotify() {
        motion.stop();
        timer.stop();
        super.removeNotify();
    }
}
