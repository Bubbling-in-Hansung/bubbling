package bubbling.server.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.server.domain.User;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientConnection implements Runnable {

    private final String id = UUID.randomUUID().toString();
    private final Socket socket;

    private volatile User user;

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
                new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.UTF_8
                )
        );

        this.writer = new BufferedWriter(
                new OutputStreamWriter(
                        socket.getOutputStream(),
                        StandardCharsets.UTF_8
                )
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

        } catch (IOException e) {

        } catch (IllegalArgumentException e) {

        } catch (RuntimeException e) {

        } finally {
            close();
        }
    }

    void send(MessageEnvelope message) {
        Objects.requireNonNull(message, "메시지는 필수입니다.");

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
                close();
            }
        }
    }

    public synchronized void authenticate(User user) {
        Objects.requireNonNull(user, "사용자는 필수입니다.");

        if (closed.get()) {
            throw new IllegalStateException("이미 종료된 연결입니다.");
        }

        if (this.user != null) {
            throw new IllegalStateException("이미 로그인된 연결입니다.");
        }

        this.user = user;
    }

    public User requireAuthenticatedUser() {
        if (closed.get()) {
            throw new IllegalStateException("이미 종료된 연결입니다.");
        }

        User authenticatedUser = this.user;

        if (authenticatedUser == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        return authenticatedUser;
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        try {
            socket.close();

        } catch (IOException e) {
        } finally {
            clientRegistry.unregister(this);
        }
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public boolean isAuthenticated() {
        return user != null && !closed.get();
    }

    public boolean isClosed() {
        return closed.get();
    }
}