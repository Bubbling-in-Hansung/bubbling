package bubbling.common.codec;

import bubbling.common.protocol.MessageEnvelope;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class JsonCodec {
    
    // gson 라이브러리 활용
    private final Gson gson = new Gson();
    
    // 기본 통신 프로토콜 json 인코딩 후 반환 
    public String encode(MessageEnvelope message) {
        return gson.toJson(message);
    }
    
    // 변환된 json 인코딩을 디코딩 진행 후 기본 통신 프로토콜로 반환
    public MessageEnvelope decode(String json) {
        try {
            return gson.fromJson(json, MessageEnvelope.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException(
                    "메시지 JSON 변환 실패", e
            );
        }
    }
    
    // payload 속성만 따로 json 인코딩
    public String toPayload(Object object) {
        return gson.toJson(object);
    }

    // payload 속성에 대해 템플릿에 맞는 객체 생성
    public <T> T fromPayload(String payload, Class<T> type) {
        try {
            // client / server 디스패처에서 지정된 type을 받아 객체 반환
            // -> MessageEnvelope의 type은 디스패처에서 플래그로 사용하고
            //    플래그에 맞는 디스패처 내부에서 클래스 타입을 인자로 전달
            return gson.fromJson(payload, type);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException(
                    "Payload JSON 변환 실패", e
            );
        }
    }
}