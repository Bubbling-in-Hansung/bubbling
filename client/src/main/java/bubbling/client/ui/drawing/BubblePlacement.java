package bubbling.client.ui.drawing;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import bubbling.client.ui.model.UiSession;

/** Initial placement only. Relayout preserves live positions and velocities. */
public final class BubblePlacement {
    private static final int SHADOW = BubbleGeometry.SHADOW_PADDING, ATTEMPTS = 600;
    private static final double GAP = BubbleGeometry.GAP;
    private record Circle(double x, double y, double radius) {
    }

    private BubblePlacement() {
    }

    public static void layout(JPanel field) {
        if (field.getWidth() <= 0 || field.getHeight() <= 0) {
            return;
        }
        List<Circle> placed = new ArrayList<>();
        // Existing bubbles are anchors, including after a filter or detail-panel resize.
        for (Component component : field.getComponents()) {
            JComponent button = (JComponent) component;
            double[] position = (double[]) button.getClientProperty("position");
            if (position == null) {
                continue;
            }
            int diameter = ((UiSession.Room) button.getClientProperty("room")).view.size();
            apply(button, position, diameter);
            placed.add(circle(position, diameter));
        }
        for (Component component : field.getComponents()) {
            JComponent button = (JComponent) component;
            if (button.getClientProperty("position") != null) {
                continue;
            }
            UiSession.Room room = (UiSession.Room) button.getClientProperty("room");
            int size = room.view.size() + SHADOW * 2;
            int maxX = Math.max(0, field.getWidth() - size), maxY = Math.max(0, field.getHeight() - size);
            double x = clamp(room.view.x() * field.getWidth() - SHADOW, maxX);
            double y = clamp(room.view.y() * field.getHeight() - SHADOW, maxY);
            double radius = room.view.size() / 2.0, offset = SHADOW + radius;
            double score = overlap(x + offset, y + offset, radius, placed);
            Random random = new Random(room.id.hashCode());
            for (int attempt = 0; score > 0 && attempt < ATTEMPTS; attempt++) {
                double candidateX = random.nextDouble() * maxX, candidateY = random.nextDouble() * maxY;
                double candidateScore = overlap(candidateX + offset, candidateY + offset, radius, placed);
                if (candidateScore < score) {
                    x = candidateX;
                    y = candidateY;
                    score = candidateScore;
                }
            }
            double[] position = {
                x, y
            };
            button.putClientProperty("position", position);
            apply(button, position, room.view.size());
            placed.add(circle(position, room.view.size()));
        }
    }

    private static void apply(JComponent button, double[] p, int diameter) {
        button.setBounds((int) Math.round(p[0]), (int) Math.round(p[1]), diameter + SHADOW * 2, diameter + SHADOW * 2);
    }

    private static Circle circle(double[] p, int diameter) {
        double radius = diameter / 2.0;
        return new Circle(p[0] + SHADOW + radius, p[1] + SHADOW + radius, radius);
    }

    private static double clamp(double value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private static double overlap(double x, double y, double radius, List<Circle> placed) {
        double sum = 0;
        for (Circle other : placed) {
            double penetration = radius + other.radius() + GAP - Math.hypot(x - other.x(), y - other.y());
            if (penetration > 0) {
                sum += penetration * penetration;
            }
        }
        return sum;
    }
}
