package bubbling.server.exception.code;

import bubbling.server.exception.ExceptionCode;

public enum ChatRoomExceptionCode implements ExceptionCode {
    NOT_FOUND("CHAT_ROOM_001", "존재하지 않는 채팅방입니다."),
    ENDED("CHAT_ROOM_002", "종료된 채팅방입니다."),
    FULL("CHAT_ROOM_003", "채팅방 정원이 가득 찼습니다."),
    ALREADY_JOINED("CHAT_ROOM_004", "이미 참여 중인 채팅방입니다."),
    NOT_JOINED("CHAT_ROOM_005", "참여 중이지 않은 채팅방입니다."),
    APPROVAL_REQUIRED("CHAT_ROOM_006", "참여 승인이 필요한 채팅방입니다."),
    FAILED_TO_CREATION("CHAT_ROOM_007", "채팅방 생성에 실패했습니다."),
    ALREADY_EXISTS("CHAT_ROOM_008", "채팅방이 이미 존재합니다."),
    ;

    private final String code;
    private final String message;

    ChatRoomExceptionCode(String code, String message) {
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