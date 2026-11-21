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
}
