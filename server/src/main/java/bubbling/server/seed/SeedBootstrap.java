package bubbling.server.seed;

import bubbling.server.domain.Bubble;
import bubbling.server.domain.User;
import bubbling.server.service.bubble.BubbleService;
import bubbling.server.service.chat.ChatMemberService;
import bubbling.server.service.chat.ChatMessageService;
import bubbling.server.service.chat.ChatRequestService;
import bubbling.server.service.chat.ChatRoomService;
import bubbling.server.service.user.UserService;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SeedBootstrap {

    public void load(
            UserService userService,
            BubbleService bubbleService,
            ChatRoomService chatRoomService,
            ChatMemberService chatMemberService,
            ChatRequestService chatRequestService,
            ChatMessageService chatMessageService
    ) {
        Properties properties = new Properties();

        try (InputStream input = getClass().getResourceAsStream("/seed.properties")) {
            if (input == null) {
                return;
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("더미 데이터 설정 파일 로딩 실패", e);
        }

        if (!Boolean.parseBoolean(properties.getProperty("seed.enabled", "false"))) {
            return;
        }

        Map<String, User> users = new HashMap<>();
        if (Boolean.parseBoolean(properties.getProperty("seed.users", "false"))) {
            users = new UserSeedLoader().loadUsers(userService);
        }

        Map<String, Bubble> bubbles = new HashMap<>();
        if (Boolean.parseBoolean(properties.getProperty("seed.bubbles", "false"))) {
            bubbles = new BubbleSeedLoader().loadBubbles(users, bubbleService);
        }

        if (Boolean.parseBoolean(properties.getProperty("seed.chats", "false"))) {
            new ChatSeedLoader().loadChats(
                    users,
                    bubbles,
                    chatRoomService,
                    chatMemberService,
                    chatRequestService,
                    chatMessageService
            );
        }
    }
}