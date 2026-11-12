package bubbling.server.exception.code;

import bubbling.server.exception.ExceptionCode;

public enum AuthExceptionCode implements ExceptionCode {
    INVALID_LOGIN_ID("AUTH_001", "유효하지 않은 아이디입니다."),
    INVALID_PASSWORD("AUTH_002", "유효하지 않은 비밀번호입니다."),
    INVALID_CREDENTIALS("AUTH_003", "아이디 또는 비밀번호가 일치하지 않습니다."),
    AUTH_REQUIRED("AUTH_004", "로그인이 필요합니다."),
    ALREADY_LOGGED_IN("AUTH_005", "이미 로그인 중인 사용자입니다.");

    private final String code;
    private final String message;

    AuthExceptionCode(String code, String message) {
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