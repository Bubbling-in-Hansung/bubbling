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
        JPanel body = panel(new BorderLayout(0, 16));
        pad(body, 24);
        body.add(title("모든 비눗방울의 참여 요청", 19), BorderLayout.NORTH);
        body.add(scroll(rows));
        body.add(button("닫기", false, this::dispose), BorderLayout.SOUTH);
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
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void refresh() {
        var incoming = session.requests().stream().filter(q -> q.incoming).toList();
        String key = incoming.stream().map(q -> q.id + ":" + q.status + ":" + roomTitle(q)
                + ":" + canAccept(q)).toList().toString();
        if (key.equals(signature)) {
            return;
        }
        signature = key;
        rows.removeAll();
        for (var q : incoming) {
            JPanel row = card(Color.WHITE, 24);
            row.setLayout(new BorderLayout(10, 8));
            pad(row, 14);
            JPanel text = column();
            append(text, title(roomTitle(q), 12), 6);
            append(text, label(q.name + " · " + status(q.status), 10, MUTED), 0);
            row.add(text);
            JPanel actions = panel(new FlowLayout());
            for (boolean accept : new boolean[] {
                false, true
            }) {
                JButton b = button(accept ? "승인" : "거절", accept, () -> UiErrors.perform(this, () -> {
                    session.decide(q, accept);
                    changed.run();
                    refresh();
                }));
                b.setEnabled(session.interactive() && q.status == ChatRequestStatus.PENDING
                        && (!accept || canAccept(q)));
                actions.add(b);
            }
            row.add(actions, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            append(rows, row, 10);
        }
        if (rows.getComponentCount() == 0) {
            append(rows, label("받은 참여 요청이 없어요.", 12, MUTED), 20);
        }
        rows.revalidate();
        rows.repaint();
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
