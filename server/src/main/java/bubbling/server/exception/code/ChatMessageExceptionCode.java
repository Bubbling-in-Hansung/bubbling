package bubbling.server.exception.code;

import bubbling.server.exception.ExceptionCode;

public enum ChatMessageExceptionCode implements ExceptionCode {
    NOT_FOUND("CHAT_MESSAGE_001", "존재하지 않는 메시지입니다."),
    INVALID_MESSAGE("CHAT_MESSAGE_002", "유효하지 않은 메시지입니다."),
    FORBIDDEN("CHAT_MESSAGE_003", "메시지를 전송할 권한이 없습니다.");

    private final String code;
    private final String message;

    ChatMessageExceptionCode(String code, String message) {
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