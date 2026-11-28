package bubbling.client.ui.game;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

import static bubbling.client.ui.style.Styles.color;

/** Independent borderless window hosting mini-games outside the main shell. */
public final class GameFrame extends JFrame {
    public GameFrame(GameView content, Window owner) {
        super("Bubbling - 미니 게임");
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(880, 640);
        setMinimumSize(new Dimension(780, 580));
        getRootPane().setBorder(BorderFactory.createLineBorder(color("d8cff0"), 1));
        setLocationRelativeTo(owner);
        setContentPane(Objects.requireNonNull(content));
    }

    /** Enables dragging the undecorated GameFrame from a top header bar. */
    public static void installDragHandle(JComponent handle) {
        Objects.requireNonNull(handle);
        MouseAdapter drag = new MouseAdapter() {
            private Point offset;

            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                Window window = SwingUtilities.getWindowAncestor(handle);
                if (window == null) return;
                offset = new Point(e.getXOnScreen() - window.getX(), e.getYOnScreen() - window.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                offset = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (offset == null || !SwingUtilities.isLeftMouseButton(e)) return;
                Window window = SwingUtilities.getWindowAncestor(handle);
                if (window == null) return;
                window.setLocation(e.getXOnScreen() - offset.x, e.getYOnScreen() - offset.y);
            }
        };
        handle.addMouseListener(drag);
        handle.addMouseMotionListener(drag);
    }
}
