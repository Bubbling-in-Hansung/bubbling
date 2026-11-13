package bubbling.common.codec;

import bubbling.common.protocol.MessageEnvelope;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class JsonCodec {
    
    private final Gson gson = new Gson();
    
    public String encode(MessageEnvelope message) {
        return gson.toJson(message);
    }
    
    public MessageEnvelope decode(String json) {
        try {
            return gson.fromJson(json, MessageEnvelope.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException(
                    "메시지 JSON 변환 실패", e
            );
        }
    }
    
    public String toPayload(Object object) {
        return gson.toJson(object);
    }

    public <T> T fromPayload(String payload, Class<T> type) {
        try {
            return gson.fromJson(payload, type);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException(
                    "Payload JSON 변환 실패", e
            );
        }
    }
}