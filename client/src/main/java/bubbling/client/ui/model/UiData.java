package bubbling.client.ui.model;

import java.util.List;
import java.time.Clock;
import java.util.function.BiConsumer;
import bubbling.common.protocol.MessageType;

public final class UiData {
    private final UiSession session;
    public UiData(boolean preview, List<RoomView> rooms) {
        session = new UiSession(preview, rooms);
    }

    public UiData(BiConsumer<MessageType, Object> send) {
        session = new UiSession(false, List.of(), Clock.systemDefaultZone(), send);
    }

    public boolean preview() {
        return session.preview();
    }

    public UiSession session() {
        return session;
    }

    public static UiData empty() {
        return new UiData(false, List.of());
    }
}
