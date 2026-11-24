package bubbling.client.ui.dialog;

import bubbling.client.ui.model.UiSession;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

import static bubbling.client.ui.style.Styles.*;

public final class PasswordChangeDialog extends JDialog {
    private static final String PASSWORD_FORMAT =
            "(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]{8,64}";
    private static final Color FIELD_LABEL = color("575164"), INPUT_INK = color("484252");

    public PasswordChangeDialog(Window owner, boolean editable, UiSession session) {
        super(owner, "비밀번호 변경", ModalityType.APPLICATION_MODAL);
        ModalStyle.prepare(this);
        JPanel body = ModalStyle.surface(18);
        body.add(ModalStyle.header(getTitle(), editable
                ? "현재 비밀번호를 확인한 뒤 새 비밀번호로 변경할 수 있어요."
                : "로그인 후 비밀번호를 변경할 수 있어요.", this::dispose), BorderLayout.NORTH);

        JPanel form = column();
        JPasswordField currentPassword = passwordField("현재 비밀번호를 입력해주세요");
        JPasswordField newPassword = passwordField("8~64자 · 영문, 숫자, 특수문자 포함");
        newPassword.setToolTipText("허용 특수문자: !@#$%^&*");
        JPasswordField confirmPassword = passwordField("새 비밀번호를 한 번 더 입력해주세요");

        append(form, field("현재 비밀번호", currentPassword), 14);
        append(form, field("새 비밀번호", newPassword), 14);
        append(form, field("새 비밀번호 확인", confirmPassword), 0);
        body.add(ModalStyle.scrollContent(form), BorderLayout.CENTER);

        JPanel actions = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(ModalStyle.action("취소", false, this::dispose));
        JButton commit = ModalStyle.action("변경 저장", true,
                () -> submit(session, currentPassword, newPassword, confirmPassword));
        commit.setEnabled(editable);
        actions.add(commit);
        body.add(actions, BorderLayout.SOUTH);

        currentPassword.addActionListener(e -> newPassword.requestFocusInWindow());
        newPassword.addActionListener(e -> confirmPassword.requestFocusInWindow());
        confirmPassword.addActionListener(e -> commit.doClick());

        setContentPane(body);
        setSize(480, 420);
        ModalStyle.install(this);
    }

    private void submit(UiSession session, JPasswordField currentField,
            JPasswordField newField, JPasswordField confirmField) {
        char[] currentChars = currentField.getPassword();
        char[] newChars = newField.getPassword();
        char[] confirmChars = confirmField.getPassword();
        try {
            UiErrors.perform(this, () -> {
                String current = new String(currentChars);
                String next = new String(newChars);
                if (current.isEmpty()) {
                    currentField.requestFocusInWindow();
                    throw new IllegalArgumentException("현재 비밀번호를 입력해 주세요.");
                }
                if (next.isEmpty()) {
                    newField.requestFocusInWindow();
                    throw new IllegalArgumentException("새 비밀번호를 입력해 주세요.");
                }
                if (!next.matches(PASSWORD_FORMAT)) {
                    newField.requestFocusInWindow();
                    newField.selectAll();
                    throw new IllegalArgumentException("비밀번호 8~64자에 영문·숫자·특수문자를 포함해 주세요.");
                }
                if (current.equals(next)) {
                    newField.requestFocusInWindow();
                    newField.selectAll();
                    throw new IllegalArgumentException("현재 비밀번호와 다른 새 비밀번호를 입력해 주세요.");
                }
                if (confirmChars.length == 0 || !Arrays.equals(newChars, confirmChars)) {
                    confirmField.requestFocusInWindow();
                    confirmField.selectAll();
                    throw new IllegalArgumentException(confirmChars.length == 0
                            ? "새 비밀번호 확인을 입력해 주세요."
                            : "새 비밀번호가 일치하지 않습니다.");
                }
                session.changePassword(current, next);
                dispose();
            });
        } finally {
            Arrays.fill(currentChars, '\0');
            Arrays.fill(newChars, '\0');
            Arrays.fill(confirmChars, '\0');
        }
    }

    private static JPanel field(String caption, JComponent input) {
        JPanel field = column();
        JLabel name = label(caption, 10, FIELD_LABEL);
        name.setFont(font(10, true));
        name.setLabelFor(input);
        append(field, name, 7);
        append(field, input, 0);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        return field;
    }

    private static JPasswordField passwordField(String hint) {
        JPasswordField field = passwordInput(hint);
        field.setFont(font(10, false));
        field.setForeground(INPUT_INK);
        field.setBackground(SURFACE);
        field.putClientProperty("FlatLaf.style", "arc: 10; borderColor: #e7e2ef");
        field.setPreferredSize(new Dimension(0, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setMinimumSize(new Dimension(0, 40));
        return field;
    }
}
