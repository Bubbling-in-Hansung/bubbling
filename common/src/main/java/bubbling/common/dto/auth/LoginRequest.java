package bubbling.common.dto.auth;

public record LoginRequest(
        String loginId,
        String password
) {}