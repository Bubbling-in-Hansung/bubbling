package bubbling.client.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;

import java.io.BufferedReader;
import java.io.IOException;

public class MessageReceiver implements Runnable {

    private final BufferedReader reader;
    private final ClientMessageDispatcher messageDispatcher;
    private final JsonCodec jsonCodec = new JsonCodec();

    public MessageReceiver(
            BufferedReader reader,
            ClientMessageDispatcher messageDispatcher
    ) {
        this.reader = reader;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void run() {
        try {
            String line;

            while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {

                MessageEnvelope message = jsonCodec.decode(line);

                if (message == null || message.type() == null) {
                    System.err.println("잘못된 메시지 형식입니다.");
                    break;
                }
                messageDispatcher.dispatch(message);
            }

            System.out.println("서버에서 연결을 종료했습니다.");
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("메시지 수신 오류: " + e.getMessage());
        }
    }
}