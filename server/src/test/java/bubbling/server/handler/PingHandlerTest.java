package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.config.ServerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PingHandlerTest {

    private final ServerConfig config = new ServerConfig();

    @Test
    @DisplayName("PING 요청 시 PONG 응답")
    void pingTest() throws IOException {
        // Given
        JsonCodec jsonCodec = new JsonCodec();

        try (Socket socket = new Socket("localhost", config.getPort())) {
            socket.setSoTimeout(3000);

            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            socket.getOutputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            MessageEnvelope request = new MessageEnvelope(
                    MessageType.PING,
                    "test-001",
                    null
            );

            // When
            writer.write(jsonCodec.encode(request));
            writer.newLine();
            writer.flush();

            String line = reader.readLine();
            assertNotNull(line);

            MessageEnvelope response = jsonCodec.decode(line);

            // Then
            assertEquals(MessageType.PONG, response.type());
            assertEquals("test-001", response.requestId());
        }
    }
}