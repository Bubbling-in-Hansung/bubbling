package bubbling.client.ui.drawing;

import bubbling.client.ui.model.UiSession;
import javax.swing.*;

/** Geometry and physics state accessors shared by painting, hit testing, placement and motion. */
final class BubbleGeometry {
    static final int SHADOW_PADDING = 18;
    static final double GAP = 8;
    private BubbleGeometry() {
    }

    static UiSession.Room room(JComponent component) {
        return component instanceof BubbleButton bubble
                ? bubble.room()
                : (UiSession.Room) component.getClientProperty("room");
    }

    static double[] position(JComponent component) {
        return component instanceof BubbleButton bubble
                ? bubble.position()
                : (double[]) component.getClientProperty("position");
    }

    static void setPosition(JComponent component, double[] position) {
        if (component instanceof BubbleButton bubble) {
            bubble.setPosition(position);
        } else {
            component.putClientProperty("position", position);
        }
    }

    static double[] velocity(JComponent component) {
        return component instanceof BubbleButton bubble
                ? bubble.velocity()
                : (double[]) component.getClientProperty("velocity");
    }
}
