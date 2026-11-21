package bubbling.server.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.server.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientConnection implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientConnection.class);

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
            if (!closed.get()) {
                log.warn("클라이언트 통신 오류: connectionId={}", id, e);
            }
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 클라이언트 요청: connectionId={}", id, e);
        } catch (RuntimeException e) {
            log.error("클라이언트 처리 중 예외 발생: connectionId={}", id, e);
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

        synchronized (clientRegistry) {
            if (closed.get()) {
                throw new IllegalStateException("이미 종료된 연결입니다.");
            }

            if (this.user != null) {
                throw new IllegalStateException("이미 로그인된 연결입니다.");
            }

            for (ClientConnection connection : clientRegistry.getAll()) {
                if (connection == this || connection.isClosed()) {
                    continue;
                }

                User authenticatedUser = connection.getUser();

                if (authenticatedUser != null && authenticatedUser.getId().equals(user.getId())) {
                    throw new IllegalStateException("다른 기기에서 로그인 중인 계정입니다.");
                }
            }

            this.user = user;
        }

        if (!closed.get()) {
            clientRegistry.notifyOnlineStatusChanged();
        }
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
            log.warn("클라이언트 소켓 종료 실패: connectionId={}", id, e);
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