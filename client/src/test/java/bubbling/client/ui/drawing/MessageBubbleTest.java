package bubbling.client.ui.drawing;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class MessageBubbleTest {
    @Test void expansionIsLocalAndPreservesTheOriginalThenRestoresCollapsedSize() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String original = "가 나 다 라 마 바 사 ".repeat(12);
            AtomicInteger resized = new AtomicInteger();
            MessageBubble bubble = new MessageBubble(original, true, true, resized::incrementAndGet);
            Dimension collapsed = bubble.getPreferredSize();
            JButton control = (JButton) bubble.getComponent(1);
            JScrollPane content = (JScrollPane) bubble.getComponent(0);
            JTextArea text = (JTextArea) content.getViewport().getView();
            assertEquals(original, text.getText());
            assertEquals("<더보기>", control.getText());
            control.doClick();
            assertEquals("<접기>", control.getText());
            assertTrue(bubble.getPreferredSize().height > collapsed.height);
            assertTrue(bubble.getPreferredSize().width <= MessageBubble.MAX_WIDTH);
            bubble.setAvailableHeight(170);
            bubble.setSize(bubble.getPreferredSize());
            bubble.doLayout();
            assertTrue(text.getPreferredSize().height > content.getHeight());
            control.doClick();
            assertEquals(collapsed, bubble.getPreferredSize());
            assertEquals(original, text.getText());
            assertEquals(2, resized.get());
        });
    }

    @Test void shortMessagesShrinkAndNeedNoExpansionControl() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MessageBubble shortBubble = new MessageBubble("안녕", false, false, () -> {});
            MessageBubble longer = new MessageBubble("가".repeat(42), false, true, () -> {});
            assertTrue(shortBubble.getPreferredSize().width < longer.getPreferredSize().width);
            assertFalse(shortBubble.getComponent(1).isVisible());
            assertTrue(longer.getPreferredSize().width <= MessageBubble.MAX_WIDTH);
            assertFalse(longer.getComponent(1).isVisible());
        });
    }
}
