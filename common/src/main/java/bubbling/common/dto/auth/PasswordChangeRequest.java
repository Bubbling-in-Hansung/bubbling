package bubbling.common.dto.auth;

public record PasswordChangeRequest(
        String currentPassword,
        String newPassword
) {}
