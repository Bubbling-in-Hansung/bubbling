package bubbling.server.scheduler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.bubble.BubbleRemovedEvent;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.network.MessageSender;
import bubbling.server.service.BubbleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BubbleExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(BubbleExpirationScheduler.class);

    private final BubbleService bubbleService;
    private final MessageSender messageSender;

    private final JsonCodec jsonCodec = new JsonCodec();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public BubbleExpirationScheduler(BubbleService bubbleService, MessageSender messageSender) {
        this.bubbleService = bubbleService;
        this.messageSender = messageSender;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(
                () -> {
                    try {
                        bubbleService.expireElapsed(LocalDateTime.now())
                                .forEach(bubble -> {
                                    MessageEnvelope event = new MessageEnvelope(
                                            MessageType.BUBBLE_REMOVED,
                                            null,
                                            jsonCodec.toPayload(
                                                    new BubbleRemovedEvent(
                                                            bubble.getId(),
                                                            bubble.getStatus()
                                                    )
                                            )
                                    );

                                    messageSender.broadcast(event);
                                });
                    } catch (Exception e) {
                        log.error("비눗방울 만료 처리 중 오류가 발생했습니다.", e);
                    }
                },
                0,
                1,
                TimeUnit.MINUTES
        );
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}