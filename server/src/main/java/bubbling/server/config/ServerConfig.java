package bubbling.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerConfig {

    private final Properties properties = new Properties();

    public ServerConfig() {
        try (InputStream input = getClass().getResourceAsStream("/server.properties")) {
            if (input == null) {
                throw new IllegalStateException("server.properties 파일을 찾을 수 없습니다.");
            }

            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("서버 설정 파일 로드 실패", e);
        }
    }

    public int getPort() {
        return Integer.parseInt(properties.getProperty("server.port"));
    }

    public int getMaxClients() {
        return Integer.parseInt(properties.getProperty("server.max-clients"));
    }

    public int getBubbleLimitSize() {
        return Integer.parseInt(properties.getProperty("bubble.max-per-user"));
    }

    public int getBubbleDefaultExpireTime() {
        return Integer.parseInt(properties.getProperty("bubble.default-expiration"));
    }
}