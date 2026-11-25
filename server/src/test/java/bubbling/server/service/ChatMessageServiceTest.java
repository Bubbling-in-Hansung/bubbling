package bubbling.server.service;

import bubbling.server.domain.*;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.ChatMessageExceptionCode;
import bubbling.server.exception.code.ChatRoomExceptionCode;
import bubbling.server.fixture.BubbleFixtures;
import bubbling.server.fixture.UserFixtures;
import bubbling.server.service.chat.ChatMemberService;
import bubbling.server.service.chat.ChatMessageService;
import bubbling.server.service.chat.ChatRoomService;
import bubbling.server.service.user.PasswordService;
import bubbling.server.service.user.AuthService;
import bubbling.server.service.user.UserService;
import bubbling.server.store.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageServiceTest {

    private ChatMemberStore chatMemberStore;
    private ChatRoomStore chatRoomStore;
    private ChatMessageStore chatMessageStore;

    private AuthService authService;
    private ChatMemberService chatMemberService;
    private ChatMessageService chatMessageService;
    private ChatRoomService chatRoomService;

    private User owner;
    private Bubble bubble;
    private ChatRoom chatRoom;
    private ChatMember ownerMember;

    @BeforeEach
    void setUp() {
        // Store
        UserStore userStore = new UserStore();
        chatMemberStore = new ChatMemberStore();
        chatRoomStore = new ChatRoomStore();
        chatMessageStore = new ChatMessageStore();
        ChatRequestStore chatRequestStore = new ChatRequestStore();

        // Service
        UserService userService = new UserService(userStore);
        PasswordService passwordService = new PasswordService();

        authService = new AuthService(
                userService,
                passwordService
        );

        chatMemberService = new ChatMemberService(chatMemberStore);
        chatMessageService = new ChatMessageService(chatMessageStore);

        chatRoomService = new ChatRoomService(
                chatRoomStore,
                chatMessageService,
                chatMemberService,
                chatRequestStore
        );

        // 공통 테스트 데이터
        owner = UserFixtures.createUser(authService, "1");
        bubble = BubbleFixtures.createOpenBubble(owner);

        chatRoom = chatRoomService.create(bubble);

        ownerMember = chatMemberService
                .findByChatRoomAndUserAndIsActive(chatRoom, owner);

        assertNotNull(ownerMember);
    }

    @Test
    @DisplayName("채팅 메시지 생성 및 저장 성공")
    void create() {
        // Given
        String content = "안녕하세요!";

        // When
        ChatMessage chatMessage = chatMessageService.create(
                chatRoom,
                ownerMember,
                content
        );

        // Then
        ChatMessage savedMessage = chatMessageStore
                .findById(chatMessage.getId())
                .orElseThrow();

        assertAll(
                () -> assertSame(chatMessage, savedMessage),
                () -> assertEquals(chatRoom, savedMessage.getChatRoom()),
                () -> assertEquals(ownerMember, savedMessage.getChatMember()),
                () -> assertEquals(content, savedMessage.getMessage()),
                () -> assertNotNull(savedMessage.getCreatedAt())
        );
    }

    @Test
    @DisplayName("채팅방의 모든 메시지 삭제")
    void deleteAllByChatRoom() {
        // Given
        ChatMessage message1 = chatMessageService.create(
                chatRoom,
                ownerMember,
                "첫 번째 메시지"
        );

        ChatMessage message2 = chatMessageService.create(
                chatRoom,
                ownerMember,
                "두 번째 메시지"
        );

        assertEquals(
                2,
                chatMessageService.findAllByChatRoom(chatRoom).size()
        );

        // When
        chatMessageService.deleteAllByChatRoom(chatRoom);

        // Then
        assertAll(
                () -> assertTrue(
                        chatMessageStore.findById(message1.getId()).isEmpty()
                ),
                () -> assertTrue(
                        chatMessageStore.findById(message2.getId()).isEmpty()
                ),
                () -> assertTrue(
                        chatMessageService.findAllByChatRoom(chatRoom).isEmpty()
                )
        );
    }

    @Test
    @DisplayName("다른 채팅방의 메시지는 삭제되지 않음")
    void deleteOnlyTargetChatRoom() {
        // Given
        ChatMessage message1 = chatMessageService.create(
                chatRoom,
                ownerMember,
                "첫 번째 채팅방 메시지"
        );

        User otherOwner = UserFixtures.createUser(authService, "2");
        Bubble otherBubble = BubbleFixtures.createOpenBubble(otherOwner);

        ChatRoom otherRoom = chatRoomService.create(otherBubble);

        ChatMember otherMember = chatMemberService
                .findByChatRoomAndUserAndIsActive(otherRoom, otherOwner);

        ChatMessage message2 = chatMessageService.create(
                otherRoom,
                otherMember,
                "두 번째 채팅방 메시지"
        );

        // When
        chatMessageService.deleteAllByChatRoom(chatRoom);

        // Then
        assertAll(
                () -> assertTrue(
                        chatMessageStore.findById(message1.getId()).isEmpty()
                ),
                () -> assertTrue(
                        chatMessageStore.findById(message2.getId()).isPresent()
                )
        );
    }

    @Test
    @DisplayName("빈 메시지는 생성할 수 없음")
    void createEmptyMessage() {
        // Given
        String content = "";

        // When
        BaseException exception = assertThrows(
                BaseException.class,
                () -> chatMessageService.create(
                        chatRoom,
                        ownerMember,
                        content
                )
        );

        // Then
        assertEquals(
                ChatMessageExceptionCode.INVALID_MESSAGE,
                exception.getExceptionCode()
        );
    }

    @Test
    @DisplayName("공백만 있는 메시지는 생성할 수 없음")
    void createBlankMessage() {
        // Given
        String content = "     ";

        // When
        BaseException exception = assertThrows(
                BaseException.class,
                () -> chatMessageService.create(
                        chatRoom,
                        ownerMember,
                        content
                )
        );

        // Then
        assertEquals(
                ChatMessageExceptionCode.INVALID_MESSAGE,
                exception.getExceptionCode()
        );
    }

    @Test
    @DisplayName("종료된 채팅방에는 메시지를 생성할 수 없음")
    void createEndedRoomMessage() {
        // Given
        chatRoom.end();

        // When
        BaseException exception = assertThrows(
                BaseException.class,
                () -> chatMessageService.create(
                        chatRoom,
                        ownerMember,
                        "종료된 채팅방에 메시지 전송"
                )
        );

        // Then
        assertEquals(
                ChatRoomExceptionCode.ENDED,
                exception.getExceptionCode()
        );

        assertTrue(
                chatMessageService.findAllByChatRoom(chatRoom).isEmpty()
        );
    }

    @Test
    @DisplayName("다른 채팅방의 멤버는 메시지를 생성할 수 없음")
    void createForeignMemberMessage() {
        // Given
        User otherOwner = UserFixtures.createUser(authService, "2");
        Bubble otherBubble = BubbleFixtures.createOpenBubble(otherOwner);

        ChatRoom otherRoom = chatRoomService.create(otherBubble);

        ChatMember otherMember = chatMemberService
                .findByChatRoomAndUserAndIsActive(otherRoom, otherOwner);

        // When
        BaseException exception = assertThrows(
                BaseException.class,
                () -> chatMessageService.create(
                        chatRoom,
                        otherMember,
                        "다른 채팅방의 메시지"
                )
        );

        // Then
        assertEquals(
                ChatMessageExceptionCode.FORBIDDEN,
                exception.getExceptionCode()
        );
    }
}