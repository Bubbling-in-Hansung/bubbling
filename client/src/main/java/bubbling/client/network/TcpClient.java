package bubbling.client.network;

import bubbling.common.protocol.MessageEnvelope;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// 테스트를 위해 AutoCloseable 구현
public class TcpClient implements AutoCloseable {

    private final String host;
    private final int port;
    private final ClientMessageDispatcher messageDispatcher;

    private Socket socket;
    // client 위임을 위한 송신 객체
    private volatile MessageSender messageSender;
    private Thread senderThread;
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

    public void clientConnect() throws IOException {

        // 이미 연결된 소켓은 예외 처리
        if (socket != null) {
            throw new IllegalStateException("이미 연결을 시도한 클라이언트입니다.");
        }

        // 최초 1회의 경우 소켓 생성
        Socket firstConnectionSocket = new Socket();

        try {
            // 서버 연결
            firstConnectionSocket.connect(
                    new InetSocketAddress(host, port),
                    3000
            );

            // 입력 스트림
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            firstConnectionSocket.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            // 출력 스트림
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            firstConnectionSocket.getOutputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            // 개인 클라이언트 소켓 설정
            this.socket = firstConnectionSocket;

            // 서버로 메시지 송신 객체
            this.messageSender = new MessageSender(writer);

            // 송신 스레드 실행
            senderThread = new Thread(
                    messageSender,
                    "message-sender"
            );

            senderThread.setDaemon(true);
            senderThread.start();

            // 서버 메시지 수신 객체
            MessageReceiver receiver = new MessageReceiver(
                    reader,
                    messageDispatcher
            );

            // 수신 스레드 실행
            receiverThread = new Thread(
                    receiver,
                    "message-receiver"
            );

            receiverThread.setDaemon(true);
            receiverThread.start();

            // 연결 성공 로그 (다이얼로그나 확인 UI 추가 필요)
            System.out.println("서버 연결 성공: " + host + ":" + port);

        } catch (IOException e) {
            try {
                firstConnectionSocket.close();
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    // 송신 객체 Getter로 송신 기능 위임을 위해 사용
    private MessageSender getMessageSender() {
        if (messageSender == null) {
            throw new IllegalStateException("[송신 스레드 없음] 서버에 연결되지 않았습니다.");
        }
        return messageSender;
    }

    // send() 위임 (MessageSender 접근 X, client에서 직접 호출)
    public void send(MessageEnvelope message) {
        getMessageSender().send(message);
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
