package bubbling.client.ui.drawing;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.List;
import bubbling.common.dto.chat.ChatParticipantDto;
import static bubbling.client.ui.style.Styles.*;

/** Native Java2D translation of world-sky/world-floor and CSS furniture. */
public final class RoomScene extends JComponent {
    private static final BufferedImage BACKGROUND = loadBackground();
    private final boolean preview;
    private List<ChatParticipantDto> participants = List.of();
    private String myMemberId;
    private java.util.function.IntSupplier character = () -> 0;
    public void setCharacter(java.util.function.IntSupplier value) {
        character = value;
    }

    public RoomScene(boolean preview) {
        this.preview = preview;
    }

    public void showParticipants(List<ChatParticipantDto> participants, String myMemberId) {
        this.participants = participants;
        this.myMemberId = myMemberId;
        repaint();
    }

    public Point participantHead(int participant) {
        int w = getWidth(), h = getHeight(), floor = (int) (h * .36), roomHeight = h - floor;
        return switch (participant) {
            case 0 -> new Point((int) (w * .27), floor + (int) (roomHeight * .54) - 40);
            case 1 -> new Point((int) (w * .70), floor + (int) (roomHeight * .49) - 40);
            case 2 -> new Point((int) (w * (participants.size() > 3 ? .43 : .49)), floor + (int) (roomHeight * .23) - 60);
            case 3 -> new Point((int) (w * .60), floor + (int) (roomHeight * .23) - 60);
            default -> throw new IllegalArgumentException("Unknown participant: " + participant);
        };
    }

    protected void paintComponent(Graphics source) {
        Graphics2D g = graphics(source);
        int w = getWidth(), h = getHeight();
        if (BACKGROUND != null) {
            double scale = Math.max((double) w / BACKGROUND.getWidth(), (double) h / BACKGROUND.getHeight());
            int drawW = (int) Math.ceil(BACKGROUND.getWidth() * scale), drawH = (int) Math.ceil(BACKGROUND.getHeight() * scale);
            g.drawImage(BACKGROUND, (w - drawW) / 2, (h - drawH) / 2, drawW, drawH, null);
        } else {
            g.setColor(color("e6f4f7"));
            g.fillRect(0, 0, w, h);
        }
        if (preview) {
            Point self = participantHead(0), other = participantHead(1), guest = participantHead(2);
            person(g, self.x, self.y, character.getAsInt(), "익명의 고래 · 나");
            person(g, other.x, other.y, 1, "달빛 구름");
            person(g, guest.x, guest.y, 3, "작은 여우");
        } else if (participants.isEmpty()) {
            centered(g, "대화에 참여하면 캐릭터가 표시돼요", w / 2, h - 25, 11, color("5e5871"));
        } else {
            for (int i = 0; i < Math.min(4, participants.size()); i++) {
                ChatParticipantDto participant = participants.get(i);
                Point head = participantHead(i);
                String name = participant.nickname();
                if (participant.memberId().equals(myMemberId)) name += " · 나";
                person(g, head.x, head.y, character.getAsInt(), name);
            }
        }
        g.dispose();
    }

    private static BufferedImage loadBackground() {
        try (var stream = RoomScene.class.getResourceAsStream("/background/chatBackground.png")) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException e) {
            return null;
        }
    }

    private void person(Graphics2D g, int x, int y, int index, String name) {
        CharacterArt.paint(g, x - 36, y, 1, index);
        g.setColor(new Color(255, 255, 255, 220));
        g.fillRoundRect(x - 48, y + 98, 96, 22, 12, 12);
        centered(g, name, x, y + 113, 8, INK);
    }
}
