package bubbling.server.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientConnection implements Runnable {

    private final String id = UUID.randomUUID().toString();

    private final Socket socket;
    private final ClientRegistry clientRegistry;
    private final MessageDispatcher messageDispatcher;

    private final BufferedReader reader;
    private final BufferedWriter writer;

    private final JsonCodec jsonCodec = new JsonCodec();

    private final Object writeLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ClientConnection(
            Socket socket,
            ClientRegistry clientRegistry,
            MessageDispatcher messageDispatcher
    ) throws IOException {
        this.socket = socket;
        this.clientRegistry = clientRegistry;
        this.messageDispatcher = messageDispatcher;

        this.reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
        );

        this.writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
        );
    }

    @Override
    public void run() {
        try {
            String line;
            while (!closed.get() && (line = reader.readLine()) != null) {
                MessageEnvelope message = jsonCodec.decode(line);

                if (message == null || message.type() == null) {
                    throw new IllegalArgumentException("잘못된 메시지 형식입니다.");
                }

                messageDispatcher.dispatch(this, message);
            }
        } catch (IOException | IllegalArgumentException e) {
            if (!closed.get()) {
                System.err.println("클라이언트 통신 오류 [" + id + "]: " + e.getMessage());
            }
        } finally {
            close();
        }
    }

    void send(MessageEnvelope message) {
        if (closed.get()) {
            return;
        }

        synchronized (writeLock) {
            if (closed.get()) {
                return;
            }

            try {
                writer.write(jsonCodec.encode(message));
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                System.err.println("메시지 전송 실패 [" + id + "]: " + e.getMessage());
                close();
            }
        }
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        try {
            socket.close();
        } catch (IOException ignored) {

        }

        clientRegistry.unregister(this);
    }

    public String getId() {
        return id;
    }

    public boolean isClosed() {
        return closed.get();
    }
}
