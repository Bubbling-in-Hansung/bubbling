package bubbling.server.exception.code;

import bubbling.server.exception.ExceptionCode;

public enum ChatMemberExceptionCode implements ExceptionCode {
    NOT_FOUND("CHAT_MEMBER_001", "존재하지 않는 채팅 멤버입니다."),
    ALREADY_JOINED("CHAT_MEMBER_002", "이미 채팅방에 참여 중인 사용자입니다."),
    NOT_JOINED("CHAT_MEMBER_003", "채팅방에 참여 중이지 않은 사용자입니다."),
    ALREADY_LEFT("CHAT_MEMBER_004", "이미 채팅방에서 퇴장한 사용자입니다."),
    INVALID_NICKNAME("CHAT_MEMBER_005", "유효하지 않은 채팅 닉네임입니다."),
    CANNOT_LEFT_OWNER("CHAT_MEMBER_006", "방장은 채팅방을 나갈 수 없습니다.");

    private final String code;
    private final String message;

    ChatMemberExceptionCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}