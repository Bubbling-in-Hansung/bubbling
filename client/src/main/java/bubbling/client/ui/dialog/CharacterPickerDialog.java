package bubbling.client.ui.dialog;

import bubbling.client.ui.drawing.CharacterArt;
import bubbling.client.ui.model.CharacterChoice;
import javax.swing.*;
import java.awt.*;
import java.util.function.IntConsumer;
import static bubbling.client.ui.style.Styles.*;

public final class CharacterPickerDialog extends JDialog {
    private int selected;
    public CharacterPickerDialog(Window owner, int initial, boolean editable, IntConsumer save) {
        super(owner, "프로필 캐릭터 선택", ModalityType.APPLICATION_MODAL);
        selected = initial;
        JPanel body = column();
        pad(body, 24);
        append(body, title("프로필 캐릭터 선택", 20), 20);
        JPanel choices = panel(new BorderLayout(20, 0));
        JComponent preview = new JComponent() {
            protected void paintComponent(Graphics source) {
                var g = graphics(source);
                g.setColor(SOFT);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                CharacterArt.paint(g, 35, 35, 1.6, selected);
                centered(g, CharacterChoice.at(selected).label(), 95, 230, 13, PURPLE);
                g.dispose();
            }
        };
        preview.setPreferredSize(new Dimension(190, 265));
        choices.add(preview, BorderLayout.WEST);
        JPanel grid = panel(new GridLayout(2, 3, 8, 8));
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < CharacterChoice.count(); i++) {
            int n = i;
            JToggleButton b = new JToggleButton(CharacterChoice.at(i).label());
            b.setSelected(i == selected);
            b.setVerticalTextPosition(SwingConstants.BOTTOM);
            b.setHorizontalTextPosition(SwingConstants.CENTER);
            b.setBackground(SOFT);
            b.setIcon(new Icon() {
                public int getIconWidth() {
                    return 58;
                }
                public int getIconHeight() {
                    return 77;
                }
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    CharacterArt.paint((Graphics2D) g, x, y, .8, n);
                }
            });
            b.addActionListener(e -> {
                selected = n;
                preview.repaint();
            });
            group.add(b);
            grid.add(b);
        }
        choices.add(grid);
        append(body, choices, 18);
        JPanel actions = panel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(button("취소", false, this::dispose));
        JButton commit = button("변경 저장", true, () -> {
            save.accept(selected);
            dispose();
        });
        commit.setEnabled(editable);
        actions.add(commit);
        append(body, actions, 0);
        setContentPane(body);
        setSize(620, 420);
        ModalStyle.install(this);
    }
}
