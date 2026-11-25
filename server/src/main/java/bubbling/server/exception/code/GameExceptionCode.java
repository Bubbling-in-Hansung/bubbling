package bubbling.server.exception.code;

import bubbling.server.exception.ExceptionCode;

public enum GameExceptionCode implements ExceptionCode {
    NEGATIVE_SCORE("GAME_001", "게임 점수는 음수가 될 수 없습니다."),
    NOT_FOUND("GAME_002", "게임 결과를 찾을 수 없습니다."),
    ;

    private final String code;
    private final String message;

    GameExceptionCode(String code, String message) {
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