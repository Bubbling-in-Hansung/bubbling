package bubbling.client.network;

import bubbling.common.codec.JsonCodec;
import bubbling.common.protocol.MessageEnvelope;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// 단일 송신 스레드에서 BlockingQueue에 적재된 메시지를 순차적으로 서버에 전송
public class MessageSender implements Runnable {

    private final BufferedWriter writer;
    private final JsonCodec jsonCodec = new JsonCodec();
    // 송신 메시지 적재용 블로킹 큐 (UI 멈춤 방지)
    private final BlockingQueue<MessageEnvelope> sendQueue = new LinkedBlockingQueue<>();

    public MessageSender(BufferedWriter writer) {
        this.writer = writer;
    }

    // 호출 스레드(Swing EDT)를 블로킹하지 않고 송신 큐에 메시지 적재
    // 스레드에서는 직접 보내도록 하는게 아닌 큐에 있는 송신 내용을 while로 서버에 전달
    public void send(MessageEnvelope message) {
        if (message == null) {
            throw new NullPointerException("메세지는 null일 수 없습니다.");
        }

        sendQueue.offer(message);
    }

    @Override
    public void run() {
        try {
            // 스레드 인터럽트 상태 검사 후 송신 큐에서 순차 전송
            while (!Thread.currentThread().isInterrupted()) {
                // take()로 큐가 비어있으면 대기(Blocking), 큐에 메시지가 적재되면 반환 진행
                MessageEnvelope message = sendQueue.take();

                try {
                    // 송신 메세지 인코딩 후 버퍼 쓰기
                    writer.write(jsonCodec.encode(message));
                    writer.newLine();
                    writer.flush();
                } catch(NullPointerException e) {
                    System.err.println("NULL 메세지 전달 오류 : " + e.getMessage());
                } catch (RuntimeException e) {
                    System.err.println("메시지 직렬화 중 예기치 않은 오류 : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            // 스레드 인터럽트 발생 시 종료 처리
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("메시지 송신 오류 : " + e.getMessage());
        } finally {
            // 남은 블로킹 큐 비우기
            sendQueue.clear();
        }
    }
}
