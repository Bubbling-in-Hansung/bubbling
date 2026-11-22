package bubbling.client.ui.dialog;

import bubbling.client.ui.model.*;
import bubbling.common.enums.ChatRequestStatus;
import javax.swing.*;
import java.awt.*;
import static bubbling.client.ui.style.Styles.*;

public final class RequestManagerDialog extends JDialog {
    private final javax.swing.Timer timer = new javax.swing.Timer(500, e -> refresh());
    private String signature = "";
    private final UiSession session;
    private final JPanel rows = column();
    private final Runnable changed;
    public RequestManagerDialog(Window owner, UiSession session, Runnable changed) {
        super(owner, "참여 요청 관리", ModalityType.APPLICATION_MODAL);
        this.session = session;
        this.changed = changed;
        ModalStyle.prepare(this);
        JPanel body = ModalStyle.surface(16);
        body.add(ModalStyle.header(getTitle(), "내 비눗방울에 도착한 요청을 확인하고 응답할 수 있어요.", this::dispose),
                BorderLayout.NORTH);
        body.add(ModalStyle.scrollContent(rows), BorderLayout.CENTER);
        JPanel footer = panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.add(ModalStyle.action("닫기", false, this::dispose));
        body.add(footer, BorderLayout.SOUTH);
        setContentPane(body);
        setSize(620, 480);
        ModalStyle.install(this);
        refresh();
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent e) {
                timer.start();
            }
            public void windowClosed(java.awt.event.WindowEvent e) {
                timer.stop();
            }
        });
    }

    private void refresh() {
        var incoming = session.requests().stream()
                .filter(q -> q.incoming && q.status != ChatRequestStatus.ACCEPTED)
                .toList();
        String key = incoming.stream().map(q -> q.id + ":" + q.status + ":" + roomTitle(q)
                + ":" + canAccept(q)).toList().toString();
        if (key.equals(signature)) {
            return;
        }
        signature = key;
        rows.removeAll();
        for (var q : incoming) {
            append(rows, requestCard(q), 10);
        }
        if (incoming.isEmpty()) {
            JPanel empty = card(color("f8f5fe"), 24);
            empty.setLayout(new GridBagLayout());
            JLabel message = label("받은 참여 요청이 없어요.", 12, color("736b80"));
            message.setIcon(icon("chat"));
            message.setIconTextGap(10);
            empty.add(message);
            empty.setPreferredSize(new Dimension(0, 114));
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 114));
            append(rows, empty, 0);
        }
        rows.revalidate();
        rows.repaint();
    }

    private JPanel requestCard(UiSession.Request request) {
        JPanel row = card(color("fbfafd"), 24);
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JPanel avatar = card(SOFT, 16);
        avatar.setLayout(new GridBagLayout());
        avatar.setPreferredSize(new Dimension(42, 42));
        avatar.add(new JLabel(icon("user")));
        row.add(avatar, BorderLayout.WEST);

        JPanel text = panel(new GridLayout(2, 1, 0, 4));
        JLabel name = title(request.name, 12);
        name.setToolTipText(request.name);
        text.add(name);
        JLabel room = label(roomTitle(request), 10, color("736b80"));
        room.setToolTipText(room.getText());
        text.add(room);
        row.add(text, BorderLayout.CENTER);

        JPanel actions = panel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        JLabel state = label(status(request.status), 10, color(switch (request.status) {
            case PENDING -> "7052b6";
            case ACCEPTED -> "38765e";
            case REJECTED -> "a44660";
            case CANCELLED -> "736b80";
        }));
        JPanel statusBadge = card(color(switch (request.status) {
            case PENDING -> "f2efff";
            case ACCEPTED -> "eaf7f0";
            case REJECTED -> "fff0f4";
            case CANCELLED -> "f1eef4";
        }), 18);
        statusBadge.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 7));
        statusBadge.add(state);
        actions.add(statusBadge);
        JButton reject = ModalStyle.danger("거절", () -> decide(request, false));
        JButton accept = ModalStyle.action("승인", true, () -> decide(request, true));
        reject.setPreferredSize(new Dimension(62, 34));
        accept.setPreferredSize(new Dimension(62, 34));
        boolean pending = session.interactive() && request.status == ChatRequestStatus.PENDING;
        reject.setEnabled(pending);
        accept.setEnabled(pending && canAccept(request));
        actions.add(reject);
        actions.add(accept);
        row.add(actions, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private void decide(UiSession.Request request, boolean accept) {
        UiErrors.perform(this, () -> {
            session.decide(request, accept);
            changed.run();
            refresh();
        });
    }

    private String roomTitle(UiSession.Request request) {
        UiSession.Room room = session.room(request.roomId);
        return room == null ? "비눗방울 참여 요청" : room.view.title();
    }

    private boolean canAccept(UiSession.Request request) {
        UiSession.Room room = session.room(request.roomId);
        return room != null && !session.hidden(room);
    }

    private String status(ChatRequestStatus value) {
        return switch (value) {
            case PENDING -> "응답 대기";
            case ACCEPTED -> "승인됨";
            case REJECTED -> "거절됨";
            case CANCELLED -> "취소됨";
        };
    }
}
