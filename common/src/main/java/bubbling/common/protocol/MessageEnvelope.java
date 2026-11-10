package bubbling.common.protocol;

// 서버 전송 raw 프로토콜 공통 규격
// jsonCodec.decode()로 인코딩된 json 문자열 객체 변환(MessageEnvelope) 가능
public record MessageEnvelope(
        MessageType type,
        String requestId,
        // toPayload()로 객체 인코딩, fromPayload()로 객체 반환(디코딩)
        String payload
) {}