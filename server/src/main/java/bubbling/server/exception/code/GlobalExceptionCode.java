package bubbling.server.exception.code;

import bubbling.server.exception.ExceptionCode;

public enum GlobalExceptionCode implements ExceptionCode {
    INVALID_REQUEST("GLOBAL_001", "잘못된 요청입니다."),
    INTERNAL_ERROR("GLOBAL_002", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;

    GlobalExceptionCode(String code, String message) {
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