package bubbling.client.ui.screen;

import bubbling.client.ui.model.*;
import bubbling.client.ui.drawing.*;
import bubbling.client.ui.dialog.CharacterPickerDialog;
import bubbling.common.dto.user.UserDto;
import bubbling.common.enums.ProfileCharacter;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import static bubbling.client.ui.style.Styles.*;

public final class ProfileScreen extends JPanel {
    private final JPanel content = new JPanel() {
        @Override
        public Dimension getMaximumSize() {
            return new Dimension(980, super.getMaximumSize().height);
        }
    };
    private final UiSession session;
    private final Consumer<ProfileCharacter> saveProfile;
    private boolean signedIn;
    private final JLabel spaceTitle;
    private final JLabel accountId = label("", 9, INK);
    private final JLabel accountNickname = label("", 9, INK);
    private final JLabel createdAt = label("", 9, INK);
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
                g.setColor(color("eee7fb"));
                g.fillRoundRect(0, 0, 130, 145, 50, 50);
                CharacterArt.paint(g, 20, 15, 1.25, session.character());
                g.dispose();
            }
        };
        art.setPreferredSize(new Dimension(140, 145));
        hero.add(art, BorderLayout.WEST);
        JPanel copy = column();
        append(copy, label("MY LITTLE SPACE", 9, PURPLE), 9);
        spaceTitle = title("나의 공간", 20);
        append(copy, spaceTitle, 9);
        append(copy, label("나만의 캐릭터와 함께 마음을 나눠요", 9, MUTED), 16);
        append(copy, button("캐릭터 변경", false, () -> characters()), 0);
        hero.add(copy);
        JPanel account = column();
        account.setPreferredSize(new Dimension(230, 140));
        account.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, LINE));
        setAccount(null);
        append(account, accountId, 22);
        append(account, accountNickname, 22);
        append(account, createdAt, 22);
        hero.add(account, BorderLayout.EAST);
        append(content, hero, 27);
        JPanel centered = new JPanel(new BorderLayout()) {
            public void doLayout() {
                int width = Math.min(980, getWidth());
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
        accountId.setText("   계정 아이디     " + (user == null ? "—" : user.loginId()));
        accountNickname.setText("   계정 닉네임     " + (user == null ? "—" : user.nickname()));
        createdAt.setText("   가입일              " + (user == null ? "—" : user.createdAt()));
    }

    private void characters() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        new CharacterPickerDialog(owner, session.character(), signedIn,
                value -> saveProfile.accept(CharacterChoice.at(value).profile())).setVisible(true);
    }
}
