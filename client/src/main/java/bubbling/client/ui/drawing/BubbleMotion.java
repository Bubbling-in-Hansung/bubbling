package bubbling.client.ui.drawing;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import bubbling.client.ui.model.UiSession;

/** EDT-only motion: near-field steering, small integration steps and contact constraints. */
public final class BubbleMotion {
    private static final double GAP = BubbleGeometry.GAP, SOFT_RANGE = 28, MAX_STEP = 2;
    private static final int CONTACT_PASSES = 16;
    private BubbleMotion() {
    }

    private static final class Body {
        final JButton button;
        final double[] p, v;
        final double radius, offset, speed;
        final boolean fixed, recovering;
        final double maxX, maxY;
        Body(JButton b, JPanel field) {
            button = b;
            p = position(b);
            v = (double[]) b.getClientProperty("velocity");
            radius = ((UiSession.Room) b.getClientProperty("room")).bubbleDiameter() / 2.0;
            offset = BubbleGeometry.SHADOW_PADDING + radius;
            speed = Math.hypot(v[0], v[1]);
            fixed = b.getModel().isRollover() || b.getModel().isPressed();
            maxX = Math.max(0, field.getWidth() - b.getWidth());
            maxY = Math.max(0, field.getHeight() - b.getHeight());
            recovering = p[0] < 0 || p[0] > maxX || p[1] < 0 || p[1] > maxY;
            // A narrower stage must not snap a bubble to its new boundary.
            if (recovering && !fixed) {
                double tx = Math.max(0, Math.min(maxX, p[0])), ty = Math.max(0, Math.min(maxY, p[1]));
                double dx = tx - p[0], dy = ty - p[1], distance = Math.hypot(dx, dy);
                double fraction = distance == 0 ? 0 : Math.min(1, 4 / distance);
                p[0] += dx * fraction;
                p[1] += dy * fraction;
                if (dx < 0) {
                    v[0] = - Math.abs(v[0]);
                } else if (dx > 0) {
                    v[0] = Math.abs(v[0]);
                }
                if (dy < 0) {
                    v[1] = - Math.abs(v[1]);
                } else if (dy > 0) {
                    v[1] = Math.abs(v[1]);
                }
            }
        }
        private static double[] position(JButton b) {
            double[] p = (double[]) b.getClientProperty("position");
            if (p == null) {
                p = new double[] {
                    b.getX(), b.getY()
                };
                b.putClientProperty("position", p);
            }
            return p;
        }
        double x() {
            return p[0] + offset;
        }
        double y() {
            return p[1] + offset;
        }
        void constrain() {
            if (fixed || recovering) {
                return;
            }
            if (p[0] < 0) {
                p[0] = 0;
                v[0] = Math.abs(v[0]);
            }
            if (p[0] > maxX) {
                p[0] = maxX;
                v[0] = - Math.abs(v[0]);
            }
            if (p[1] < 0) {
                p[1] = 0;
                v[1] = Math.abs(v[1]);
            }
            if (p[1] > maxY) {
                p[1] = maxY;
                v[1] = - Math.abs(v[1]);
            }
        }
    }

    public static void advance(JPanel field, double multiplier) {
        if (field.getWidth() <= 0 || field.getHeight() <= 0) {
            return;
        }
        List<Body> bodies = new ArrayList<>();
        double travel = 0;
        for (Component c : field.getComponents()) {
            Body b = new Body((JButton) c, field);
            bodies.add(b);
            if (!b.fixed && !b.recovering) {
                travel = Math.max(travel, b.speed * multiplier);
            }
        }
        int steps = Math.max(1, (int) Math.ceil(travel / MAX_STEP));
        for (int step = 0; step < steps; step++) {
            // Turn gently before contact instead of exchanging velocities like billiard balls.
            for (int i = 0; i < bodies.size(); i++) {
                for (int j = i + 1; j < bodies.size(); j++) {
                    Body a = bodies.get(i), b = bodies.get(j);
                    if (a.recovering || b.recovering) {
                        continue;
                    }
                    double dx = b.x() - a.x(), dy = b.y() - a.y();
                    double limit = a.radius + b.radius + GAP, range = limit + SOFT_RANGE;
                    if (dx * dx + dy * dy >= range * range) {
                        continue;
                    }
                    double distance = Math.hypot(dx, dy);
                    double nx = distance < .0001 ? 1 : dx / distance, ny = distance < .0001 ? 0 : dy / distance;
                    double strength = Math.min(1, Math.max(0, (limit + SOFT_RANGE - distance) / SOFT_RANGE));
                    steer(a, - nx, - ny, strength / steps);
                    steer(b, nx, ny, strength / steps);
                }
            }
            for (Body b : bodies) {
                if (!b.fixed && !b.recovering) {
                    b.p[0] += b.v[0] * multiplier / steps;
                    b.p[1] += b.v[1] * multiplier / steps;
                }
                b.constrain();
            }
            // Resolve residual contact with repeated wall correction; hovering bodies are immovable.
            for (int pass = 0; pass < CONTACT_PASSES; pass++) {
                double worst = 0;
                for (int i = 0; i < bodies.size(); i++) {
                    for (int j = i + 1; j < bodies.size(); j++) {
                        Body a = bodies.get(i), b = bodies.get(j);
                        if (a.recovering || b.recovering || a.fixed && b.fixed) {
                            continue;
                        }
                        double dx = b.x() - a.x(), dy = b.y() - a.y(), limit = a.radius + b.radius + GAP;
                        if (dx * dx + dy * dy >= limit * limit) {
                            continue;
                        }
                        double distance = Math.hypot(dx, dy);
                        double penetration = limit - distance;
                        if (penetration <= 0) {
                            continue;
                        }
                        worst = Math.max(worst, penetration);
                        double nx = distance < .0001 ? 1 : dx / distance, ny = distance < .0001 ? 0 : dy / distance;
                        // Share the correction, then give unused travel to the body with free space.
                        // This avoids repeatedly pushing one body through a wall and clamping it back.
                        double capacityA = available(a, - nx, - ny), capacityB = available(b, nx, ny);
                        double moveA = Math.min(capacityA, penetration / 2), moveB = Math.min(capacityB, penetration / 2);
                        double remainder = penetration - moveA - moveB;
                        double extraA = Math.min(Math.max(0, capacityA - moveA), remainder);
                        moveA += extraA;
                        remainder -= extraA;
                        moveB += Math.min(Math.max(0, capacityB - moveB), remainder);
                        if (moveA > 0) {
                            a.p[0] -= nx * moveA;
                            a.p[1] -= ny * moveA;
                        }
                        if (moveB > 0) {
                            b.p[0] += nx * moveB;
                            b.p[1] += ny * moveB;
                        }
                    }
                }
                for (Body b : bodies) {
                    b.constrain();
                }
                if (worst < .05) {
                    break;
                }
            }
        }
        for (Body b : bodies) {
            b.button.setLocation((int) Math.round(b.p[0]), (int) Math.round(b.p[1]));
        }
    }

    /** Maximum displacement along a unit direction that stays inside the field. */
    private static double available(Body b, double nx, double ny) {
        if (b.fixed || b.recovering) {
            return 0;
        }
        double distance = Double.POSITIVE_INFINITY;
        if (nx > 0.000001) {
            distance = Math.min(distance, (b.maxX - b.p[0]) / nx);
        } else if (nx < - .000001) {
            distance = Math.min(distance, - b.p[0] / nx);
        }
        if (ny > 0.000001) {
            distance = Math.min(distance, (b.maxY - b.p[1]) / ny);
        } else if (ny < - .000001) {
            distance = Math.min(distance, - b.p[1] / ny);
        }
        return Math.max(0, distance);
    }

    private static void steer(Body b, double nx, double ny, double strength) {
        if (b.fixed || b.recovering || b.speed == 0) {
            return;
        }
        // A small tangential component lets exactly head-on pairs pass around one another.
        double amount = .12 * strength * b.speed;
        b.v[0] += amount * (nx - ny * .35);
        b.v[1] += amount * (ny + nx * .35);
        double length = Math.hypot(b.v[0], b.v[1]);
        if (length > 0) {
            b.v[0] *= b.speed / length;
            b.v[1] *= b.speed / length;
        }
    }
}
