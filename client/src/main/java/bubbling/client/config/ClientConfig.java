package bubbling.client.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientConfig {

    private final Properties properties = new Properties();

    public ClientConfig() {
        // 정적 설정 프로퍼티 파일 불러오기
        try (InputStream input =
                     getClass().getResourceAsStream("/client.properties")) {

            if (input == null) {
                throw new IllegalStateException(
                        "client.properties 파일이 없습니다."
                );
            }

            properties.load(input);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "클라이언트 설정을 불러올 수 없습니다.", e
            );
        }
    }

    // 서버 호스트 정보 가져오기 (정적 설정 >> 현재 주소 : 127.0.0.1)
    public String getHost() {
        return properties.getProperty("server.host");
    }

    // 서버 포트 정보 가져오기 (정적 설정 >> 현재 포트 : 8088)
    public int getPort() {
        return Integer.parseInt(
                properties.getProperty("server.port")
        );
    }
    
    // UI Config 따로 클래스 분리 필요 (합의 필요)
    // 정적 UI 세팅 설정 (전체 UI 일회성 설정은 여기서 진행)
    public int getWidth() {
        return Integer.parseInt(properties.getProperty("client.width"));
    }

    public int getHeight() {
        return Integer.parseInt(properties.getProperty("client.height"));
    }

    public int getMinWidth() {
        return Integer.parseInt(properties.getProperty("client.min-width"));
    }

    public int getMinHeight() {
        return Integer.parseInt(properties.getProperty("client.min-height"));
    }
}
