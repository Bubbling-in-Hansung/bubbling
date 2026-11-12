package bubbling.server.exception.code;

import bubbling.server.exception.ExceptionCode;

public enum UserExceptionCode implements ExceptionCode {
    NOT_FOUND("USER_001", "존재하지 않는 사용자입니다."),
    DUPLICATE_LOGIN_ID("USER_002", "이미 사용 중인 아이디입니다."),
    INVALID_NICKNAME("USER_003", "유효하지 않은 닉네임입니다."),
    INVALID_LOGIN_ID("USER_004", "유효하지 않은 로그인 아이디입니다."),
    INVALID_PASSWORD_LENGTH("USER_005", "유효한 패스워드 길이를 벗어났습니다."),
    INVALID_PASSWORD_CHARACTER("USER_006", "패스워드에 유효하지 않은 문자가 포함되었습니다.");

    private final String code;
    private final String message;

    UserExceptionCode(String code, String message) {
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