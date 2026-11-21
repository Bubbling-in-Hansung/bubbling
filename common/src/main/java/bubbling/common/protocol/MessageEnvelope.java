package bubbling.common.protocol;

public record MessageEnvelope(
        MessageType type,
        String requestId,
        String payload
) {}