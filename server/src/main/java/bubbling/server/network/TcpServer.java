package bubbling.server.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpServer {

    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);

    private final int port;
    private final int maxClients;

    private final ClientRegistry clientRegistry;
    private final MessageDispatcher messageDispatcher;

    private final ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile ServerSocket serverSocket;

    public TcpServer(
            int port,
            int maxClients,
            ClientRegistry clientRegistry,
            MessageDispatcher messageDispatcher
    ) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("잘못된 포트 번호입니다.");
        }

        if (maxClients < 1) {
            throw new IllegalArgumentException("최대 접속자 수는 1 이상이어야 합니다.");
        }

        this.port = port;
        this.maxClients = maxClients;

        this.clientRegistry = Objects.requireNonNull(
                clientRegistry,
                "ClientRegistry는 필수입니다."
        );

        this.messageDispatcher = Objects.requireNonNull(
                messageDispatcher,
                "MessageDispatcher는 필수입니다."
        );

        this.executorService = Executors.newFixedThreadPool(maxClients);
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("이미 실행 중인 서버입니다.");
        }

        try (ServerSocket listener = new ServerSocket(port)) {
            this.serverSocket = listener;
            log.info("TCP 서버 시작: port={}, maxClients={}", port, maxClients);

            while (running.get()) {
                Socket socket;
                try {
                    socket = listener.accept();
                } catch (SocketException e) {
                    if (running.get()) {
                        throw e;
                    }
                    break;
                }

                ClientConnection connection = null;
                try {
                    connection = new ClientConnection(
                            socket,
                            clientRegistry,
                            messageDispatcher
                    );

                    if (!clientRegistry.register(connection, maxClients)) {
                        log.warn("최대 접속자 수 초과: maxClients={}", maxClients);
                        connection.close();
                        continue;
                    }

                    executorService.execute(connection);

                    log.info("클라이언트 접속: id={}", connection.getId());
                } catch (IOException | RejectedExecutionException e) {
                    if (connection != null) {
                        connection.close();
                    } else {
                        try {
                            socket.close();
                        } catch (IOException ignored) {
                        }
                    }

                    if (running.get()) {
                        log.warn("클라이언트 연결 처리 실패", e);
                    }
                }
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running.set(false);
        ServerSocket listener = serverSocket;

        if (listener != null) {
            try {
                listener.close();
            } catch (IOException e) {
                log.warn("서버 소켓 종료 중 오류가 발생했습니다.", e);
            }
        }

        clientRegistry.closeAll();
        executorService.shutdownNow();
    }
}
