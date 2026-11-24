package bubbling.client.ui.dialog;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

import static bubbling.client.ui.style.Styles.*;

/** Temporary owner scrim; restores the previous glass pane and keyboard focus. */
final class ModalStyle {
    private ModalStyle() {
    }

    static void prepare(JDialog dialog) {
        dialog.setUndecorated(true);
        dialog.setResizable(false);
    }

    static JPanel surface(int gap) {
        JPanel panel = new JPanel(new BorderLayout(0, gap));
        panel.setBackground(Color.WHITE);
        pad(panel, 24);
        return panel;
    }

    static JPanel header(String heading, String description, Runnable dismiss) {
        JPanel header = panel(new BorderLayout(14, 0));
        JPanel copy = column();
        append(copy, title(heading, 19), description == null ? 0 : 6);
        if (description != null) {
            append(copy, label(description, 11, color("736b80")), 0);
        }
        header.add(copy);
        JPanel corner = panel(new BorderLayout());
        JButton close = closeButton("닫기", dismiss);
        close.setMargin(new Insets(0, 0, 0, 0));
        close.getAccessibleContext().setAccessibleName("닫기");
        corner.add(close, BorderLayout.NORTH);
        header.add(corner, BorderLayout.EAST);
        return header;
    }

    static JButton action(String text, boolean primary, Runnable task) {
        JButton control = button(text, primary, task);
        control.setRolloverEnabled(true);
        control.putClientProperty("gradient", primary);
        control.setPreferredSize(new Dimension(Math.max(86, control.getPreferredSize().width), 38));
        return control;
    }

    static JButton danger(String text, Runnable task) {
        JButton control = action(text, false, task);
        control.putClientProperty("soft", true);
        control.putClientProperty("softHover", color("f9dce5"));
        control.setBackground(color("fff0f4"));
        control.setForeground(color("a44660"));
        return control;
    }

    static JScrollPane scrollContent(JComponent content) {
        JScrollPane pane = scroll(content);
        pane.getViewport().setBackground(Color.WHITE);
        JScrollBar bar = pane.getVerticalScrollBar();
        bar.setPreferredSize(new Dimension(10, 0));
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected void paintTrack(Graphics source, JComponent component, Rectangle bounds) {
                source.setColor(Color.WHITE);
                source.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            protected void paintThumb(Graphics source, JComponent component, Rectangle bounds) {
                if (bounds.isEmpty() || !component.isEnabled()) return;
                Graphics2D g = graphics(source);
                g.setColor(color(isThumbRollover() ? "a990dc" : "d8cbed"));
                g.fillRoundRect(bounds.x + 2, bounds.y, Math.max(1, bounds.width - 4), bounds.height, 6, 6);
                g.dispose();
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return hiddenArrow();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return hiddenArrow();
            }

            private JButton hiddenArrow() {
                JButton arrow = new JButton();
                arrow.setPreferredSize(new Dimension());
                arrow.setFocusable(false);
                return arrow;
            }
        });
        return pane;
    }

    static void install(JDialog dialog) {
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        if (dialog.isUndecorated() && dialog.getGraphicsConfiguration().getDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
            dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), 38, 38));
        }
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
