package bubbling.client.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MessageSender와 MessageReceiver의 큐 기반 송신 및 수신 파싱 복구 동작을 검증한다.
 */
class MessageSenderAndReceiverTest {

    private final JsonCodec jsonCodec = new JsonCodec();

    @Test
    @DisplayName("MessageSender는 null 전송을 거부하고 큐의 메시지를 순차 전송한다")
    void senderEncodesAndFlushesQueuedMessages() throws Exception {
        PipedWriter pipedWriter = new PipedWriter();
        PipedReader pipedReader = new PipedReader(pipedWriter);
        BufferedWriter writer = new BufferedWriter(pipedWriter);
        BufferedReader reader = new BufferedReader(pipedReader);

        MessageSender sender = new MessageSender(writer);
        assertThrows(NullPointerException.class, () -> sender.send(null));

        Thread senderThread = new Thread(sender, "test-sender");
        senderThread.start();

        MessageEnvelope envelope = new MessageEnvelope(MessageType.PING, "req-1", jsonCodec.toPayload("ping"));
        sender.send(envelope);

        String line = reader.readLine();
        assertNotNull(line);
        MessageEnvelope decoded = jsonCodec.decode(line);
        assertEquals(envelope, decoded);

        senderThread.interrupt();
        senderThread.join(2000);
        assertFalse(senderThread.isAlive());
    }

    @Test
    @DisplayName("MessageSender는 IOException 발생 시 송신 스레드를 안전하게 종료한다")
    void senderStopsCleanlyOnIoException() throws Exception {
        CountDownLatch writeAttempted = new CountDownLatch(1);
        Writer failingWriter = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                writeAttempted.countDown();
                throw new IOException("broken pipe");
            }

            @Override
            public void flush() throws IOException {
                throw new IOException("broken pipe");
            }

            @Override
            public void close() {
            }
        };

        MessageSender sender = new MessageSender(new BufferedWriter(failingWriter));
        Thread senderThread = new Thread(sender, "test-failing-sender");
        senderThread.start();

        sender.send(new MessageEnvelope(MessageType.PING, "req-err", jsonCodec.toPayload("ping")));

        assertTrue(writeAttempted.await(2, TimeUnit.SECONDS));
        senderThread.join(2000);
        assertFalse(senderThread.isAlive());
    }

    @Test
    @DisplayName("MessageReceiver는 잘못된 JSON 라인을 건너뛰고 다음 정상 메시지를 디스패치한다")
    void receiverSkipsMalformedLinesAndDispatchesValidMessage() {
        MessageEnvelope validEnvelope = new MessageEnvelope(MessageType.PONG, "req-2", jsonCodec.toPayload("pong"));
        String payloadStream = String.join("\n",
                "{invalid-json",
                "{\"requestId\":\"no-type\"}",
                jsonCodec.encode(validEnvelope)
        ) + "\n";

        BlockingQueue<MessageEnvelope> dispatched = new LinkedBlockingQueue<>();
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher() {
            @Override
            public void dispatch(MessageEnvelope message) {
                dispatched.offer(message);
            }
        };

        MessageReceiver receiver = new MessageReceiver(
                new BufferedReader(new StringReader(payloadStream)),
                dispatcher
        );

        receiver.run();

        List<MessageEnvelope> received = new ArrayList<>();
        dispatched.drainTo(received);
        assertEquals(1, received.size());
        assertEquals(validEnvelope, received.get(0));
    }
}
