package bubbling.client.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;

public class ClientMessageDispatcher {

    private final JsonCodec jsonCodec = new JsonCodec();

    public void dispatch(MessageEnvelope message) {
        switch (message.type()) {
            case PONG -> {
                String content = jsonCodec.fromPayload(message.payload(), String.class);
                System.out.println("[PONG] " + content);
            }

            case TEST_BROADCAST -> {
                String content = jsonCodec.fromPayload(message.payload(), String.class);
                System.out.println("[BROADCAST] " + content);
            }

            case ERROR -> {
                String content = jsonCodec.fromPayload(message.payload(), String.class);
                System.err.println("[ERROR] " + content);
            }

            default -> {
                System.out.println("처리되지 않은 메시지: " + message.type());
            }
        }
    }
}
