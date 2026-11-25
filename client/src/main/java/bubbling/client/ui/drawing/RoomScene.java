package bubbling.client.ui.drawing;

import bubbling.client.ui.model.CharacterChoice;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.List;
import bubbling.common.dto.chat.ChatParticipantDto;
import static bubbling.client.ui.style.Styles.*;

/** Native Java2D translation of world-sky/world-floor and CSS furniture. */
public final class RoomScene extends JComponent {
    public static final int MAX_VISIBLE_PARTICIPANTS = 4;
    private static final BufferedImage BACKGROUND = loadBackground();
    private static final double CHARACTER_SCALE = 1.3;
    private static final int CHARACTER_WIDTH = (int) Math.ceil(CharacterArt.BASE_WIDTH * CHARACTER_SCALE);
    private static final int CHARACTER_HEIGHT = (int) Math.ceil(CharacterArt.BASE_HEIGHT * CHARACTER_SCALE);
    private static final int NAME_WIDTH = 96;
    private static final int FIGURE_HEIGHT = CHARACTER_HEIGHT + 25;
    private static final Color FALLBACK_BG = color("e6f4f7"), EMPTY_HINT_INK = color("5e5871"),
            NAME_PILL_BG = new Color(255, 255, 255, 220);
    private List<ChatParticipantDto> participants = List.of();
    private final java.util.List<String> drawOrder = new java.util.ArrayList<>();
    private String myMemberId;
    private Point2D.Double ownPosition;
    private java.util.function.IntSupplier character = () -> 0;
    public void setCharacter(java.util.function.IntSupplier value) {
        character = value;
    }

    public void showParticipants(List<ChatParticipantDto> participants, String myMemberId) {
        this.participants = participants;
        this.myMemberId = myMemberId;
        int visibleCount = Math.min(MAX_VISIBLE_PARTICIPANTS, participants.size());
        java.util.List<String> visibleIds = new java.util.ArrayList<>(visibleCount);
        for (int i = 0; i < visibleCount; i++) {
            visibleIds.add(participants.get(i).chatMemberId());
        }
        drawOrder.retainAll(visibleIds);
        for (String id : visibleIds) {
            if (!drawOrder.contains(id)) {
                drawOrder.add(id);
            }
        }
        repaint();
    }

    public void bringToFront(String memberId) {
        if (memberId == null) return;
        if (drawOrder.remove(memberId)) {
            drawOrder.add(memberId);
        }
    }

    public void moveOwnCharacter(Point target) {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        ownPosition = new Point2D.Double((double) target.x / getWidth(), (double) target.y / getHeight());
        bringToFront(myMemberId);
        repaint();
    }

    public void resetOwnPosition() {
        ownPosition = null;
        drawOrder.clear();
        repaint();
    }

    public List<Integer> paintOrder() {
        int visibleCount = Math.min(MAX_VISIBLE_PARTICIPANTS, participants.size());
        java.util.List<Integer> order = new java.util.ArrayList<>(visibleCount);
        for (String id : drawOrder) {
            for (int i = 0; i < visibleCount; i++) {
                if (participants.get(i).chatMemberId().equals(id)) {
                    order.add(i);
                    break;
                }
            }
        }
        for (int i = 0; i < visibleCount; i++) {
            if (!order.contains(i)) {
                order.add(i);
            }
        }
        return java.util.Collections.unmodifiableList(order);
    }

    public Point participantHead(int participant) {
        int w = getWidth(), h = getHeight(), floor = (int) (h * .36), roomHeight = h - floor;
        boolean own = participant < participants.size()
                && participants.get(participant).chatMemberId().equals(myMemberId);
        if (own && ownPosition != null) {
            int margin = Math.min(w / 2, Math.max(CHARACTER_WIDTH, NAME_WIDTH) / 2);
            int x = (int) Math.round(ownPosition.x * w);
            int y = (int) Math.round(ownPosition.y * h) - CHARACTER_HEIGHT / 2;
            return new Point(Math.clamp(x, margin, w - margin), Math.clamp(y, 0, Math.max(0, h - FIGURE_HEIGHT)));
        }
        Point head = switch (participant) {
            case 0 -> new Point((int) (w * .27), floor + (int) (roomHeight * .54) - 40);
            case 1 -> new Point((int) (w * .70), floor + (int) (roomHeight * .49) - 40);
            case 2 -> new Point((int) (w * (participants.size() > 3 ? .43 : .49)), floor + (int) (roomHeight * .23) - 60);
            case 3 -> new Point((int) (w * .60), floor + (int) (roomHeight * .23) - 60);
            default -> throw new IllegalArgumentException("Unknown participant: " + participant);
        };
        head.y = Math.max(0, head.y - (CHARACTER_HEIGHT - CharacterArt.BASE_HEIGHT));
        return head;
    }

    public int participantBottom(Point head) {
        return head.y + FIGURE_HEIGHT;
    }

    protected void paintComponent(Graphics source) {
        Graphics2D g = graphics(source);
        int w = getWidth(), h = getHeight();
        if (BACKGROUND != null) {
            double scale = Math.max((double) w / BACKGROUND.getWidth(), (double) h / BACKGROUND.getHeight());
            int drawW = (int) Math.ceil(BACKGROUND.getWidth() * scale), drawH = (int) Math.ceil(BACKGROUND.getHeight() * scale);
            g.drawImage(BACKGROUND, (w - drawW) / 2, (h - drawH) / 2, drawW, drawH, null);
        } else {
            g.setColor(FALLBACK_BG);
            g.fillRect(0, 0, w, h);
        }
        if (participants.isEmpty()) {
            centered(g, "대화에 참여하면 캐릭터가 표시돼요", w / 2, h - 25, 11, EMPTY_HINT_INK);
        } else {
            for (int i : paintOrder()) {
                ChatParticipantDto participant = participants.get(i);
                Point head = participantHead(i);
                String name = participant.nickname();
                boolean own = participant.chatMemberId().equals(myMemberId);
                if (own) name += " · 나";
                int appearance = own ? character.getAsInt() : CharacterChoice.indexOf(participant.profile());
                person(g, head.x, head.y, appearance, name);
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
        CharacterArt.paint(g, x - CHARACTER_WIDTH / 2, y, CHARACTER_SCALE, index);
        int labelY = y + CHARACTER_HEIGHT + 3;
        g.setColor(NAME_PILL_BG);
        g.fillRoundRect(x - NAME_WIDTH / 2, labelY, NAME_WIDTH, 22, 12, 12);
        centered(g, name, x, labelY + 15, 8, INK);
    }
}
