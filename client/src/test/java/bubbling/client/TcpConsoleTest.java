package bubbling.client;

import bubbling.client.config.ClientConfig;
import bubbling.client.network.ClientMessageDispatcher;
import bubbling.client.network.TcpClient;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

/**
 * 수동 TCP 콘솔 테스트 실행용 클래스.
 * - JUnit 자동 테스트가 아닌 main() 기반 대화형 테스트로, build.gradle에서 test 태스크 실행 대상에서 제외되어 있음.
 * - 서버를 먼저 실행한 뒤 IDE에서 직접 main()을 실행하여 PING/TEST_MESSAGE 송수신을 수동 확인할 때 사용.
 */
public class TcpConsoleTest {

    public static void main(String[] args) {

        // client.properties에서 서버 호스트/포트 설정 로드
        ClientConfig config = new ClientConfig();

        // 수신 메시지 처리용 디스패처 생성
        ClientMessageDispatcher dispatcher =
                new ClientMessageDispatcher();

        // payload JSON 직렬화용 코덱 생성
        JsonCodec jsonCodec = new JsonCodec();

        // try-with-resources로 종료 시 TcpClient.close() 자동 호출
        try (TcpClient client = new TcpClient(
                config.getHost(),
                config.getPort(),
                dispatcher
        )) {

            // TCP 서버 연결 및 송·수신 데몬 스레드 시작
            client.clientConnect();

            Scanner scanner = new Scanner(System.in);

            System.out.println("=== TCP 연결 테스트 ===");
            System.out.println("ping: PING 전송");
            System.out.println("일반 텍스트: 브로드캐스트 테스트");
            System.out.println("exit: 종료");

            // 콘솔 입력을 읽어 서버로 테스트 메시지 전송
            while (scanner.hasNextLine()) {

                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                if (input.isEmpty()) {
                    continue;
                }

                // "ping" 입력 시 PING, 그 외 입력은 TEST_MESSAGE로 분기
                MessageType type =
                        input.equalsIgnoreCase("ping")
                                ? MessageType.PING
                                : MessageType.TEST_MESSAGE;

                MessageEnvelope message = new MessageEnvelope(
                        type,
                        UUID.randomUUID().toString(),
                        jsonCodec.toPayload(input)
                );

                client.send(message);
            }

        } catch (IOException e) {
            System.err.println("TCP 통신 오류: " + e.getMessage());
        }
    }
}