package bubbling.client.ui.dialog;

import bubbling.client.ui.drawing.CharacterArt;
import bubbling.client.ui.model.CharacterChoice;
import javax.swing.*;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.*;
import java.util.function.IntConsumer;
import static bubbling.client.ui.style.Styles.*;

public final class CharacterPickerDialog extends JDialog {
    private int selected;
    public CharacterPickerDialog(Window owner, int initial, boolean editable, IntConsumer save) {
        super(owner, "프로필 캐릭터 선택", ModalityType.APPLICATION_MODAL);
        selected = initial;
        ModalStyle.prepare(this);
        JPanel body = ModalStyle.surface(16);
        body.add(ModalStyle.header(getTitle(), editable ? "채팅에서 사용할 캐릭터를 골라주세요."
                : "캐릭터를 미리 볼 수 있어요. 현재 연결에서는 저장할 수 없어요.", this::dispose), BorderLayout.NORTH);
        JPanel choices = panel(new BorderLayout(20, 0));
        JComponent preview = new JComponent() {
            @Override
            protected void paintComponent(Graphics source) {
                var g = graphics(source);
                int center = getWidth() / 2;
                g.setPaint(new RadialGradientPaint(center, getHeight() * .4f, 156,
                        new float[] {0, 1}, new Color[] {color("faf7ff"), color("e8e0f8")}));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                CharacterArt.paint(g, center - 58, (getHeight() - 218) / 2, 1.6, selected);
                centered(g, CharacterChoice.at(selected).label(), center, getHeight() - 49, 14, PURPLE);
                centered(g, "선택한 캐릭터 미리보기", center, getHeight() - 26, 10, color("736b80"));
                g.dispose();
            }
        };
        preview.setPreferredSize(new Dimension(190, 265));
        choices.add(preview, BorderLayout.WEST);
        JPanel grid = panel(new GridLayout(2, 3, 8, 8));
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < CharacterChoice.count(); i++) {
            int n = i;
            ChoiceCard choice = new ChoiceCard(i);
            group.add(choice);
            choice.setSelected(i == selected);
            choice.addActionListener(e -> {
                selected = n;
                preview.repaint();
            });
            grid.add(choice);
        }
        choices.add(grid);
        body.add(choices, BorderLayout.CENTER);
        JPanel actions = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(ModalStyle.action("취소", false, this::dispose));
        JButton commit = ModalStyle.action("변경 저장", true, () -> {
            save.accept(selected);
            dispose();
        });
        commit.setEnabled(editable);
        actions.add(commit);
        body.add(actions, BorderLayout.SOUTH);
        setContentPane(body);
        setSize(640, 430);
        ModalStyle.install(this);
    }

    private static final class ChoiceCard extends JToggleButton {
        ChoiceCard(int index) {
            super(CharacterChoice.at(index).label());
            setUI(new BasicToggleButtonUI());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setVerticalTextPosition(SwingConstants.BOTTOM);
            setHorizontalTextPosition(SwingConstants.CENTER);
            setFont(font(11, true));
            setIconTextGap(6);
            setIcon(new Icon() {
                public int getIconWidth() { return 52; }
                public int getIconHeight() { return 69; }
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    CharacterArt.paint((Graphics2D) g, x, y, .72, index);
                }
            });
            setForeground(color("625b70"));
            addItemListener(event -> setForeground(isSelected() ? PURPLE : color("625b70")));
        }

        @Override
        protected void paintComponent(Graphics source) {
            Graphics2D g = graphics(source);
            int w = getWidth();
            int h = getHeight();
            g.setColor(isSelected() ? SOFT : color(getModel().isRollover() ? "f7f2ff" : "fbfafd"));
            g.fillRoundRect(1, 1, w - 2, h - 2, 24, 24);
            g.setColor(isSelected() || hasFocus() ? color("9779e7") : LINE);
            g.drawRoundRect(1, 1, w - 3, h - 3, 24, 24);
            if (hasFocus()) {
                g.drawRoundRect(4, 4, w - 9, h - 9, 20, 20);
            }
            if (isSelected()) {
                g.setColor(PURPLE);
                g.fillOval(w - 26, 8, 18, 18);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(w - 22, 17, w - 19, 20);
                g.drawLine(w - 19, 20, w - 13, 14);
            }
            g.dispose();
            super.paintComponent(source);
        }
    }
}
