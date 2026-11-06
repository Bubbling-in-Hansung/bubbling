package bubbling.client.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientConfig {

    private final Properties properties = new Properties();

    public ClientConfig() {

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

    public String getHost() {
        return properties.getProperty("server.host");
    }

    public int getPort() {
        return Integer.parseInt(
                properties.getProperty("server.port")
        );
    }

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
