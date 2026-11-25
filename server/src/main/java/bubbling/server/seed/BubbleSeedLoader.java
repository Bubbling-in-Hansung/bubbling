package bubbling.server.seed;

import bubbling.common.codec.JsonCodec;
import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleMethod;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.User;
import bubbling.server.service.bubble.BubbleService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BubbleSeedLoader {

    private final JsonCodec jsonCodec = new JsonCodec();

    private record SeedBubble(
            String key,
            String ownerLoginId,
            String title,
            BubbleCategory category,
            BubbleMethod method,
            int maxMemberCnt,
            boolean isOpen,
            int expireMinute,
            boolean expired
    ) {}

    public Map<String, Bubble> loadBubbles(
            Map<String, User> users,
            BubbleService bubbleService
    ) {
        try (InputStream input = getClass().getResourceAsStream("/seed/bubbles.json")) {
            if (input == null) {
                throw new IllegalStateException("bubbles.json 파일을 찾을 수 없습니다.");
            }

            String json = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            SeedBubble[] bubbles = jsonCodec.fromPayload(
                    json,
                    SeedBubble[].class
            );

            if (bubbles == null) {
                throw new IllegalStateException("비눗방울 테스트 데이터가 없습니다.");
            }

            Map<String, Bubble> createdBubbles = new HashMap<>();

            for (SeedBubble data : bubbles) {
                if (data.key() == null || createdBubbles.containsKey(data.key())) {
                    throw new IllegalStateException("유효하지 않거나 중복된 비눗방울 키: " + data.key());
                }

                User owner = users.get(data.ownerLoginId());
                if (owner == null) {
                    throw new IllegalStateException("존재하지 않는 테스트 사용자: " + data.ownerLoginId());
                }

                Bubble bubble = bubbleService.create(
                        owner,
                        data.title(),
                        data.category(),
                        data.method(),
                        data.maxMemberCnt(),
                        data.isOpen(),
                        data.expireMinute()
                );

                if (data.expired()) {
                    bubble.close();
                }

                createdBubbles.put(data.key(), bubble);
            }

            return createdBubbles;
        } catch (IOException e) {
            throw new IllegalStateException("테스트 계정 로딩 실패", e);
        }
    }
}