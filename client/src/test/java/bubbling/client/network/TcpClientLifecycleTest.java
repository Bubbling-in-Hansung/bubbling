package bubbling.client.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TcpClient의 로컬 서버 연결, 양방향 송수신, 중복 연결 예외 및 미연결 송신 예외를 검증한다.
 */
class TcpClientLifecycleTest {

    private final JsonCodec jsonCodec = new JsonCodec();

    @Test
    @DisplayName("TcpClient는 로컬 서버와 양방향 송수신하고 중복 clientConnect()를 차단한다")
    void connectsExchangesMessagesAndRejectsSecondConnect() throws Exception {
        BlockingQueue<MessageEnvelope> clientReceived = new LinkedBlockingQueue<>();
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher() {
            @Override
            public void dispatch(MessageEnvelope message) {
                clientReceived.offer(message);
            }
        };

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            BlockingQueue<MessageEnvelope> serverReceived = new LinkedBlockingQueue<>();

            Thread serverThread = new Thread(() -> {
                try (Socket accepted = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8));
                     BufferedWriter out = new BufferedWriter(new OutputStreamWriter(accepted.getOutputStream(), StandardCharsets.UTF_8))) {

                    String line = in.readLine();
                    if (line != null) {
                        MessageEnvelope req = jsonCodec.decode(line);
                        serverReceived.offer(req);
                        MessageEnvelope resp = new MessageEnvelope(
                                MessageType.PONG,
                                req.requestId(),
                                jsonCodec.toPayload("pong")
                        );
                        out.write(jsonCodec.encode(resp));
                        out.newLine();
                        out.flush();
                    }
                    in.readLine();
                } catch (IOException ignored) {
                }
            }, "test-loopback-server");
            serverThread.setDaemon(true);
            serverThread.start();

            try (TcpClient client = new TcpClient("127.0.0.1", port, dispatcher)) {
                client.clientConnect();
                assertThrows(IllegalStateException.class, client::clientConnect);

                MessageEnvelope ping = new MessageEnvelope(MessageType.PING, "req-100", jsonCodec.toPayload("ping"));
                client.send(ping);

                MessageEnvelope onServer = serverReceived.poll(2, TimeUnit.SECONDS);
                assertNotNull(onServer);
                assertEquals(MessageType.PING, onServer.type());

                MessageEnvelope onClient = clientReceived.poll(2, TimeUnit.SECONDS);
                assertNotNull(onClient);
                assertEquals(MessageType.PONG, onClient.type());
                assertEquals("req-100", onClient.requestId());
            }

            serverThread.join(2000);
        }
    }

    @Test
    @DisplayName("연결 전 send() 호출은 IllegalStateException을 던지고 연결 실패 시 IOException을 던진다")
    void rejectsSendBeforeConnectAndThrowsOnUnreachableServer() throws Exception {
        int unusedPort;
        try (ServerSocket ephemeral = new ServerSocket(0)) {
            unusedPort = ephemeral.getLocalPort();
        }

        try (TcpClient client = new TcpClient("127.0.0.1", unusedPort, new ClientMessageDispatcher())) {
            MessageEnvelope ping = new MessageEnvelope(MessageType.PING, "req-fail", jsonCodec.toPayload("ping"));
            assertThrows(IllegalStateException.class, () -> client.send(ping));
            assertThrows(IOException.class, client::clientConnect);
        }
    }
}
