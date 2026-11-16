package bubbling.server.service;

import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.BubbleMethod;
import bubbling.common.enums.ChatRoomStatus;
import bubbling.server.domain.Bubble;
import bubbling.server.domain.ChatMember;
import bubbling.server.domain.ChatRoom;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.fixture.BubbleFixtures;
import bubbling.server.fixture.UserFixtures;
import bubbling.server.security.PasswordService;
import bubbling.server.store.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BubbleServiceTest {

    private UserStore userStore;
    private BubbleStore bubbleStore;
    private ChatRoomStore chatRoomStore;
    private ChatMemberStore chatMemberStore;
    private ChatMessageStore chatMessageStore;
    private ChatRequestStore chatRequestStore;

    private AuthService authService;
    private BubbleService bubbleService;
    private ChatMemberService chatMemberService;
    private ChatRequestService chatRequestService;

    private static PasswordService passwordService;

    @BeforeAll
    static void setUpAll() {
        passwordService = new PasswordService();
    }

    @BeforeEach
    void setUp() {
        // Store
        userStore = new UserStore();
        bubbleStore = new BubbleStore();
        chatRoomStore = new ChatRoomStore();
        chatMemberStore = new ChatMemberStore();
        chatMessageStore = new ChatMessageStore();
        chatRequestStore = new ChatRequestStore();

        // Service
        UserService userService = new UserService(userStore);
        authService = new AuthService(userService, passwordService);

        chatMemberService = new ChatMemberService(chatMemberStore);

        ChatMessageService chatMessageService = new ChatMessageService(chatMessageStore);

        ChatRoomService chatRoomService = new ChatRoomService(
                chatRoomStore,
                chatMessageService,
                chatMemberService,
                chatRequestStore
        );

        chatRequestService = new ChatRequestService(
                chatRequestStore,
                chatRoomService,
                chatMemberService
        );

        bubbleService = new BubbleService(
                bubbleStore,
                chatRoomService,
                chatRequestService);
    }

    @Test
    @DisplayName("비눗방울을 생성하면 Store에 저장")
    void createBubbleSuccess() {
        // Given
        User owner = UserFixtures.createUser(authService,"1");

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
    @DisplayName("활성 비눗방울이 존재할 때 비눗방울 생성 시도")
    void createDuplicateBubble() {
        // Given
        User owner = UserFixtures.createUser(authService,"1");

        // When
        Bubble origin = BubbleFixtures.createOpenBubble(owner);
        bubbleService.create(
                owner,
                origin.getTitle(),
                origin.getCategory(),
                origin.getMethod(),
                origin.getMaxMemberCnt(),
                origin.getIsOpen(),
                30
        );
        Bubble dup = BubbleFixtures.createOpenBubble(owner);

        // Then
        assertThrows(BaseException.class, () -> bubbleService.create(
                owner,
                dup.getTitle(),
                dup.getCategory(),
                dup.getMethod(),
                dup.getMaxMemberCnt(),
                dup.getIsOpen(),
                30
        ));
    }

    @Test
    @DisplayName("활성 상태의 비눗방울만 조회")
    void findAllByAlive() {
        // Given
        User owner1 = UserFixtures.createUser(authService,"1");
        User owner2 = UserFixtures.createUser(authService,"2");

        Bubble aliveBubble = BubbleFixtures.createOpenBubble(owner1);
        Bubble expiredBubble = BubbleFixtures.createOpenBubble(owner2);

        aliveBubble = bubbleService.create(
                owner1,
                aliveBubble.getTitle(),
                aliveBubble.getCategory(),
                aliveBubble.getMethod(),
                aliveBubble.getMaxMemberCnt(),
                aliveBubble.getIsOpen(),
                30
        );
        expiredBubble = bubbleService.create(
                owner2,
                expiredBubble.getTitle(),
                expiredBubble.getCategory(),
                expiredBubble.getMethod(),
                expiredBubble.getMaxMemberCnt(),
                expiredBubble.getIsOpen(),
                30
        );

        expiredBubble.close();

        // When
        List<Bubble> bubbles = bubbleService.findAllByAlive();

        // Then
        assertEquals(1, bubbles.size());
        assertTrue(bubbles.contains(aliveBubble));
        assertFalse(bubbles.contains(expiredBubble));
    }

    @Test
    @DisplayName("비눗방울 수정 성공")
    void updateSuccess() {
        // Given
        User owner = UserFixtures.createUser(authService,"1");
        Bubble bubble = BubbleFixtures.createOpenBubble(owner);
        bubble = bubbleService.create(
                owner,
                bubble.getTitle(),
                bubble.getCategory(),
                bubble.getMethod(),
                bubble.getMaxMemberCnt(),
                bubble.getIsOpen(),
                30
        );

        // When
        bubbleService.update(
                owner,
                bubble.getId(),
                "사실 고민 없어요",
                BubbleCategory.ETC,
                BubbleMethod.SOLUTION,
                2,
                false
        );

        // Then
        Bubble updatedBubble = bubbleStore.findById(bubble.getId())
                .orElseThrow();

        assertSame(bubble, updatedBubble);

        assertAll(
                () -> assertEquals("사실 고민 없어요", updatedBubble.getTitle()),
                () -> assertEquals(BubbleCategory.ETC, updatedBubble.getCategory()),
                () -> assertEquals(BubbleMethod.SOLUTION, updatedBubble.getMethod()),
                () -> assertEquals(2, updatedBubble.getMaxMemberCnt()),
                () -> assertFalse(updatedBubble.getIsOpen())
        );
    }

    @Test
    @DisplayName("서로 다른 사용자 3명이 공감하면 공감 수가 3 증가")
    void reactSuccess() {
        // Given
        User owner = UserFixtures.createUser(authService,"1");

        Bubble bubble = BubbleFixtures.createOpenBubble(owner);
        bubble = bubbleService.create(
                owner,
                bubble.getTitle(),
                bubble.getCategory(),
                bubble.getMethod(),
                bubble.getMaxMemberCnt(),
                bubble.getIsOpen(),
                30
        );

        User reactor1 = UserFixtures.createUser(authService,"r1");
        User reactor2 = UserFixtures.createUser(authService,"r2");
        User reactor3 = UserFixtures.createUser(authService,"r3");

        // When
        bubbleService.react(reactor1, bubble.getId());
        bubbleService.react(reactor2, bubble.getId());
        bubbleService.react(reactor3, bubble.getId());

        // Then
        assertEquals(3, bubble.getReactionCnt());
    }

    @Test
    @DisplayName("비눗방울 소유자는 자신의 비눗방울에 공감할 수 없다")
    void reactOwner() {
        // Given
        User owner = UserFixtures.createUser(authService, "1");

        Bubble origin = BubbleFixtures.createOpenBubble(owner);
        Bubble bubble = bubbleService.create(
                owner,
                origin.getTitle(),
                origin.getCategory(),
                origin.getMethod(),
                origin.getMaxMemberCnt(),
                origin.getIsOpen(),
                30
        );

        // When
        BaseException exception = assertThrows(
                BaseException.class,
                () -> bubbleService.react(owner, bubble.getId())
        );

        // Then
        assertEquals(0, bubble.getReactionCnt());
    }

    @Test
    @DisplayName("비눗방울 삭제 시 채팅방과 방장 멤버도 삭제")
    void deleteSuccess() {
        // Given
        User owner = UserFixtures.createUser(authService,"1");
        Bubble bubble = BubbleFixtures.createOpenBubble(owner);
        bubble = bubbleService.create(
                owner,
                bubble.getTitle(),
                bubble.getCategory(),
                bubble.getMethod(),
                bubble.getMaxMemberCnt(),
                bubble.getIsOpen(),
                30
        );

        ChatRoom chatRoom = chatRoomStore.findByBubble(bubble)
                .orElseThrow();

        ChatMember ownerMember = chatMemberService
                .findByChatRoomAndUserAndIsActive(chatRoom, owner);

        assertNotNull(ownerMember);

        String bubbleId = bubble.getId();
        String chatRoomId = chatRoom.getId();
        String chatMemberId = ownerMember.getId();

        // When
        bubbleService.delete(owner, bubbleId);

        // Then
        assertAll(
                () -> assertTrue(bubbleStore.findById(bubbleId).isEmpty()),
                () -> assertTrue(chatRoomStore.findById(chatRoomId).isEmpty()),
                () -> assertTrue(chatMemberStore.findById(chatMemberId).isEmpty()),
                () -> assertEquals(0, chatRequestStore.findAll().size())
        );
    }
}