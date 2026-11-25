package bubbling.client.ui.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Display/input limits count grapheme clusters, not UTF-16 code units. */
public final class MessageText {
    public static final int COLUMNS = 14;
    public static final int MAX_LINES = 3;
    public static final int MAX_LENGTH = COLUMNS * MAX_LINES;
    private static final Pattern CHARACTER = Pattern.compile("\\X");
    private MessageText() {
    }

    public static List<String> characters(String text) {
        return CHARACTER.matcher(text).results().map(match -> match.group()).toList();
    }

    public static boolean acceptsInput(String text) {
        return !text.contains("\n") && !text.contains("\r") && characters(text).size() <= MAX_LENGTH;
    }

    /** Preserves whitespace and old explicit line breaks; never mutates stored text. */
    public static List<String> lines(String text, java.util.function.ToIntFunction<String> width, int maxWidth) {
        List<String> result = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        int count = 0;
        for (String character : characters(text)) {
            if (character.contains("\n") || character.contains("\r")) {
                result.add(line.toString());
                line.setLength(0);
                count = 0;
                continue;
            }
            if (count > 0 && (count == COLUMNS || width.applyAsInt(line.toString() + character) > maxWidth)) {
                result.add(line.toString());
                line.setLength(0);
                count = 0;
            }
            line.append(character);
            count++;
        }
        result.add(line.toString());
        return List.copyOf(result);
    }

    /** Wraps grapheme code points to fit pixel width and clamps overflow with an ellipsis. */
    public static List<String> wrapWithEllipsis(String text, java.util.function.ToIntFunction<String> width,
            int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int codePoint : text.codePoints().toArray()) {
            String next = Character.toString(codePoint);
            if (!line.isEmpty() && width.applyAsInt(line + next) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(next);
        }
        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line.toString());
        }
        int limit = Math.max(1, maxLines);
        if (lines.size() > limit) {
            lines.subList(limit, lines.size()).clear();
            String last = lines.getLast();
            while (!last.isEmpty() && width.applyAsInt(last + "…") > maxWidth) {
                last = last.substring(0, last.offsetByCodePoints(last.length(), -1));
            }
            lines.set(limit - 1, last + "…");
        }
        return List.copyOf(lines);
    }
}
