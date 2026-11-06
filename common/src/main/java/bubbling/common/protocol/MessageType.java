package bubbling.common.protocol;

public enum MessageType {
    // 통신
    PING,
    PONG,
    TEST_MESSAGE,
    TEST_BROADCAST,

    // 인증
    REGISTER,
    REGISTER_RESULT,
    LOGIN,
    LOGIN_RESULT,

    // 메인 화면
    INITIAL_STATE_REQUEST,
    INITIAL_STATE,
    ONLINE_STATUS,

    // 비눗방울
    BUBBLE_CREATE,
    BUBBLE_CREATED,
    BUBBLE_UPDATE,
    BUBBLE_UPDATED,
    BUBBLE_DELETE,
    BUBBLE_REMOVED,
    BUBBLE_REACT,
    BUBBLE_REACTED,

    // 상담 요청
    CHAT_REQUEST,
    CHAT_REQUESTED,
    CHAT_ACCEPT,
    CHAT_REJECT,
    CHAT_REQUEST_UPDATED,

    // 채팅방
    CHAT_STARTED,
    CHAT_MESSAGE,
    CHAT_END,
    CHAT_ENDED,

    // 시스템 오류
    ERROR
}
