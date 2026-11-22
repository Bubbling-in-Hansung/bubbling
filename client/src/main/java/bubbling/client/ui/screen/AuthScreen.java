package bubbling.client.ui.screen;

import com.formdev.flatlaf.ui.FlatPasswordFieldUI;

import javax.swing.*;
import javax.swing.text.Element;
import javax.swing.text.PasswordView;
import javax.swing.text.View;
import java.awt.*;
import java.util.Arrays;
import static bubbling.client.ui.style.Styles.*;

public final class AuthScreen extends JPanel {
    @FunctionalInterface
    public interface Submit {
        void accept(String loginId, String password, String nickname);
    }

    private final JPanel card = card(Color.WHITE, 52);
    private final JTextField loginId = input("아이디를 입력해주세요");
    private final JLabel status = label(" ", 10, PURPLE);
    private final JButton submit;

    public AuthScreen(boolean signup, Runnable switchMode, Submit action) {
        setLayout(null);
        card.setLayout(new GridBagLayout());
        add(card);
        JPanel form = column();
        append(form, label("WELCOME TO BUBBLING", 9, PURPLE), 9);
        append(form, title(signup ? "회원가입" : "다시 만나서 반가워요", 24), 7);
        append(form, label(signup ? "계정 닉네임은 채팅방 랜덤 닉네임과 별개예요." : "당신을 기다리는 마음들이 있어요.", 10, color("918b9a")), 24);
        if (signup) loginId.putClientProperty("JTextField.placeholderText", "아이디 8~32자 · 영문으로 시작");
        field(form, "아이디", loginId);
        JPasswordField password = passwordField(signup ? "8~64자 · 영문, 숫자, 특수문자 포함" : "비밀번호를 입력해주세요");
        if (signup) password.setToolTipText("허용 특수문자: !@#$%^&*");
        field(form, "비밀번호", password);
        JPasswordField confirmation = signup ? passwordField("비밀번호를 한 번 더 입력해주세요") : null;
        JTextField nickname = input("사용할 닉네임을 입력해주세요");
        if (signup) {
            field(form, "비밀번호 확인", confirmation);
            field(form, "계정 닉네임", nickname);
        }
        submit = pending(signup ? "가입 완료" : "로그인");
        submit.addActionListener(event -> submit(action, password, confirmation, nickname));
        if (signup) {
            password.addActionListener(event -> confirmation.requestFocusInWindow());
            confirmation.addActionListener(event -> submit.doClick());
            nickname.addActionListener(event -> submit.doClick());
        } else {
            password.addActionListener(event -> submit.doClick());
        }
        append(form, status, 8);
        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        append(form, submit, 16);
        append(form, button(signup ? "이미 계정이 있나요?  로그인" : "아직 계정이 없나요?  회원가입", false, switchMode), 8);
        form.setPreferredSize(new Dimension(330, Math.max(signup ? 440 : 380, form.getPreferredSize().height)));
        card.add(form);
    }

    private static JPasswordField passwordField(String hint) {
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
        password.setFont(font(10, false));
        password.setPreferredSize(new Dimension(200, 42));
        password.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        password.putClientProperty("JTextField.placeholderText", hint);
        return password;
    }

    private void submit(Submit action, JPasswordField password, JPasswordField confirmation, JTextField nickname) {
        char[] entered = password.getPassword();
        char[] repeated = confirmation == null ? null : confirmation.getPassword();
        try {
            if (confirmation != null && (repeated.length == 0 || !Arrays.equals(entered, repeated))) {
                status.setText(repeated.length == 0 ? "비밀번호 확인을 입력해 주세요." : "비밀번호가 일치하지 않습니다.");
                confirmation.requestFocusInWindow();
                confirmation.selectAll();
                return;
            }
            action.accept(loginId.getText(), new String(entered), nickname.getText());
            password.setText("");
            if (confirmation != null) confirmation.setText("");
        } finally {
            Arrays.fill(entered, '\0');
            if (repeated != null) Arrays.fill(repeated, '\0');
        }
    }

    public void authentication(boolean enabled, String message) {
        submit.setEnabled(enabled);
        submit.setToolTipText(null);
        status.setText(message.isEmpty() ? " " : message);
    }

    private void field(JPanel p, String name, JTextField input) {
        JLabel l = label(name, 9, INK);
        l.setLabelFor(input);
        append(p, l, 6);
        append(p, input, 12);
    }

    public void doLayout() {
        int split = (int) (getWidth() * .525);
        card.setBounds(split, 22, getWidth() - split - 22, getHeight() - 44);
    }

    protected void paintComponent(Graphics source) {
        Graphics2D g = graphics(source);
        int w = getWidth(), h = getHeight();
        g.setPaint(new GradientPaint(0, 0, color("7650e3"), w, h, color("9c78f1")));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 255, 255, 20));
        g.fillOval(- 180, - 240, 550, 550);
        g.fillOval(w / 3, h - 150, 450, 450);
        int x = Math.max(70, (int) (w * .09)), y = h / 2 - 160;
        logo(g, x, y, Color.WHITE, 27);
        text(g, "말하지 못한 마음을", x, y + 125, 29, Color.WHITE, false);
        text(g, "가볍게 띄워보세요", x, y + 168, 29, Color.WHITE, true);
        text(g, "누군가의 마음에 닿는 순간,", x, y + 218, 12, new Color(255, 255, 255, 180), false);
        text(g, "당신의 고민은 조금 더 가벼워질 거예요.", x, y + 242, 12, new Color(255, 255, 255, 180), false);
        g.setColor(new Color(255, 255, 255, 65));
        g.setStroke(new BasicStroke(1));
        g.drawOval((int) (w * .525) - 170, 120, 110, 110);
        g.drawOval(75, h - 125, 55, 55);
        g.drawOval((int) (w * .525) - 170, h - 118, 28, 28);
        g.dispose();
    }
}
