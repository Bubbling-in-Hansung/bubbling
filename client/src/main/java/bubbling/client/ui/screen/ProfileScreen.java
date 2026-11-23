package bubbling.client.ui.screen;

import bubbling.client.ui.model.*;
import bubbling.client.ui.drawing.*;
import bubbling.client.ui.dialog.CharacterPickerDialog;
import bubbling.common.dto.user.UserDto;
import bubbling.common.enums.ProfileCharacter;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;
import static bubbling.client.ui.style.Styles.*;

public final class ProfileScreen extends JPanel {
    private static final int MAX_CONTENT_WIDTH = 980;
    private static final Color ART_BG = color("eee7fb");
    private final JPanel content = new JPanel() {
        @Override
        public Dimension getMaximumSize() {
            return new Dimension(MAX_CONTENT_WIDTH, super.getMaximumSize().height);
        }
    };
    private final UiSession session;
    private final Consumer<ProfileCharacter> saveProfile;
    private boolean signedIn;
    private final JLabel spaceTitle;
    private final JLabel accountId = title("", 12);
    private final JLabel accountNickname = title("", 12);
    private final JLabel createdAt = title("", 12);
    public ProfileScreen(UiData data, Consumer<ProfileCharacter> saveProfile) {
        super(new BorderLayout());
        this.saveProfile = saveProfile;
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        session = data.session();
        setBackground(BG);
        pad(content, 32);
        JPanel hero = card(Color.WHITE, 44);
        hero.setLayout(new BorderLayout(25, 0));
        pad(hero, 25);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        hero.setPreferredSize(new Dimension(900, 210));
        JComponent art = new JComponent() {
            protected void paintComponent(Graphics s) {
                Graphics2D g = graphics(s);
                g.setColor(ART_BG);
                g.fillRoundRect(0, 0, 130, 145, 50, 50);
                CharacterArt.paint(g, 20, 15, 1.25, session.character());
                g.dispose();
            }
        };
        art.setPreferredSize(new Dimension(140, 145));
        hero.add(art, BorderLayout.WEST);
        JPanel copy = column();
        append(copy, label("MY LITTLE SPACE", 11, PURPLE), 9);
        spaceTitle = title("나의 공간", 24);
        append(copy, spaceTitle, 9);
        append(copy, label("나만의 캐릭터와 함께 마음을 나눠요", 12, MUTED), 16);
        append(copy, button("캐릭터 변경", false, () -> characters()), 0);
        hero.add(copy);
        JPanel account = column();
        account.setPreferredSize(new Dimension(260, 140));
        account.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, LINE),
                BorderFactory.createEmptyBorder(12, 20, 0, 0)));
        setAccount(null);
        append(account, accountRow("계정 아이디", accountId), 16);
        append(account, accountRow("계정 닉네임", accountNickname), 16);
        append(account, accountRow("가입일", createdAt), 0);
        hero.add(account, BorderLayout.EAST);
        append(content, hero, 27);
        JPanel centered = new JPanel(new BorderLayout()) {
            public void doLayout() {
                int width = Math.min(MAX_CONTENT_WIDTH, getWidth());
                content.setBounds((getWidth() - width) / 2, 0, width, Math.max(content.getPreferredSize().height, getHeight()));
            }
        };
        centered.setOpaque(false);
        centered.add(content);
        add(scroll(centered));
    }

    public void setAccount(UserDto user) {
        signedIn = user != null;
        spaceTitle.setText(user == null ? "나의 공간" : user.nickname() + "님의 공간");
        accountId.setText(user == null ? "—" : user.loginId());
        accountNickname.setText(user == null ? "—" : user.nickname());
        accountId.setToolTipText(user == null ? null : user.loginId());
        accountNickname.setToolTipText(user == null ? null : user.nickname());
        createdAt.setText(user == null ? "—" : accountDate(user.createdAt()));
    }

    private static JPanel accountRow(String name, JLabel value) {
        JPanel row = panel(new BorderLayout(12, 0));
        JLabel key = label(name, 11, MUTED);
        key.setPreferredSize(new Dimension(76, key.getPreferredSize().height));
        key.setLabelFor(value);
        row.add(key, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static String accountDate(String value) {
        if (value == null || value.isBlank()) return "—";
        try {
            LocalDate date = value.contains("T") ? LocalDateTime.parse(value).toLocalDate() : LocalDate.parse(value);
            return "%d년 %d월 %d일".formatted(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        } catch (DateTimeParseException e) {
            return value;
        }
    }

    private void characters() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        new CharacterPickerDialog(owner, session.character(), signedIn,
                value -> saveProfile.accept(CharacterChoice.at(value).profile())).setVisible(true);
    }
}
