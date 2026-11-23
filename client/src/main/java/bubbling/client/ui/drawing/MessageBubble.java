package bubbling.client.ui.drawing;

import bubbling.client.ui.model.MessageText;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static bubbling.client.ui.style.Styles.*;

/** Native image slices, centered text and local expansion; stored message text is untouched. */
public final class MessageBubble extends JPanel {
    public static final int MAX_WIDTH = 220;
    private static final int MIN_WIDTH = 96;
    private static final int SIDE_PADDING = 12, VERTICAL_PADDING = 5;
    private static final int SLICE_BASE_WIDTH = 474, TOP_SLICE_HEIGHT = 105, BODY_SLICE_HEIGHT = 100,
            BOTTOM_SLICE_HEIGHT = 105, TAIL_SLICE_HEIGHT = 39;
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();
    private final boolean mine, historyStyle;
    private final BufferedImage topImage, bodyImage, bottomImage;
    private final CenteredText text;
    private final JScrollPane content;
    private final JButton toggle = new JButton();
    private final List<String> fullLines, collapsedLines;
    private final boolean expandable;
    private final Runnable resized;
    private boolean expanded;
    private int availableHeight = 300;
    private int topHeight, bottomHeight, tileHeight, bodyTiles, tailHeight, contentHeight, controlHeight;
    public MessageBubble(String message, boolean mine, boolean historyStyle, Runnable resized) {
        this.mine = mine;
        this.historyStyle = historyStyle;
        this.resized = resized;
        setOpaque(false);
        setLayout(null);
        String prefix = mine ? (historyStyle ? "p_r_" : "p_c_") : "w_";
        topImage = image(prefix + "top.png");
        bodyImage = image(prefix + "center.png");
        bottomImage = image(mine ? prefix + "bottom.png" : (historyStyle ? "w_l_bottom.png" : "w_c_bottom.png"));
        setFont(font(11, false));
        FontMetrics metrics = getFontMetrics(getFont());
        fullLines = MessageText.lines(message, metrics::stringWidth, MAX_WIDTH - SIDE_PADDING * 2);
        collapsedLines = fullLines.subList(0, Math.min(MessageText.MAX_LINES, fullLines.size()));
        expandable = fullLines.size() > MessageText.MAX_LINES || MessageText.characters(message).size() > MessageText.MAX_LENGTH;
        text = new CenteredText(message, collapsedLines);
        text.setFont(getFont());
        text.setForeground(mine ? Color.WHITE : INK);
        content = new JScrollPane(text);
        content.setOpaque(false);
        content.getViewport().setOpaque(false);
        content.setBorder(null);
        content.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        content.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        content.setWheelScrollingEnabled(false);
        content.addMouseWheelListener(event -> {
            JViewport viewport = content.getViewport();
            int bottom = Math.max(0, text.getPreferredSize().height - viewport.getExtentSize().height);
            if (!expanded || bottom == 0) {
                // A short/collapsed bubble must not swallow the outer history's wheel input.
                var ancestor = SwingUtilities.getAncestorOfClass(JScrollPane.class, MessageBubble.this);
                if (ancestor instanceof JScrollPane outer) {
                    Point point = SwingUtilities.convertPoint(content, event.getPoint(), outer);
                    outer.dispatchEvent(new MouseWheelEvent(outer, event.getID(), event.getWhen(), event.getModifiersEx(), point.x,
                            point.y, event.getXOnScreen(), event.getYOnScreen(), event.getClickCount(), event.isPopupTrigger(), event.getScrollType(),
                            event.getScrollAmount(), event.getWheelRotation(), event.getPreciseWheelRotation()));
                    event.consume();
                }
                return;
            }
            int delta = (int) Math.round(event.getPreciseWheelRotation() * metrics.getHeight() * 3);
            viewport.setViewPosition(new Point(0, Math.max(0, Math.min(bottom, viewport.getViewPosition().y + delta))));
            event.consume();
        });
        content.getVerticalScrollBar().setUnitIncrement(metrics.getHeight());
        add(content);
        toggle.setFont(getFont());
        toggle.setForeground(mine ? Color.WHITE : INK);
        toggle.setOpaque(false);
        toggle.setContentAreaFilled(false);
        toggle.setBorder(BorderFactory.createEmptyBorder());
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> setExpanded(!expanded));
        toggle.setVisible(expandable);
        add(toggle);
        MouseAdapter open = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && expandable && !expanded) {
                    setExpanded(true);
                }
            }
        };
        addMouseListener(open);
        text.addMouseListener(open);
        content.getViewport().addMouseListener(open);
        if (expandable) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            text.setCursor(getCursor());
        }
        updateSize();
    }

    private static BufferedImage image(String name) {
        if (IMAGES.containsKey(name)) {
            return IMAGES.get(name);
        }
        BufferedImage result = null;
        try (var stream = MessageBubble.class.getResourceAsStream("/chat/" + name)) {
            if (stream != null) {
                BufferedImage original = ImageIO.read(stream);
                if (original != null) {
                    int seam = name.endsWith("_top.png") ? original.getHeight() - 1 : name.endsWith("_bottom.png") ? 0 : original.getHeight() / 2;
                    int left = 0, right = original.getWidth() - 1;
                    while (left < right && (original.getRGB(left, seam) >>> 24) <= 10) {
                        left++;
                    }
                    while (right > left && (original.getRGB(right, seam) >>> 24) <= 10) {
                        right--;
                    }
                    result = original.getSubimage(left, 0, right - left + 1, original.getHeight());
                }
            }
        } catch (IOException ignored) {
            /* Native shape fallback below. */
        }
        IMAGES.put(name, result);
        return result;
    }

    private void setExpanded(boolean value) {
        expanded = value;
        updateSize();
        content.getViewport().setViewPosition(new Point());
        resized.run();
    }

    /** Character stacks supply their actual available space; long originals scroll inside. */
    public void setAvailableHeight(int height) {
        int next = Math.max(1, height);
        if (availableHeight != next) {
            availableHeight = next;
            if (expanded) {
                updateSize();
            }
        }
    }

    private void updateSize() {
        List<String> lines = expanded ? fullLines : collapsedLines;
        FontMetrics metrics = getFontMetrics(getFont());
        controlHeight = expandable ? metrics.getHeight() + 3 : 0;
        toggle.setText(expanded ? "<접기>" : "<더보기>");
        int textWidth = lines.stream().mapToInt(metrics::stringWidth).max().orElse(0);
        int width = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, Math.max(textWidth, expandable ? metrics.stringWidth(toggle.getText())
                : 0) + SIDE_PADDING * 2));
        // Each slice scales uniformly; extra body tiles change the overall aspect ratio.
        double scale = (double) width / (topImage == null ? SLICE_BASE_WIDTH : topImage.getWidth());
        topHeight = Math.max(1, (int) Math.round((topImage == null ? TOP_SLICE_HEIGHT : topImage.getHeight()) * scale));
        bottomHeight = Math.max(1, (int) Math.round(width * (bottomImage == null
                ? (double) BOTTOM_SLICE_HEIGHT / SLICE_BASE_WIDTH
                : (double) bottomImage.getHeight() / bottomImage.getWidth())));
        tileHeight = Math.max(1, (int) Math.round(width * (bodyImage == null
                ? (double) BODY_SLICE_HEIGHT / SLICE_BASE_WIDTH
                : (double) bodyImage.getHeight() / bodyImage.getWidth())));
        tailHeight = Math.max(1, (int) Math.ceil(bottomHeight * (double) TAIL_SLICE_HEIGHT / BOTTOM_SLICE_HEIGHT));
        int naturalTextHeight = lines.size() * metrics.getHeight();
        int fixedHeight = topHeight + bottomHeight;
        int textBudget = Math.max(metrics.getHeight(), availableHeight - fixedHeight - controlHeight * 2 - VERTICAL_PADDING * 2 - tileHeight);
        contentHeight = expanded ? Math.min(naturalTextHeight, textBudget) : naturalTextHeight;
        // Leave matching space above the text so the control does not shift its body center.
        int required = contentHeight + controlHeight * 2 + VERTICAL_PADDING * 2;
        bodyTiles = Math.max(0, (int) Math.ceil((required - (fixedHeight - tailHeight)) / (double) tileHeight));
        int height = fixedHeight + bodyTiles * tileHeight;
        text.showLines(lines, width - SIDE_PADDING * 2);
        text.setToolTipText(expanded && contentHeight < naturalTextHeight ? "마우스 휠로 원문을 확인할 수 있어요." : null);
        Dimension size = new Dimension(width, height);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        revalidate();
        repaint();
    }

    @Override
    public void doLayout() {
        int y = (getHeight() - tailHeight - contentHeight) / 2;
        content.setBounds(SIDE_PADDING, y, getWidth() - SIDE_PADDING * 2, contentHeight);
        toggle.setBounds(SIDE_PADDING, y + contentHeight, getWidth() - SIDE_PADDING * 2, controlHeight);
        content.doLayout();
        content.getViewport().doLayout();
    }

    @Override
    protected void paintComponent(Graphics source) {
        super.paintComponent(source);
        Graphics2D g = (Graphics2D) source.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        if (topImage != null && bodyImage != null && bottomImage != null) {
            g.drawImage(topImage, 0, 0, getWidth(), topHeight, null);
            for (int i = 0; i < bodyTiles; i++) {
                g.drawImage(bodyImage, 0, topHeight + i * tileHeight, getWidth(), tileHeight, null);
            }
            g.drawImage(bottomImage, 0, topHeight + bodyTiles * tileHeight, getWidth(), bottomHeight, null);
        } else {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(mine ? PURPLE : (historyStyle ? SOFT : Color.WHITE));
            int bottom = getHeight() - tailHeight;
            g.fillRoundRect(0, 0, getWidth(), bottom, Math.min(18, topHeight), Math.min(18, topHeight));
            int x = historyStyle ? (mine ? getWidth() - 18 : 18) : getWidth() / 2;
            g.fillPolygon(new int[] {
                x - 5, x + 5, x
            }, new int[] {
                bottom - 1, bottom - 1, getHeight() - 1
            }, 3);
        }
        g.dispose();
    }

    private static final class CenteredText extends JTextArea {
        private List<String> lines;
        private int width = MAX_WIDTH;
        CenteredText(String original, List<String> lines) {
            super(original);
            this.lines = lines;
            setEditable(false);
            setFocusable(false);
            setOpaque(false);
            setBorder(null);
        }
        void showLines(List<String> value, int width) {
            lines = value;
            this.width = width;
            revalidate();
            repaint();
        }
        @Override
        public Dimension getPreferredSize() {
            return lines == null ? new Dimension() : new Dimension(width, lines.size() * getFontMetrics(getFont()).getHeight());
        }
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        @Override
        protected void paintComponent(Graphics source) {
            Graphics2D g = (Graphics2D) source.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(getFont());
            g.setColor(getForeground());
            FontMetrics metrics = g.getFontMetrics();
            int y = Math.max(0, (getHeight() - lines.size() * metrics.getHeight()) / 2) + metrics.getAscent();
            for (String line : lines) {
                g.drawString(line, (getWidth() - metrics.stringWidth(line)) / 2, y);
                y += metrics.getHeight();
            }
            g.dispose();
        }
    }
}
