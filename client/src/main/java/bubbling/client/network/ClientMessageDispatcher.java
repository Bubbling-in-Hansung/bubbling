package bubbling.client.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;

// 수신한 프로토콜에 맞춰 swing UI 변경 정보(DTO)를 EDT로 전송
// 추후, dispatch 구조 추상화 후 구현 클래스 분리 가능 (합의 필요)
public class ClientMessageDispatcher {

    // DTO 객체 변환 코덱
    private final JsonCodec jsonCodec = new JsonCodec();

    // EDT 전송 (단독 시스템 관리는 미정)
    // SwingUtilities.invokeLater(() -> { ... });로 동시성 관리
    public void dispatch(MessageEnvelope message) {
        switch (message.type()) {
            case PONG -> {
                String content = jsonCodec.fromPayload(message.payload(), String.class);
                System.out.println("[PONG] " + content);
            }

            case TEST_BROADCAST -> {
                String content = jsonCodec.fromPayload(message.payload(), String.class);
                System.out.println("[BROADCAST] " + content);
            }

            case ERROR -> {
                String content = jsonCodec.fromPayload(message.payload(), String.class);
                System.err.println("[ERROR] " + content);
            }

            default -> {
                System.out.println("처리되지 않은 메시지: " + message.type());
            }
        }
    }
}
