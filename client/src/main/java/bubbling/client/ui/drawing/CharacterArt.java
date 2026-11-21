package bubbling.client.ui.drawing;

import bubbling.client.ui.model.CharacterChoice;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;

/** Classpath images cached once; presentation order is independent of wire values. */
public final class CharacterArt {
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
                g.setColor(new Color(230, 222, 247));
                g.fillOval(x, y, (int) (72 * scale), (int) (95 * scale));
                return;
            }
            double fit = Math.min(72.0 / image.getWidth(), 95.0 / image.getHeight()) * scale;
            int w = (int) Math.round(image.getWidth() * fit), h = (int) Math.round(image.getHeight() * fit);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(image, x + ((int) (72 * scale) - w) / 2, y + (int) (95 * scale) - h, w, h, null);
        } finally {
            g.dispose();
        }
    }
}
