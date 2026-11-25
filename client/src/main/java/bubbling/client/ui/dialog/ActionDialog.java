package bubbling.client.ui.dialog;

import javax.swing.*;
import java.awt.*;

import static bubbling.client.ui.style.Styles.*;

public final class ActionDialog {
    private ActionDialog() {
    }

    public static boolean confirm(Component source, String message, String action) {
        String heading = action + " 확인";
        JDialog dialog = new JDialog(ModalStyle.resolveOwner(source), heading, Dialog.ModalityType.APPLICATION_MODAL);
        ModalStyle.prepare(dialog);
        JPanel body = ModalStyle.surface(14);
        body.add(heading(heading), BorderLayout.NORTH);

        JTextArea copy = ModalStyle.messageArea(message, 12);
        copy.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        copy.getAccessibleContext().setAccessibleName("알림 내용");
        JScrollPane text = ModalStyle.scrollContent(copy);
        copy.setSize(316, Short.MAX_VALUE);
        text.setPreferredSize(new Dimension(316, Math.min(176, Math.max(36, copy.getPreferredSize().height))));
        body.add(text, BorderLayout.CENTER);

        JPanel actions = panel(new GridLayout(1, 0, 8, 0));
        boolean[] accepted = {false};
        Runnable submit = () -> {
            accepted[0] = true;
            dialog.dispose();
        };
        JButton cancel = ModalStyle.action("취소", false, dialog::dispose);
        actions.add(cancel);
        actions.add(ModalStyle.danger(action, submit));
        body.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(body);
        dialog.getRootPane().setDefaultButton(cancel);
        dialog.pack();
        ModalStyle.install(dialog);
        dialog.setVisible(true);
        return accepted[0];
    }

    private static JPanel heading(String heading) {
        JPanel header = panel(new BorderLayout(12, 0));
        JPanel badge = card(DANGER_BG, 18);
        badge.setLayout(new GridBagLayout());
        badge.setPreferredSize(new Dimension(42, 42));
        JLabel mark = title("!", 18);
        mark.setForeground(DANGER_INK);
        badge.add(mark);
        header.add(badge, BorderLayout.WEST);
        header.add(title(heading, 16), BorderLayout.CENTER);
        return header;
    }
}
