package bubbling.client.ui.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Temporary owner scrim; restores the previous glass pane and keyboard focus. */
final class ModalStyle {
    private ModalStyle() {
    }

    static void install(JDialog dialog) {
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (!(dialog.getOwner() instanceof RootPaneContainer owner)) {
            return;
        }
        Component previous = owner.getGlassPane();
        boolean visible = previous.isVisible();
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JComponent scrim = new JComponent() {
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(25, 20, 45, 75));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        dialog.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                owner.setGlassPane(scrim);
                scrim.setVisible(true);
            }
            public void windowClosed(WindowEvent e) {
                if (owner.getGlassPane() == scrim) {
                    owner.setGlassPane(previous);
                    previous.setVisible(visible);
                }
                if (focus != null && focus.isShowing()) {
                    focus.requestFocusInWindow();
                }
            }
        });
    }
}
