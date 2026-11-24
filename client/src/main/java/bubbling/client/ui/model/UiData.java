package bubbling.client.ui.model;

import java.time.Clock;
import java.util.function.BiConsumer;
import bubbling.common.protocol.MessageType;

public final class UiData {
    private final UiSession session;
    public UiData(BiConsumer<MessageType, Object> send) {
        session = new UiSession(Clock.systemDefaultZone(), send);
    }

    public UiSession session() {
        return session;
    }
}
