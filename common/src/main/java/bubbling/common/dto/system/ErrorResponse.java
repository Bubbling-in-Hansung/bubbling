package bubbling.common.dto.system;

public record ErrorResponse(
        String code,
        String message
) {}