package bubbling.client.ui.drawing;

import bubbling.client.ui.model.CharacterChoice;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;

/** Classpath images cached once; presentation order is independent of wire values. */
public final class CharacterArt {
    public static final int BASE_WIDTH = 72;
    public static final int BASE_HEIGHT = 95;
    private static final Color FALLBACK_OVAL = new Color(230, 222, 247);
    private static final EnumMap<CharacterChoice, BufferedImage> IMAGES = new EnumMap<>(CharacterChoice.class);
    static {
        for (var value : CharacterChoice.values()) {
            try (var input = CharacterArt.class.getResourceAsStream(value.image())) {
                if (input != null) {
                    IMAGES.put(value, ImageIO.read(input));
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private CharacterArt() {
    }

    public static void paint(Graphics2D source, int x, int y, double scale, int index) {
        var image = IMAGES.get(CharacterChoice.at(index));
        var g = (Graphics2D) source.create();
        try {
            if (image == null) {
                g.setColor(FALLBACK_OVAL);
                g.fillOval(x, y, (int) (BASE_WIDTH * scale), (int) (BASE_HEIGHT * scale));
                return;
            }
            double fit = Math.min((double) BASE_WIDTH / image.getWidth(), (double) BASE_HEIGHT / image.getHeight()) * scale;
            int w = (int) Math.round(image.getWidth() * fit), h = (int) Math.round(image.getHeight() * fit);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(image, x + ((int) (BASE_WIDTH * scale) - w) / 2, y + (int) (BASE_HEIGHT * scale) - h, w, h, null);
        } finally {
            g.dispose();
        }
    }
}
