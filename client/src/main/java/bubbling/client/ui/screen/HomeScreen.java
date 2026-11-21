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
    private final java.util.Map<String, JButton> bubbleButtons = new java.util.LinkedHashMap<>();
    private final UiSession session;
    private final Consumer<RoomView> enter;
    private final JPanel detail = panel(new BorderLayout()), field = new JPanel(null);
    private final RoomDetailPanel detailContent = new RoomDetailPanel(this::closeDetail);
    private String category = "전체";
    private UiSession.Room selected;
    private boolean automatic;
    private final javax.swing.Timer timer;
    private String signature = "";
    private DetailState detailState;
    private record DetailState(String roomId, RoomView view, int people, boolean joined, boolean ended,
            boolean hidden, long retrySeconds, String requestId, ChatRequestStatus requestStatus) {
    }
    private final javax.swing.Timer motion = new javax.swing.Timer(30, e -> BubbleMotion.advance(field, MOTION_SPEED_MULTIPLIER));
    public HomeScreen(UiData data, Consumer<RoomView> enter) {
        super(new BorderLayout());
        session = data.session();
        this.enter = enter;
        setBackground(BG);
        BubbleStage center = new BubbleStage(field, () -> new RequestManagerDialog(SwingUtilities.getWindowAncestor(this),
                session, this::refresh).setVisible(true), () -> new RoomEditor(SwingUtilities.getWindowAncestor(this), null,
                session, this::refresh).setVisible(true), session.interactive());
        add(center);
        detail.setPreferredSize(new Dimension(340, 0));
        detail.setOpaque(true);
        detail.setBackground(Color.WHITE);
        detail.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, LINE));
        JScrollPane detailScroll = scroll(detailContent);
        detailScroll.getViewport().setBackground(Color.WHITE);
        detail.add(detailScroll);
        detail.setVisible(false);
        add(detail, BorderLayout.EAST);
        timer = new javax.swing.Timer(500, e -> refresh());
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
                detail.setPreferredSize(new Dimension(getWidth() <= 1028 ? 320 : 340, 0));
                BubblePlacement.layout(field);
                revalidate();
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
        session.tick();
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
        session.tick();
        var rooms = session.rooms();
        String key = category + rooms.stream().map(r -> r.id + ":" + r.people + ":" + session.hidden(r) + ":" + r.view).toList();
        if (!key.equals(signature)) {
            signature = key;
            java.util.Set<String> visible = new java.util.HashSet<>();
            for (var r : rooms) {
                if (session.hidden(r) || !category.equals("전체") && !category.equals(r.view.category())) {
                    continue;
                }
                visible.add(r.id);
                JButton b = bubbleButtons.get(r.id);
                if (b == null) {
                    b = new BubbleButton(r);
                    b.putClientProperty("room", r);
                    int index = bubbleButtons.size();
                    b.putClientProperty("velocity", new double[] {
                        .35 + (index % 3) * .12, .24 + (index % 2) * .1
                    });
                    b.addActionListener(e -> {
                        selected = r;
                        for (JButton other : bubbleButtons.values()) {
                            other.setSelected(other == e.getSource());
                        }
                        automatic = false;
                        detailState = null;
                        detail.setVisible(true);
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
            } else if (session.live()) {
                enter.accept(r.view);
            } else {
                session.enter(r);
                enter.accept(r.view);
            }
            refresh();
        }));
        boolean entryEnabled = session.interactive() && (r.joined || r.ended || !session.hidden(r) && remaining == 0
                && (request == null || request.status != ChatRequestStatus.PENDING));
        join.setEnabled(entryEnabled);
        join.putClientProperty("gradient", true);
        RoomDetailPanel.addAction(p, join, true, 14);
        if (request != null && request.status == ChatRequestStatus.REJECTED) {
            append(p, label("참여 요청이 거절되었어요.", 11, MUTED), 12);
        }
        if (session.preview() && request != null && request.status == ChatRequestStatus.PENDING) {
            append(p, label("미리보기 · 요청 결과 시연", 9, MUTED), 8);
            RoomDetailPanel.addAction(p, button("승인 결과 받기", false, () -> {
                session.decide(request, true);
                refresh();
                if (request.status == ChatRequestStatus.ACCEPTED) {
                    session.visit(r);
                    enter.accept(r.view);
                }
            }), false, 8);
            RoomDetailPanel.addAction(p, button("거절 결과 받기", false, () -> {
                session.decide(request, false);
                refresh();
            }), false, 12);
        }
        if (r.view.owner()) {
            JPanel owner = card(color("f4f0ff"), 24);
            owner.setLayout(new BorderLayout(0, 8));
            pad(owner, 12);
            owner.add(label("내가 만든 비눗방울", 9, color("8a73c8")), BorderLayout.NORTH);
            JButton manage = button("참여 요청 확인", true, () -> new RequestManagerDialog(SwingUtilities.getWindowAncestor(this),
                    session, this::refresh).setVisible(true));
            manage.setFont(font(9, true));
            owner.add(manage);
            append(p, owner, 8);
            JButton edit = button("비눗방울 수정", false, () -> new RoomEditor(SwingUtilities.getWindowAncestor(this), r.view,
                    session, this::refresh).setVisible(true));
            edit.setEnabled(session.interactive() && !r.removed);
            JButton remove = button("비눗방울 삭제", false, () -> {
                if (JOptionPane.showConfirmDialog(this, "비눗방울을 삭제할까요?", "확인", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    session.remove(r);
                    refresh();
                }
            });
            remove.setEnabled(session.interactive() && !r.removed);
            JPanel ownerActions = panel(new GridLayout(1, 2, 7, 0));
            edit.setText("수정");
            edit.setIcon(penIcon());
            remove.setText("삭제");
            remove.setForeground(color("c5667c"));
            edit.setFont(font(9, false));
            remove.setFont(font(9, false));
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
