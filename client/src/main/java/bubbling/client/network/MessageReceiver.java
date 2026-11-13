package bubbling.client.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;

import java.io.BufferedReader;
import java.io.IOException;

public class MessageReceiver implements Runnable {

    private final BufferedReader reader;
    private final ClientMessageDispatcher messageDispatcher;
    // 객체 변환 코덱 (프로토콜 규격 변환만 사용중)
    private final JsonCodec jsonCodec = new JsonCodec();

    public MessageReceiver(
            BufferedReader reader,
            ClientMessageDispatcher messageDispatcher
    ) {
        this.reader = reader;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void run() {
        try {
            String line;

            // 스레드 인터럽트 상태 검사 후 입력 스트림 수신
            while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {
                try {
                    // 수신 메세지 -> 프로토콜 규격으로 변환
                    MessageEnvelope message = jsonCodec.decode(line);

                    // 에러 처리
                    if (message == null || message.type() == null) {
                        System.err.println("잘못된 메시지 형식입니다.");
                        continue;
                    }

                    // 프로토콜 규격을 디스패처로 전송 (디스패처에서 변환 후 EDT 전송)
                    // 프로토콜 규격만 전송하는 것이 목적 (이후 내용은 디스패처 내부에서 처리)
                    messageDispatcher.dispatch(message);
                }
                // catch 대상 : 프로토콜 규격, DTO 규격 파싱 실패 또는 NULL 참조 (2차)
                catch (IllegalArgumentException e) {
                    System.err.println("파싱 실패(무시됨): " + e.getMessage());
                } catch (RuntimeException e) {
                    // 디스패처 처리 중 예상치 못한 버그 처리
                    System.err.println("메시지 처리 중 예기치 않은 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // swing UI에 종료 기반 다이얼로그 출력 필요
            // 서버 종료 : readline()에서 null 수신
            System.out.println("서버에서 연결을 종료했습니다.");
        }
        // catch 대상 : 소켓 연결 끊김(IOException)
        catch (IOException e) {
            // swing UI에 종료 기반 다이얼로그 출력 필요
            System.err.println("메시지 수신 오류: " + e.getMessage());
        }
    }
}