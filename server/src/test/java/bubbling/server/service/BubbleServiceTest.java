package bubbling.server.service;

import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleMethod;
import bubbling.common.enums.ChatRoomStatus;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.ChatMember;
import bubbling.server.domain.ChatRoom;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.security.PasswordService;
import bubbling.server.store.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BubbleServiceTest {

    private UserStore userStore;
    private BubbleStore bubbleStore;
    private ChatRoomStore chatRoomStore;
    private ChatMemberStore chatMemberStore;
    private ChatMessageStore chatMessageStore;
    private ChatRequestStore chatRequestStore;

    private UserService userService;
    private AuthService authService;
    private BubbleService bubbleService;
    private ChatMemberService chatMemberService;
    private ChatMessageService chatMessageService;

    private static PasswordService passwordService;

    @BeforeAll
    static void setUpAll() {
        passwordService = new PasswordService();
    }

    @BeforeEach
    void setUp() {
        userStore = new UserStore();
        bubbleStore = new BubbleStore();
        chatRoomStore = new ChatRoomStore();
        chatMemberStore = new ChatMemberStore();
        chatMessageStore = new ChatMessageStore();

        userService = new UserService(userStore);
        authService = new AuthService(userService, passwordService);
        chatMemberService = new ChatMemberService(chatMemberStore);
        chatMessageService = new ChatMessageService(chatMessageStore);
        chatRequestStore = new ChatRequestStore();

        ChatMemberService chatMemberService = new ChatMemberService(chatMemberStore);
        ChatRoomService chatRoomService =
                new ChatRoomService(
                        chatRoomStore,
                        chatMessageService,
                        chatMemberService,
                        chatRequestStore
                );

        bubbleService = new BubbleService(
                bubbleStore,
                chatRoomService
        );
    }

    private User createTestUser(String id) {
        return authService.register(
                "testUser"+1,
                    "password"+id+"!",
                "테스트유저"+id
        );
    }

    @Test
    @DisplayName("비눗방울을 생성하면 Store에 저장")
    void createBubbleSuccess() {
        // Given
        User owner = createTestUser("1");

        // When
        Bubble bubble = bubbleService.create(
                owner,
                "진로 고민이 있어요",
                BubbleCategory.CAREER,
                BubbleMethod.LISTEN,
                4,
                true,
                30
        );

        // Then
        // 1. 비눗방울 저장 확인
        Bubble savedBubble = bubbleStore.findById(bubble.getId())
                .orElseThrow();

        assertSame(bubble, savedBubble);

        // 2. 채팅방 생성 확인
        ChatRoom chatRoom = chatRoomStore
                .findByBubble(bubble)
                .orElseThrow();

        assertNotNull(chatRoom);
        assertEquals(bubble, chatRoom.getBubble());
        assertEquals(ChatRoomStatus.ACTIVE, chatRoom.getStatus());

        // 3. 방장 멤버 생성 확인
        ChatMember ownerMember = chatMemberService
                .findByChatRoomAndUserAndIsActive(chatRoom, owner);

        assertNotNull(ownerMember);
        assertEquals(owner, ownerMember.getUser());
        assertEquals(chatRoom, ownerMember.getChatRoom());
        assertTrue(ownerMember.isActive());

        // 4. 초기 참여 인원 확인
        int memberCount = chatMemberService
                .countAllActiveJoinMemberByChatRoom(chatRoom);

        assertEquals(1, memberCount);
    }

    @Test
    void findAllByAlive() {

    }

    @Test
    @DisplayName("비눗방울 수정 성공")
    void updateSuccess() {
        // Given
        User owner = createTestUser("2");
        Bubble bubble = bubbleService.create(
                owner,
                "진로 고민이 있어요",
                BubbleCategory.CAREER,
                BubbleMethod.LISTEN,
                4,
                true,
                30
        );
        Bubble updatedBubble = Bubble.create(
                owner,
                "사실 고민 없어요",
                BubbleCategory.ETC,
                BubbleMethod.SOLUTION,
                4,
                false,
                30
        );

        // When
        bubbleService.update(
                owner,
                bubble.getId(),
                "사실 고민 없어요",
                BubbleCategory.ETC,
                BubbleMethod.SOLUTION,
                4,
                false
        );

        // Then
        assertEquals(bubble.getTitle(), updatedBubble.getTitle());
        assertEquals(bubble.getCategory(), updatedBubble.getCategory());
        assertEquals(bubble.getMethod(), updatedBubble.getMethod());
        assertEquals(bubble.getMaxMemberCnt(), updatedBubble.getMaxMemberCnt());
        assertEquals(bubble.getIsOpen(), updatedBubble.getIsOpen());
    }

    @Test
    @DisplayName("만료 시각 지난 비눗방울 만료")
    void expireElapsed() {
        // Given
        User owner = createTestUser("3");
        Bubble bubble = bubbleService.create(
                owner,
                "진로 고민이 있어요",
                BubbleCategory.CAREER,
                BubbleMethod.LISTEN,
                4,
                true,
                0
        );

        // When
    }

    @Test
    @DisplayName("최초의 리액션")
    void reactSuccess() {
        // Given
        User owner = createTestUser("4");
        Bubble bubble = bubbleService.create(
                owner,
                "reaction_test_title",
                BubbleCategory.ETC,
                BubbleMethod.SOLUTION,
                4,
                true,
                60
        );

        // When
        int cnt = 3;
        User reactor = createTestUser("5");
        for(int i = 0; i < cnt; i++) {
            bubbleService.react(
                    reactor,
                    bubble.getId()
            );
        }

        // Then
        assertEquals(bubble.getReactionCnt(), cnt);
    }

    @Test
    @DisplayName("비눗방울 소유자의 리액션")
    void reactOwner() {
        // Given
        User owner = createTestUser("6");
        Bubble bubble = bubbleService.create(
                owner,
                "reaction_test_title",
                BubbleCategory.ETC,
                BubbleMethod.SOLUTION,
                4,
                true,
                60
        );

        // When & Then
        assertThrows(
                BaseException.class,
                () -> bubbleService.react(
                        owner,
                        bubble.getId()
                )
        );
    }

    @Test
    @DisplayName("비눗방울 삭제 성공 후 채팅방, 메세지, 멤버까지 전부 삭제")
    void deleteSuccess() {
        // Given
        User owner = createTestUser("7");
        Bubble bubble = bubbleService.create(
                owner,
                "진로 고민이 있어요",
                BubbleCategory.CAREER,
                BubbleMethod.LISTEN,
                4,
                true,
                30
        );

        // When
        bubbleService.delete(owner, bubble.getId());

        // Then
        ChatRoom chatRoom = chatRoomStore
                .findByBubble(bubble)
                .orElse(null);

        // 3. 방장 멤버 생성 확인
        ChatMember ownerMember = chatMemberService
                .findByChatRoomAndUserAndIsActive(chatRoom, owner);

        assertThrows(
                BaseException.class,
                () -> bubbleService.findById(bubble.getId()));
    }
}