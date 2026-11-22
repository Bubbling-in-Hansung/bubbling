package bubbling.client.ui.dialog;

import bubbling.client.ui.model.*;
import bubbling.common.enums.BubbleCategory;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import static bubbling.client.ui.style.Styles.*;

public final class RoomEditor extends JDialog {
    private static final int MAX_TITLE_LENGTH = 50;
    private static final int DIALOG_WIDTH = 760;
    private static final int DIALOG_HEIGHT = 650;
    private static final int DEFAULT_MINUTES = 30;
    private static final List<Integer> EXPIRY_MINUTES = List.of(10, 20, 30, 60);
    private static final List<Integer> CAPACITIES = List.of(2, 3, 4);

    public RoomEditor(Window owner, RoomView initial, UiSession session, Runnable changed) {
        super(owner, initial == null ? "새 비눗방울 띄우기" : "비눗방울 수정", ModalityType.APPLICATION_MODAL);
        ModalStyle.prepare(this);
        JPanel card = ModalStyle.surface(18);
        card.add(ModalStyle.header(getTitle(), null, this::dispose), BorderLayout.NORTH);

        JPanel form = column();
        JTextField name = input("어떤 고민을 나누고 싶나요?");
        name.setText(initial == null ? "" : initial.title());
        styleInput(name);
        name.setToolTipText("고민 제목 · 최대 " + MAX_TITLE_LENGTH + "자");
        append(form, field("고민 제목", name), 14);

        JComboBox<String> category = new JComboBox<>(Arrays.stream(BubbleCategory.values())
                .map(BubbleCategory::getLabel).toArray(String[]::new));
        if (initial != null) {
            category.setSelectedItem(initial.category());
        }
        styleInput(category);
        append(form, field("카테고리", category), 15);

        ConversationMode initialMode = Arrays.stream(ConversationMode.values())
                .filter(value -> initial != null && value.label().equals(initial.mode()))
                .findFirst().orElse(ConversationMode.TOGETHER);
        List<OptionGroup.Choice<ConversationMode>> modeChoices = Arrays.stream(ConversationMode.values())
                .map(value -> new OptionGroup.Choice<>(value, value.label(), value.description(),
                        icon(value == ConversationMode.LISTEN ? "user" : "chat")))
                .toList();
        OptionGroup<ConversationMode> mode = new OptionGroup<>(modeChoices, initialMode, 69, 8);
        append(form, field("대화 방식", mode), 15);

        boolean editing = initial != null;
        OptionGroup<Integer> minutes = new OptionGroup<>(EXPIRY_MINUTES.stream()
                .map(value -> new OptionGroup.Choice<>(value, value % 60 == 0 ? value / 60 + "시간" : value + "분"))
                .toList(), editing ? null : DEFAULT_MINUTES, 34, 5);
        OptionGroup<Integer> count = new OptionGroup<>(CAPACITIES.stream()
                .map(value -> new OptionGroup.Choice<>(value, value + "명"))
                .toList(), initial == null ? 4 : initial.capacity(), 34, 5);
        JPanel columns = panel(new GridLayout(1, 2, 14, 0));
        columns.add(field(editing ? "만료 시간 · 수정 시점부터" : "만료 시간", minutes));
        columns.add(field("최대 참여 인원 · 방장 포함", count));
        columns.setMaximumSize(new Dimension(Integer.MAX_VALUE, columns.getPreferredSize().height));
        append(form, columns, 15);

        OptionGroup<Boolean> privacy = new OptionGroup<>(List.of(
                new OptionGroup.Choice<>(false, "공개방", "누구나 자유롭게 대화에 참여할 수 있어요.", icon("users")),
                new OptionGroup.Choice<>(true, "비밀방", "방장의 승인을 받은 사람만 참여할 수 있어요.", icon("lock"))),
                initial != null && initial.privateRoom(), 55, 8);
        append(form, field("공개 여부", privacy), 0);
        card.add(ModalStyle.scrollContent(form), BorderLayout.CENTER);

        JPanel actions = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(ModalStyle.action("취소", false, this::dispose));
        var initialRoom = initial == null ? null : session.find(initial);
        JButton save = ModalStyle.action(initial == null ? "비눗방울 띄우기" : "수정 완료", true,
                () -> UiErrors.perform(this, () -> {
                    String value = name.getText().strip();
                    if (value.isEmpty() || value.length() > MAX_TITLE_LENGTH) {
                        throw new IllegalArgumentException("제목은 1~" + MAX_TITLE_LENGTH + "자로 입력해주세요.");
                    }
                    if (minutes.value() == null) {
                        throw new IllegalArgumentException("수정 시점부터 적용할 만료 시간을 선택해 주세요.");
                    }
                    var old = initialRoom;
                    if (editing && (old == null || old.removed)) {
                        throw new IllegalStateException("수정할 비눗방울이 더 이상 표시되지 않아요.");
                    }
                    int people = old == null ? 1 : old.people;
                    if (count.value() < people) {
                        throw new IllegalArgumentException("현재 참여 인원보다 정원을 줄일 수 없어요.");
                    }
                    var view = new RoomView(value, (String) category.getSelectedItem(), mode.value().label(), people,
                            count.value(), privacy.value(), false, true, initial == null ? 0 : initial.tint(),
                            initial == null ? .35 : initial.x(), initial == null ? .3 : initial.y(),
                            initial == null ? 140 : initial.size());
                    if (old == null) {
                        session.create(view, minutes.value());
                    } else {
                        session.update(old, view, minutes.value());
                    }
                    changed.run();
                    dispose();
                }));
        actions.add(save);
        card.add(actions, BorderLayout.SOUTH);
        setContentPane(card);
        fitToScreen();
        ModalStyle.install(this);
    }

    private static JPanel field(String caption, JComponent input) {
        JPanel field = column();
        JLabel name = label(caption, 10, color("575164"));
        name.setFont(font(10, true));
        name.setLabelFor(input);
        append(field, name, 7);
        append(field, input, 0);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        return field;
    }

    private static void styleInput(JComponent component) {
        component.setFont(font(10, false));
        component.setForeground(color("484252"));
        component.setBackground(color("fbfafd"));
        component.putClientProperty("FlatLaf.style", "arc: 10; borderColor: #e7e2ef");
        component.setPreferredSize(new Dimension(0, 40));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        component.setMinimumSize(new Dimension(0, 40));
    }

    private void fitToScreen() {
        GraphicsConfiguration configuration = getGraphicsConfiguration();
        Rectangle bounds = configuration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        setSize(Math.min(DIALOG_WIDTH, bounds.width - insets.left - insets.right - 40),
                Math.min(DIALOG_HEIGHT, bounds.height - insets.top - insets.bottom - 40));
    }
}
