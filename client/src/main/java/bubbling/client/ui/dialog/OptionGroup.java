package bubbling.client.ui.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Objects;
import static bubbling.client.ui.style.Styles.*;

/** Native mutually exclusive choices shared by the editor's option sections. */
final class OptionGroup<T> extends JPanel {
    record Choice<T>(T value, String title, String description, Icon icon) {
        Choice(T value, String title) {
            this(value, title, "", null);
        }
    }

    private T selected;

    OptionGroup(List<Choice<T>> choices, T initial, int height, int gap) {
        super(new GridLayout(1, choices.size(), gap, 0));
        setOpaque(false);
        setPreferredSize(new Dimension(0, height));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        setMinimumSize(new Dimension(0, height));
        ButtonGroup group = new ButtonGroup();
        boolean initialFound = choices.stream().anyMatch(choice -> Objects.equals(choice.value(), initial));
        T value = initial == null || initialFound ? initial : choices.getFirst().value();
        for (Choice<T> choice : choices) {
            JToggleButton button = new ChoiceButton(choice.title(), choice.description(), choice.icon());
            button.addItemListener(event -> {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    selected = choice.value();
                }
            });
            group.add(button);
            button.setSelected(Objects.equals(choice.value(), value));
            add(button);
        }
    }

    T value() {
        return selected;
    }

    private static final class ChoiceButton extends JToggleButton {
        private final String description;
        private final Icon choiceIcon;

        ChoiceButton(String title, String description, Icon choiceIcon) {
            super(title);
            this.description = description;
            this.choiceIcon = choiceIcon;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(font(description.isEmpty() ? 8 : 9, !description.isEmpty()));
            setToolTipText(description.isEmpty() ? title : title + " · " + description);
            getAccessibleContext().setAccessibleDescription(description);
        }

        @Override
        protected void paintComponent(Graphics source) {
            Graphics2D g = graphics(source);
            boolean highlighted = isSelected() || getModel().isRollover();
            g.setColor(color(highlighted ? "f3efff" : "fbfafd"));
            g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
            if (isSelected() || hasFocus()) {
                g.setColor(color("eee8ff"));
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
            }
            g.setStroke(new BasicStroke(1));
            g.setColor(color(isSelected() || hasFocus() ? "9c80ec" : "e9e4ef"));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
            int left = 11;
            if (choiceIcon != null) {
                choiceIcon.paintIcon(this, g, left, (getHeight() - choiceIcon.getIconHeight()) / 2);
                left += choiceIcon.getIconWidth() + 8;
            }
            g.clipRect(left, 2, Math.max(0, getWidth() - left - 8), getHeight() - 4);
            g.setFont(getFont());
            g.setColor(color(isSelected() ? "6f4cd2" : "635d6c"));
            FontMetrics metrics = g.getFontMetrics();
            int baseline = description.isEmpty() ? (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent()
                    : getHeight() / 2 - 6;
            int x = description.isEmpty() ? (getWidth() - metrics.stringWidth(getText())) / 2 : left;
            g.drawString(getText(), x, baseline);
            if (!description.isEmpty()) {
                g.setFont(font(7, false));
                g.setColor(color(isSelected() ? "8a70d7" : "9a94a3"));
                g.drawString(description, left, baseline + 21);
            }
            g.dispose();
        }
    }
}
