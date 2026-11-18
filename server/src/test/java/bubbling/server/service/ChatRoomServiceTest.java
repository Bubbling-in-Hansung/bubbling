package bubbling.server.service;

import bubbling.common.enums.*;
import bubbling.server.domain.*;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.*;
import bubbling.server.fixture.BubbleFixtures;
import bubbling.server.fixture.UserFixtures;
import bubbling.server.security.PasswordService;
import bubbling.server.store.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatRoomServiceTest {

    private ChatRoomStore chatRoomStore;
    private ChatMemberStore chatMemberStore;
    private ChatMessageStore chatMessageStore;

    private AuthService authService;
    private ChatRoomService chatRoomService;
    private ChatMemberService chatMemberService;
    private ChatMessageService chatMessageService;

    @BeforeEach
    void setUp() {
        // Store
        UserStore userStore = new UserStore();
        chatRoomStore = new ChatRoomStore();
        chatMemberStore = new ChatMemberStore();
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
    }

    @Test
    @DisplayName("채팅방 생성 시 방장 멤버도 함께 생성")
    void create() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");
        Bubble bubble = BubbleFixtures.createOpenBubble(owner);

        // When
        ChatRoom chatRoom = chatRoomService.create(bubble);

        // Then
        ChatRoom savedRoom = chatRoomStore.findById(chatRoom.getId())
                .orElseThrow();

        ChatMember ownerMember = chatMemberService
                .findByChatRoomAndUserAndIsActive(chatRoom, owner);

        assertAll(
                () -> assertSame(chatRoom, savedRoom),
                () -> assertEquals(bubble, chatRoom.getBubble()),
                () -> assertEquals(ChatRoomStatus.ACTIVE, chatRoom.getStatus()),
                () -> assertNotNull(ownerMember),
                () -> assertTrue(ownerMember.isActive()),
                () -> assertEquals(
                        1,
                        chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom)
                )
        );
    }

    @Test
    @DisplayName("채팅방 삭제 시 멤버와 메시지도 함께 삭제")
    void deleteByBubble() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");
        Bubble bubble = BubbleFixtures.createOpenBubble(owner);

        ChatRoom chatRoom = chatRoomService.create(bubble);

        ChatMember ownerMember = chatMemberService
                .findByChatRoomAndUserAndIsActive(chatRoom, owner);

        ChatMessage chatMessage = chatMessageService.create(
                chatRoom,
                ownerMember,
                "테스트 메시지입니다."
        );

        String roomId = chatRoom.getId();
        String memberId = ownerMember.getId();
        String messageId = chatMessage.getId();

        // When
        chatRoomService.deleteByBubble(bubble);

        // Then
        assertAll(
                () -> assertTrue(chatRoomStore.findById(roomId).isEmpty()),
                () -> assertTrue(chatMemberStore.findById(memberId).isEmpty()),
                () -> assertTrue(chatMessageStore.findById(messageId).isEmpty())
        );
    }

    @Test
    @DisplayName("공개 채팅방에 정상적으로 참여")
    void join() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");
        User participant = UserFixtures.createUser(authService, "2");

        Bubble bubble = BubbleFixtures.createOpenBubble(owner);
        ChatRoom chatRoom = chatRoomService.create(bubble);

        // When
        ChatMember chatMember = chatRoomService.join(
                participant,
                bubble
        );

        // Then
        assertAll(
                () -> assertNotNull(chatMember),
                () -> assertEquals(participant, chatMember.getUser()),
                () -> assertEquals(chatRoom, chatMember.getChatRoom()),
                () -> assertTrue(chatMember.isActive()),
                () -> assertEquals(
                        2,
                        chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom)
                )
        );
    }

    @Test
    @DisplayName("비공개 채팅방은 승인 없이 참여 불가")
    void joinWithoutApproval() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");
        User participant = UserFixtures.createUser(authService, "2");

        Bubble bubble = BubbleFixtures.createPrivateBubble(owner);
        ChatRoom chatRoom = chatRoomService.create(bubble);

        // When
        BaseException exception = assertThrows(
                BaseException.class,
                () -> chatRoomService.join(participant, bubble)
        );

        // Then
        assertEquals(
                ChatRoomExceptionCode.APPROVAL_REQUIRED,
                exception.getExceptionCode()
        );

        assertEquals(
                1,
                chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom)
        );
    }

    @Test
    @DisplayName("비공개 채팅방은 승인된 경우 참여 가능")
    void joinWithApproval() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");
        User participant = UserFixtures.createUser(authService, "2");

        Bubble bubble = BubbleFixtures.createPrivateBubble(owner);
        ChatRoom chatRoom = chatRoomService.create(bubble);

        // When
        ChatMember chatMember = chatRoomService.join(
                participant,
                bubble,
                true
        );

        // Then
        assertAll(
                () -> assertNotNull(chatMember),
                () -> assertTrue(chatMember.isActive()),
                () -> assertEquals(
                        2,
                        chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom)
                )
        );
    }

    @Test
    @DisplayName("최대 인원에 도달한 채팅방은 추가 참여 불가")
    void joinFull() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");
        User participant1 = UserFixtures.createUser(authService, "2");
        User participant2 = UserFixtures.createUser(authService, "3");

        Bubble bubble = BubbleFixtures.createBubbleWithMaxMemberCnt(owner, 2);
        ChatRoom chatRoom = chatRoomService.create(bubble);

        chatRoomService.join(participant1, bubble);

        // When
        BaseException exception = assertThrows(
                BaseException.class,
                () -> chatRoomService.join(participant2, bubble)
        );

        // Then
        assertEquals(
                ChatRoomExceptionCode.FULL,
                exception.getExceptionCode()
        );

        assertEquals(
                2,
                chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom)
        );
    }

    @Test
    @DisplayName("이미 참여 중인 사용자는 중복 참여할 수 없음")
    void joinAlreadyJoined() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");
        User participant = UserFixtures.createUser(authService, "2");

        Bubble bubble = BubbleFixtures.createOpenBubble(owner);
        ChatRoom chatRoom = chatRoomService.create(bubble);

        chatRoomService.join(participant, bubble);

        // When
        BaseException exception = assertThrows(
                BaseException.class,
                () -> chatRoomService.join(participant, bubble)
        );

        // Then
        assertEquals(
                ChatMemberExceptionCode.ALREADY_JOINED,
                exception.getExceptionCode()
        );

        assertEquals(
                2,
                chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom)
        );
    }

    @Test
    @DisplayName("일반 참여자는 채팅방에서 퇴장 가능")
    void leave() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");
        User participant = UserFixtures.createUser(authService, "2");

        Bubble bubble = BubbleFixtures.createOpenBubble(owner);
        ChatRoom chatRoom = chatRoomService.create(bubble);

        ChatMember chatMember = chatRoomService.join(
                participant,
                bubble
        );

        // When
        ChatMember leftMember = chatRoomService.leave(
                participant,
                chatRoom
        );

        // Then
        assertAll(
                () -> assertSame(chatMember, leftMember),
                () -> assertFalse(leftMember.isActive()),
                () -> assertNotNull(leftMember.getLeftAt()),
                () -> assertEquals(
                        1,
                        chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom)
                ),
                () -> assertTrue(
                        chatMemberStore.findById(chatMember.getId()).isPresent()
                )
        );
    }

    @Test
    @DisplayName("퇴장한 사용자는 다시 참여할 수 있음")
    void rejoinAfterLeave() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");
        User participant = UserFixtures.createUser(authService, "2");

        Bubble bubble = BubbleFixtures.createOpenBubble(owner);
        ChatRoom chatRoom = chatRoomService.create(bubble);

        ChatMember firstMember = chatRoomService.join(
                participant,
                bubble
        );

        chatRoomService.leave(participant, chatRoom);

        // When
        ChatMember rejoinedMember = chatRoomService.join(
                participant,
                bubble
        );

        // Then
        assertAll(
                () -> assertNotEquals(
                        firstMember.getId(),
                        rejoinedMember.getId()
                ),
                () -> assertFalse(firstMember.isActive()),
                () -> assertTrue(rejoinedMember.isActive()),
                () -> assertEquals(
                        2,
                        chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom)
                )
        );
    }

    @Test
    @DisplayName("방장은 채팅방에서 퇴장할 수 없음")
    void leaveOwner() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");

        Bubble bubble = BubbleFixtures.createOpenBubble(owner);
        ChatRoom chatRoom = chatRoomService.create(bubble);

        // When
        BaseException exception = assertThrows(
                BaseException.class,
                () -> chatRoomService.leave(owner, chatRoom)
        );

        // Then
        assertEquals(
                ChatMemberExceptionCode.CANNOT_LEFT_OWNER,
                exception.getExceptionCode()
        );

        assertEquals(
                1,
                chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom)
        );
    }

    @Test
    @DisplayName("방장이 채팅방을 종료하면 비눗방울도 만료")
    void end() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");

        Bubble bubble = BubbleFixtures.createOpenBubble(owner);
        ChatRoom chatRoom = chatRoomService.create(bubble);

        // When
        ChatRoom endedRoom = chatRoomService.end(
                owner,
                chatRoom.getId()
        );

        // Then
        assertAll(
                () -> assertSame(chatRoom, endedRoom),
                () -> assertEquals(ChatRoomStatus.ENDED, chatRoom.getStatus()),
                () -> assertNotNull(chatRoom.getEndedAt()),
                () -> assertEquals(BubbleStatus.EXPIRED, bubble.getStatus())
        );
    }

    @Test
    @DisplayName("방장이 아닌 사용자는 채팅방을 종료할 수 없음")
    void endNotOwner() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");
        User participant = UserFixtures.createUser(authService, "2");

        Bubble bubble = BubbleFixtures.createOpenBubble(owner);
        ChatRoom chatRoom = chatRoomService.create(bubble);

        chatRoomService.join(participant, bubble);

        // When
        BaseException exception = assertThrows(
                BaseException.class,
                () -> chatRoomService.end(
                        participant,
                        chatRoom.getId()
                )
        );

        // Then
        assertEquals(
                ChatRoomExceptionCode.FORBIDDEN,
                exception.getExceptionCode()
        );

        assertAll(
                () -> assertEquals(ChatRoomStatus.ACTIVE, chatRoom.getStatus()),
                () -> assertEquals(BubbleStatus.ALIVE, bubble.getStatus())
        );
    }

    @Test
    @DisplayName("종료된 채팅방에는 참여할 수 없음")
    void joinEndedRoom() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");
        User participant = UserFixtures.createUser(authService, "2");

        Bubble bubble = BubbleFixtures.createOpenBubble(owner);
        ChatRoom chatRoom = chatRoomService.create(bubble);

        chatRoomService.end(owner, chatRoom.getId());

        // When
        BaseException exception = assertThrows(
                BaseException.class,
                () -> chatRoomService.join(participant, bubble)
        );

        // Then
        assertEquals(
                ChatRoomExceptionCode.ENDED,
                exception.getExceptionCode()
        );

        assertEquals(
                1,
                chatMemberService.countAllActiveJoinMemberByChatRoom(chatRoom)
        );
    }
}