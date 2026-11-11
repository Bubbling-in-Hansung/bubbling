package bubbling.server.scheduler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.bubble.BubbleRemovedEvent;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.network.MessageSender;
import bubbling.server.service.BubbleService;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BubbleExpirationScheduler {

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
                        e.printStackTrace();
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