package bubbling.client.ui.style;

import java.awt.Font;
import java.io.*;

public final class UiFonts {
    private static final float UI_SCALE = 1.2f;
    private static final Font REGULAR = load("/fonts/Noto_Sans_KR/static/NotoSansKR-Regular.ttf", false);
    private static final Font MEDIUM = load("/fonts/Noto_Sans_KR/static/NotoSansKR-Medium.ttf", false);
    private static final Font SEMIBOLD = load("/fonts/Noto_Sans_KR/static/NotoSansKR-SemiBold.ttf", true);
    private static final Font BOLD = load("/fonts/Noto_Sans_KR/static/NotoSansKR-Bold.ttf", true);
    private static final Font LOGO = load("/fonts/Nunito/static/Nunito-ExtraBold.ttf", true);
    private UiFonts() {
    }

    private static Font load(String path, boolean fallbackBold) {
        try (var in = UiFonts.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Missing " + path);
            }
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Exception e) {
            System.err.println("Font fallback: " + e.getMessage());
            return new Font("맑은 고딕", fallbackBold ? Font.BOLD : Font.PLAIN, 12);
        }
    }

    public static Font body(int size, boolean bold) {
        return body(size, bold ? 700 : 400);
    }

    public static Font body(int size, int weight) {
        return bodyAtSize(size * UI_SCALE, weight);
    }

    public static Font bodyAtSize(float size, int weight) {
        Font face = weight >= 700 ? BOLD : weight >= 600 ? SEMIBOLD : weight >= 500 ? MEDIUM : REGULAR;
        // The file supplies the actual outlines for this weight; only the point size changes.
        return face.deriveFont((float) Math.round(size));
    }

    public static Font logo(int size) {
        return LOGO.deriveFont((float) Math.round(size * UI_SCALE));
    }

    public static Font scale(Font font) {
        return font.deriveFont((float) Math.round(font.getSize2D() * UI_SCALE));
    }
}
