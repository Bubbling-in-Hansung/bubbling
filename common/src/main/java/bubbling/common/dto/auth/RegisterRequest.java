package bubbling.common.dto.auth;

public record RegisterRequest(
        String loginId,
        String password
) {}