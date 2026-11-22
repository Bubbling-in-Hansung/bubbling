package bubbling.server.exception.code;

import bubbling.server.exception.ExceptionCode;

public enum BubbleExceptionCode implements ExceptionCode {
    NOT_FOUND("BUBBLE_001", "존재하지 않는 비눗방울입니다."),
    FORBIDDEN("BUBBLE_002", "비눗방울에 대한 권한이 없습니다."),
    EXPIRED("BUBBLE_003", "만료된 비눗방울입니다."),
    ALREADY_EXISTS("BUBBLE_004", "이미 활성화된 비눗방울이 존재합니다."),
    INVALID_VALUE("BUBBLE_005", "비눗방울 설정값이 유효하지 않습니다."),
    FAILED_TO_CREATION("BUBBLE_006", "비눗방울 생성에 실패했습니다."),
    NOT_ALLOWED_TO_OWNER("BUBBLE_007", "본인의 비눗방울에 공감할 수 없습니다."),
    INVALID_REACTION_PERIOD("BUBBLE_008", "공감은 1분에 한 번씩 할 수 있습니다."),
    ALREADY_EXISTS_REACTION("BUBBLE_009", "해당 비눗방울에 대한 리액션이 이미 존재합니다."),
    ;

    private final String code;
    private final String message;

    BubbleExceptionCode(String code, String message) {
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