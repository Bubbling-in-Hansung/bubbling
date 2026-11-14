package bubbling.client;

import bubbling.client.config.ClientConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 클라이언트 진입점 패키지 구조(방안 A) 및 기본 설정 로드 검증 테스트.
 */
class ClientConfigAndEntryTest {

    @Test
    @DisplayName("ClientApplication과 ClientBootstrap은 루트 패키지 bubbling.client에 위치한다")
    void entryClassesBelongToBubblingClientPackage() {
        assertEquals("bubbling.client", ClientApplication.class.getPackageName());
        assertEquals("bubbling.client", ClientBootstrap.class.getPackageName());
        assertDoesNotThrow(ClientBootstrap::new);
    }

    @Test
    @DisplayName("ClientConfig는 client.properties의 기본 서버 및 창 설정을 로드한다")
    void loadsDefaultProperties() {
        ClientConfig config = new ClientConfig();

        assertEquals("127.0.0.1", config.getHost());
        assertEquals(8088, config.getPort());
        assertEquals(1200, config.getWidth());
        assertEquals(700, config.getHeight());
        assertEquals(1000, config.getMinWidth());
        assertEquals(600, config.getMinHeight());
    }
}
