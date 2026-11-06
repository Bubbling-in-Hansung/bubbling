package bubbling.client.network;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpClient implements AutoCloseable {

    private final String host;
    private final int port;
    private final ClientMessageDispatcher messageDispatcher;

    private Socket socket;
    private MessageSender messageSender;
    private Thread receiverThread;

    public TcpClient(
            String host,
            int port,
            ClientMessageDispatcher messageDispatcher
    ) {
        this.host = host;
        this.port = port;
        this.messageDispatcher = messageDispatcher;
    }

    public void connect() throws IOException {

        if (socket != null) {
            throw new IllegalStateException("이미 연결을 시도한 클라이언트입니다.");
        }

        Socket newSocket = new Socket();

        try {
            newSocket.connect(
                    new InetSocketAddress(host, port),
                    3000
            );

            // 입력 스트림
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            newSocket.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            // 출력 스트림
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            newSocket.getOutputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            this.socket = newSocket;

            // 서버로 메시지 전송
            this.messageSender = new MessageSender(writer);

            // 서버 메시지 수신
            MessageReceiver receiver = new MessageReceiver(
                    reader,
                    messageDispatcher
            );

            receiverThread = new Thread(
                    receiver,
                    "message-receiver"
            );

            receiverThread.setDaemon(true);
            receiverThread.start();

            System.out.println("서버 연결 성공: " + host + ":" + port);

        } catch (IOException e) {
            try {
                newSocket.close();
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    public MessageSender getMessageSender() {
        if (messageSender == null) {
            throw new IllegalStateException("서버에 연결되지 않았습니다.");
        }
        return messageSender;
    }

    @Override
    public void close() {

        if (socket == null) {
            return;
        }

        try {
            socket.close();
        } catch (IOException ignored) {
        }

        System.out.println("서버 연결 종료");
    }
}
