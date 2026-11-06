package bubbling.client;

import bubbling.client.config.ClientConfig;
import bubbling.client.network.ClientMessageDispatcher;
import bubbling.client.network.MessageSender;
import bubbling.client.network.TcpClient;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

public class TcpConsoleTest {

    public static void main(String[] args) {

        ClientConfig config = new ClientConfig();

        ClientMessageDispatcher dispatcher =
                new ClientMessageDispatcher();

        JsonCodec jsonCodec = new JsonCodec();

        try (TcpClient client = new TcpClient(
                config.getHost(),
                config.getPort(),
                dispatcher
        )) {

            client.connect();

            MessageSender sender = client.getMessageSender();

            Scanner scanner = new Scanner(System.in);

            System.out.println("=== TCP 연결 테스트 ===");
            System.out.println("ping: PING 전송");
            System.out.println("일반 텍스트: 브로드캐스트 테스트");
            System.out.println("exit: 종료");

            while (scanner.hasNextLine()) {

                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                if (input.isEmpty()) {
                    continue;
                }

                MessageType type =
                        input.equalsIgnoreCase("ping")
                                ? MessageType.PING
                                : MessageType.TEST_MESSAGE;

                MessageEnvelope message = new MessageEnvelope(
                        type,
                        UUID.randomUUID().toString(),
                        jsonCodec.toPayload(input)
                );

                sender.send(message);
            }

        } catch (IOException e) {
            System.err.println("TCP 통신 오류: " + e.getMessage());
        }
    }
}