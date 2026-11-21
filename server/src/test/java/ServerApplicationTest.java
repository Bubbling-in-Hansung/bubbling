import bubbling.server.config.ServerComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ServerApplicationTest {

    @Test
    @DisplayName("서버 컴포넌트 정상 초기화")
    void initialize() {
        assertDoesNotThrow(ServerComponent::new);
    }
}