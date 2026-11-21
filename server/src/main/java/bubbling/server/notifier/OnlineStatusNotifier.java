package bubbling.server.notifier;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.system.OnlineStatusDto;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.network.ClientRegistry;
import bubbling.server.network.MessageSender;

public class OnlineStatusNotifier {

    private final ClientRegistry clientRegistry;
    private final MessageSender messageSender;

    private final JsonCodec jsonCodec = new JsonCodec();

    public OnlineStatusNotifier(ClientRegistry clientRegistry, MessageSender messageSender) {
        this.clientRegistry = clientRegistry;
        this.messageSender = messageSender;
    }

    public void notifyChanged() {
        OnlineStatusDto payload = new OnlineStatusDto(clientRegistry.getOnlineCount());

        MessageEnvelope event = new MessageEnvelope(
                MessageType.ONLINE_STATUS,
                null,
                jsonCodec.toPayload(payload)
        );

        messageSender.broadcast(event);
    }
}
