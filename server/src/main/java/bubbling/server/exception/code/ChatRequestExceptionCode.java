package bubbling.server.exception.code;

import bubbling.server.exception.ExceptionCode;

public enum ChatRequestExceptionCode implements ExceptionCode {
    NOT_FOUND("CHAT_REQUEST_001", "존재하지 않는 참여 요청입니다."),
    FORBIDDEN("CHAT_REQUEST_002", "참여 요청을 처리할 권한이 없습니다."),
    ALREADY_PENDING("CHAT_REQUEST_003", "이미 대기 중인 참여 요청이 있습니다."),
    ALREADY_PROCESSED("CHAT_REQUEST_004", "이미 처리된 참여 요청입니다."),
    NOT_ALLOWED("CHAT_REQUEST_005", "참여 요청을 보낼 수 없습니다."),
    OPEN_CHATROOM("CHAT_REQUEST_006", "공개된 채팅방입니다."),
    NOT_FOUND_ACCEPT("CHAT_REQUEST_007", "참여 승인 기록이 없습니다."),
    ;

    private final String code;
    private final String message;

    ChatRequestExceptionCode(String code, String message) {
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