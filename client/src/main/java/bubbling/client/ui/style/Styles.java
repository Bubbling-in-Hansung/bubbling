package bubbling.client.ui.style;

import com.formdev.flatlaf.ui.FlatPasswordFieldUI;

import javax.swing.*;
import javax.swing.text.Element;
import javax.swing.text.PasswordView;
import javax.swing.text.View;
import java.awt.*;
import java.awt.geom.Path2D;

/** CSS tokens and native Swing controls; no HTML renderer. */
public final class Styles {
    public static final Color BG = color("faf9fe"), INK = color("29263a"), MUTED = color("aaa5b5"), PURPLE = color("7952de"),
            LINE = color("ebe8f4"), SOFT = color("f2efff"), SUBTLE = color("736b80"), COPY = color("625b70"),
            SURFACE = color("fbfafd"), DANGER_BG = color("fff0f4"), DANGER_INK = color("a44660"),
            BADGE_RED = color("c93d60"), ACTIVE_BG = color("e7f9f3"), ACTIVE_DOT = color("52d4aa");
    private static final Color DISABLED_FILL = color("e8e5ed"), CATEGORY_HOVER_BG = color("eee9ff"),
            TRANSPARENT = new Color(0, 0, 0, 0), GRADIENT_START = color("8254f6"), GRADIENT_END = color("6740dc"),
            CLOSE_BG = color("f6f4f9"), CLOSE_INK = color("9e99aa"), CATEGORY_ACTIVE_INK = color("6540d8"),
            CATEGORY_IDLE_INK = color("777388"), HEART_DISABLED = color("c98797"), INACTIVE_BG = color("f1eef5");
    private Styles() {
    }

    public static void install() {
        UIManager.put("TextComponent.arc", 22);
        UIManager.put("Component.arc", 18);
        UIManager.put("Component.focusColor", SOFT);
        UIManager.put("defaultFont", font(11, false));
    }

    public static Color color(String hex) {
        return new Color(Integer.parseInt(hex, 16));
    }

    public static Font font(int size, boolean bold) {
        return UiFonts.body(size, bold);
    }

    public static Graphics2D graphics(Graphics source) {
        Graphics2D g = (Graphics2D) source.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    public static JLabel label(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(size, false));
        l.setForeground(color);
        return l;
    }

    public static JLabel title(String text, int size) {
        JLabel l = label(text, size, INK);
        l.setFont(font(size, true));
        return l;
    }

    public static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static void pad(JComponent c, int n) {
        c.setBorder(BorderFactory.createEmptyBorder(n, n, n, n));
    }

    public static JPanel column() {
        JPanel p = panel(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    public static void append(JPanel p, JComponent c, int gap) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(c);
        p.add(Box.createVerticalStrut(gap));
    }

    public static JButton button(String text, boolean primary, Runnable action) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics source) {
                Graphics2D g = graphics(source);
                Color fill = primary ? PURPLE : (getModel().isRollover() || isSelected() ? SOFT : Color.WHITE);
                if (!isEnabled()) {
                    fill = DISABLED_FILL;
                }
                boolean borderless = Boolean.TRUE.equals(getClientProperty("borderless"));
                if (borderless && isEnabled() && hasFocus()) {
                    fill = SOFT;
                }
                boolean flat = Boolean.TRUE.equals(getClientProperty("flat"));
                boolean floating = Boolean.TRUE.equals(getClientProperty("floating"));
                boolean gradient = floating || Boolean.TRUE.equals(getClientProperty("gradient"));
                boolean soft = Boolean.TRUE.equals(getClientProperty("soft"));
                boolean category = Boolean.TRUE.equals(getClientProperty("categoryTab"));
                int arc = floating ? 48 : flat ? 36 : 20;
                if (soft && isEnabled() && !getModel().isRollover() && !isSelected()) {
                    fill = getBackground();
                }
                if (soft && isEnabled() && getModel().isRollover()
                        && getClientProperty("softHover") instanceof Color hoverColor) {
                    fill = hoverColor;
                }
                if (category) {
                    fill = getModel().isRollover() || isSelected() || hasFocus() ? CATEGORY_HOVER_BG : TRANSPARENT;
                }
                if (gradient && isEnabled()) {
                    g.setPaint(new GradientPaint(0, 0, GRADIENT_START, getWidth(), getHeight(), GRADIENT_END));
                } else {
                    g.setColor(fill);
                }
                if (!flat || soft || getModel().isRollover() || isSelected() || (category || borderless) && hasFocus()) {
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                }
                g.setColor(LINE);
                if (!primary && !flat && !borderless) {
                    g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                }
                g.dispose();
                super.paintComponent(source);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(font(11, true));
        b.setForeground(primary ? Color.WHITE : PURPLE);
        b.setMargin(new Insets(8, 12, 8, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    public static JButton closeButton(String tooltip, Runnable action) {
        JButton close = button("", false, action);
        close.putClientProperty("flat", true);
        close.putClientProperty("soft", true);
        close.setBackground(CLOSE_BG);
        close.setForeground(CLOSE_INK);
        close.setIcon(icon("close"));
        close.setBorder(BorderFactory.createEmptyBorder());
        close.setPreferredSize(new Dimension(34, 34));
        close.setToolTipText(tooltip);
        close.getAccessibleContext().setAccessibleName(tooltip);
        return close;
    }

    public static JButton reactionButton(Runnable action) {
        JButton reaction = button("", false, action);
        Icon heart = icon("heart");
        reaction.setIcon(heart);
        reaction.setDisabledIcon(heart);
        reaction.setIconTextGap(3);
        reaction.setMargin(new Insets(1, 5, 1, 5));
        reaction.setFont(font(9, true));
        reaction.putClientProperty("flat", true);
        return reaction;
    }

    public static void updateReactionButton(JButton button, int count, boolean enabled) {
        button.setEnabled(enabled);
        button.setText(Integer.toString(count));
        String countLabel = "공감 " + count + "개";
        String label = enabled ? "공감하기 · " + countLabel : countLabel;
        button.setToolTipText(label);
        button.getAccessibleContext().setAccessibleName(label);
    }

    public static JButton pending(String text) {
        JButton b = button(text, true, () -> {
        });
        b.setEnabled(false);
        b.setToolTipText("서버 핸들러 연결 후 사용할 수 있습니다.");
        return b;
    }

    public static void removeButtonOutline(JButton button) {
        Insets padding = button.getInsets();
        button.putClientProperty("borderless", true);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setBorder(BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
    }

    /** CSS category pills: transparent idle state, rounded fill and no focus/selection outline. */
    public static JButton categoryTab(String text, Runnable action) {
        JButton tab = button(text, false, action);
        tab.putClientProperty("flat", true);
        tab.putClientProperty("categoryTab", true);
        tab.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        tab.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        tab.setRolloverEnabled(true);
        updateCategoryTab(tab, false);
        tab.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent event) {
                updateCategoryTab(tab, true);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent event) {
                updateCategoryTab(tab, tab.isSelected());
            }
        });
        return tab;
    }

    public static void selectCategoryTab(JButton tab, boolean selected) {
        tab.setSelected(selected);
        updateCategoryTab(tab, selected);
    }

    private static void updateCategoryTab(JButton tab, boolean highlighted) {
        tab.setFont(UiFonts.body(12, highlighted ? 700 : 400));
        tab.setForeground(highlighted ? CATEGORY_ACTIVE_INK : CATEGORY_IDLE_INK);
    }

    /** Prototype's 24-unit pen path, rendered natively at 19px rather than a font-dependent emoji. */
    public static Icon penIcon() {
        return new Icon() {
            public int getIconWidth() {
                return 19;
            }
            public int getIconHeight() {
                return 19;
            }
            public void paintIcon(Component c, Graphics source, int x, int y) {
                Graphics2D g = graphics(source);
                g.translate(x, y);
                g.scale(19.0 / 24, 19.0 / 24);
                g.setColor(c.getForeground());
                g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(12, 20, 21, 20);
                Path2D pen = new Path2D.Double();
                pen.moveTo(16.5, 3.5);
                pen.curveTo(18.5, 1.5, 21.5, 4.5, 19.5, 6.5);
                pen.lineTo(8, 18);
                pen.lineTo(4, 19);
                pen.lineTo(5, 15);
                pen.closePath();
                g.draw(pen);
                g.dispose();
            }
        };
    }

    public static Icon icon(String name) {
        return new Icon() {
            public int getIconWidth() {
                return 22;
            }
            public int getIconHeight() {
                return 22;
            }
            public void paintIcon(Component c, Graphics source, int x, int y) {
                Graphics2D g = graphics(source);
                g.translate(x, y);
                g.setColor(PURPLE);
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (name.equals("close")) {
                    g.setColor(c.getForeground());
                    g.drawLine(5, 5, 17, 17);
                    g.drawLine(17, 5, 5, 17);
                } else if (name.equals("chevron-left")) {
                    g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D chevron = new Path2D.Float();
                    chevron.moveTo(14, 3);
                    chevron.lineTo(6, 11);
                    chevron.lineTo(14, 19);
                    g.draw(chevron);
                } else if (name.equals("home")) {
                    paintBubblesMark(g, 0, 0, 1.0);
                } else if (name.equals("chat")) {
                    Path2D bubble = new Path2D.Float();
                    bubble.moveTo(6, 3);
                    bubble.quadTo(3, 3, 3, 6);
                    bubble.lineTo(3, 12);
                    bubble.quadTo(3, 15, 6, 15);
                    bubble.lineTo(8.5, 15);
                    bubble.lineTo(11, 19);
                    bubble.lineTo(13.5, 15);
                    bubble.lineTo(16, 15);
                    bubble.quadTo(19, 15, 19, 12);
                    bubble.lineTo(19, 6);
                    bubble.quadTo(19, 3, 16, 3);
                    bubble.closePath();
                    g.draw(bubble);
                } else if (name.equals("user") || name.equals("users")) {
                    g.drawOval(7, 2, 7, 7);
                    g.drawArc(4, 11, 13, 14, 0, 180);
                    if (name.equals("users")) {
                        g.drawArc(15, 3, 5, 6, -80, 160);
                        g.drawArc(17, 11, 5, 10, -20, 100);
                    }
                } else if (name.equals("lock")) {
                    g.drawRoundRect(5, 10, 12, 10, 2, 2);
                    g.drawArc(8, 3, 6, 12, 0, 180);
                    g.fillOval(10, 13, 2, 3);
                } else if (name.equals("heart")) {
                    g.setColor(c.isEnabled() ? BADGE_RED : HEART_DISABLED);
                    Path2D heart = new Path2D.Float();
                    heart.moveTo(11, 18);
                    heart.curveTo(9, 16, 3, 12, 3, 8);
                    heart.curveTo(3, 4, 8, 2, 11, 6);
                    heart.curveTo(14, 2, 19, 4, 19, 8);
                    heart.curveTo(19, 12, 13, 16, 11, 18);
                    heart.closePath();
                    g.fill(heart);
                } else {
                    g.drawLine(5, 3, 5, 19);
                    g.drawLine(5, 3, 11, 3);
                    g.drawLine(5, 19, 11, 19);
                    g.drawLine(10, 11, 20, 11);
                    g.drawLine(16, 7, 20, 11);
                    g.drawLine(16, 15, 20, 11);
                }
                g.dispose();
            }
        };
    }

    public static JTextField input(String hint) {
        JTextField f = new JTextField();
        f.putClientProperty("JTextField.placeholderText", hint);
        f.setFont(font(10, false));
        f.setPreferredSize(new Dimension(200, 42));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return f;
    }

    public static JPasswordField passwordInput(String hint) {
        JPasswordField password = new JPasswordField();
        password.setUI(new FlatPasswordFieldUI() {
            @Override
            public View create(Element element) {
                return new PasswordView(element) {
                    @Override
                    protected int drawEchoCharacter(Graphics graphics, int x, int y, char mask) {
                        super.drawEchoCharacter(graphics, x, y, mask);
                        // Use the same character advance as Swing's caret and selection.
                        return x + password.getFontMetrics(password.getFont()).charWidth(mask);
                    }
                };
            }
        });
        password.setFont(font(12, false));
        password.setPreferredSize(new Dimension(200, 42));
        password.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        password.putClientProperty("JTextField.placeholderText", hint);
        return password;
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane s = new JScrollPane(new ScrollBody(c));
        s.setBorder(null);
        s.getViewport().setBackground(BG);
        s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        s.getVerticalScrollBar().setUnitIncrement(20);
        return s;
    }

    private static final class ScrollBody extends JPanel implements Scrollable {
        ScrollBody(JComponent child) {
            super(new BorderLayout());
            setOpaque(false);
            add(child, BorderLayout.NORTH);
        }
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
            return 20;
        }
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
            return Math.max(20, r.height - 20);
        }
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    public static JPanel card(Color color, int radius) {
        return card(color, radius, true);
    }

    public static JPanel card(Color color, int radius, boolean outline) {
        return new JPanel() {
            {
                setOpaque(false);
            }
            @Override
            protected void paintComponent(Graphics source) {
                Graphics2D g = graphics(source);
                g.setColor(color);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                if (outline) {
                    g.setColor(LINE);
                    g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                }
                g.dispose();
            }
        };
    }

    public static void text(Graphics2D g, String text, int x, int y, int size, Color c, boolean bold) {
        g.setFont(font(size, bold));
        g.setColor(c);
        g.drawString(text, x, y);
    }

    public static void centered(Graphics2D g, String text, int x, int y, int size, Color c) {
        g.setFont(font(size, false));
        g.setColor(c);
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
    }

    public static void paintStatusDot(Graphics source, int size, int innerOffset, int innerSize, boolean active) {
        Graphics2D g = graphics(source);
        g.setColor(active ? ACTIVE_BG : INACTIVE_BG);
        g.fillOval(0, 0, size, size);
        g.setColor(active ? ACTIVE_DOT : MUTED);
        g.fillOval(innerOffset, innerOffset, innerSize, innerSize);
        g.dispose();
    }

    private static void paintBubblesMark(Graphics2D g, int x, int y, double scale) {
        g.setStroke(new BasicStroke((float) (1.6 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Ellipse2D.Double(x + 2 * scale, y + 1 * scale, 9 * scale, 9 * scale));
        g.draw(new java.awt.geom.Ellipse2D.Double(x + 11 * scale, y + 11 * scale, 8 * scale, 8 * scale));
        g.draw(new java.awt.geom.Ellipse2D.Double(x + 1 * scale, y + 15 * scale, 4 * scale, 4 * scale));
    }

    public static void logo(Graphics2D g, int x, int y, Color c, int size) {
        g.setColor(c);
        paintBubblesMark(g, x, y, 1.45);
        g.setFont(UiFonts.logo(size));
        g.drawString("Bubbling", x + 41, y + 24);
    }
}
