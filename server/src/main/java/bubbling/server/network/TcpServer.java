package bubbling.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpServer {

    private final int port;
    private final int maxClients;

    private final ClientRegistry clientRegistry;
    private final MessageSender messageSender;
    private final MessageDispatcher messageDispatcher;
    private final ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ServerSocket serverSocket;

    public TcpServer(int port, int maxClients) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("잘못된 포트 번호");
        }

        if (maxClients < 1) {
            throw new IllegalArgumentException("최대 접속자 수는 1 이상이어야 합니다.");
        }

        this.port = port;
        this.maxClients = maxClients;

        this.clientRegistry = new ClientRegistry();
        this.messageSender = new MessageSender(clientRegistry);
        this.messageDispatcher = new MessageDispatcher(messageSender);

        this.executorService = Executors.newFixedThreadPool(maxClients);
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("이미 실행 중인 서버입니다.");
        }

        try (ServerSocket listener = new ServerSocket(port)) {
            this.serverSocket = listener;
            System.out.println("TCP 서버 시작");
            System.out.println("PORT: " + port);
            System.out.println("MAX_CLIENTS: " + maxClients);

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
                        System.out.println("최대 접속자 수 초과");
                        connection.close();
                        continue;
                    }

                    executorService.execute(connection);

                    System.out.println("클라이언트 접속: " + connection.getId());
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
                        System.err.println("연결 처리 실패: " + e.getMessage());
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
            } catch (IOException ignored) {
            }
        }

        clientRegistry.closeAll();
        executorService.shutdownNow();
    }
}
