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

    // 사용자
    USER_INFO_REQUEST,
    USER_INFO,
    USER_UPDATE,
    USER_UPDATED,

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

    // 상담 요청 (비공개 채팅방)
    CHAT_REQUEST,
    CHAT_REQUESTED,
    CHAT_ACCEPT,
    CHAT_REJECT,
    CHAT_REQUEST_UPDATED,

    // 채팅방
    CHAT_STARTED,
    CHAT_JOIN,
    CHAT_JOINED,
    CHAT_LEAVE,
    CHAT_LEFT,
    CHAT_MEMBER_JOINED,
    CHAT_MEMBER_LEFT,

    // 채팅방 조회
    CHAT_ROOM_LIST_REQUEST,
    CHAT_ROOM_LIST,
    CHAT_ROOM_DETAIL_REQUEST,
    CHAT_ROOM_DETAIL,

    // 채팅 메시지
    CHAT_MESSAGE,
    CHAT_HISTORY_REQUEST,
    CHAT_HISTORY,

    // 채팅방 종료
    CHAT_END,
    CHAT_ENDED,

    // 시스템 오류
    ERROR
}