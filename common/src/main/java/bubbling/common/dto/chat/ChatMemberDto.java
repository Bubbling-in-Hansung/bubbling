package bubbling.common.dto.chat;

public record ChatMemberDto(
        String roomId,
        String memberId,
        String randomNickname
) {}