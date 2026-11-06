package bubbling.client.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;

import java.io.BufferedWriter;
import java.io.IOException;

public class MessageSender {

    private final BufferedWriter writer;
    private final JsonCodec jsonCodec = new JsonCodec();

    private final Object writeLock = new Object();

    public MessageSender(BufferedWriter writer) {
        this.writer = writer;
    }

    public void send(MessageEnvelope message) throws IOException {
        synchronized (writeLock) {
            writer.write(jsonCodec.encode(message));
            writer.newLine();
            writer.flush();
        }
    }
}