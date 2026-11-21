package bubbling.client.ui.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageTextTest {
    @Test void countsVisibleEmojiAndCombiningCharactersWithoutSplittingThem() {
        String family = "👨‍👩‍👧‍👦";
        assertEquals(1, MessageText.characters(family).size());
        assertEquals(1, MessageText.characters("e\u0301").size());
        assertTrue(MessageText.acceptsInput(family.repeat(42)));
        assertFalse(MessageText.acceptsInput(family.repeat(43)));
        var lines = MessageText.lines(family.repeat(42), value -> MessageText.characters(value).size(), 14);
        assertEquals(3, lines.size());
        assertTrue(lines.stream().allMatch(line -> MessageText.characters(line).size() == 14));
    }

    @Test void preservesSpacesAndOriginalLineBreaksButRejectsNewMultilineInput() {
        String original = "  가 나  ";
        assertEquals(original, String.join("", MessageText.lines(original, String::length, 100)));
        assertEquals(java.util.List.of("가", "나"), MessageText.lines("가\r\n나", String::length, 100));
        assertFalse(MessageText.acceptsInput("가\n나"));
        assertTrue(MessageText.acceptsInput(" ".repeat(42)));
        assertFalse(MessageText.acceptsInput(" ".repeat(43)));
    }
}
