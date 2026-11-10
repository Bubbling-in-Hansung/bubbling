package bubbling.server.handler;

import bubbling.common.protocol.MessageEnvelope;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;
import bubbling.server.service.BubbleService;

public class BubbleHandler {

    private final BubbleService bubbleService;
    private final MessageSender messageSender;

    public BubbleHandler(BubbleService bubbleService, MessageSender messageSender) {
        this.bubbleService = bubbleService;
        this.messageSender = messageSender;
    }

    public void create(ClientConnection connection, MessageEnvelope message) {
        // 비눗방울 생성
    }

    public void update(ClientConnection connection, MessageEnvelope message) {
        // 비눗방울 수정
    }

    public void delete(ClientConnection connection, MessageEnvelope message) {
        // 비눗방울 삭제
    }

    public void react(ClientConnection connection, MessageEnvelope message) {
        // 비눗방울 공감
    }
}