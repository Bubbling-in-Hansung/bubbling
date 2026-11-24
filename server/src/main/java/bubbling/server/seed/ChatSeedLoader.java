package bubbling.server.seed;

import bubbling.common.codec.JsonCodec;
import bubbling.server.domain.*;
import bubbling.server.service.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ChatSeedLoader {

    private final JsonCodec jsonCodec = new JsonCodec();

    private record SeedChat(
            String bubbleKey,
            List<String> participants,
            List<SeedMessage> messages,
            boolean ended
    ) {}

    private record SeedMessage(
            String senderLoginId,
            String content
    ) {}

    public void loadChats(
            Map<String, User> users,
            Map<String, Bubble> bubbles,
            ChatRoomService chatRoomService,
            ChatMemberService chatMemberService,
            ChatRequestService chatRequestService,
            ChatMessageService chatMessageService
    ) {
        try (InputStream input = getClass().getResourceAsStream("/seed/chats.json")) {
            if (input == null) {
                throw new IllegalStateException("chats.json 파일을 찾을 수 없습니다.");
            }

            String json = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            SeedChat[] chats = jsonCodec.fromPayload(
                    json,
                    SeedChat[].class
            );

            if (chats == null) {
                throw new IllegalStateException("채팅 테스트 데이터가 없습니다.");
            }

            Set<String> loadedKeys = new HashSet<>();
            for (SeedChat data : chats) {
                if (!loadedKeys.add(data.bubbleKey())) {
                    throw new IllegalStateException("중복된 채팅 데이터: " + data.bubbleKey());
                }

                Bubble bubble = bubbles.get(data.bubbleKey());
                if (bubble == null) {
                    throw new IllegalStateException("존재하지 않는 비눗방울: " + data.bubbleKey());
                }

                ChatRoom chatRoom = chatRoomService.findByBubble(bubble);

                List<String> participants = data.participants() == null
                        ? List.of()
                        : data.participants();

                List<SeedMessage> messages = data.messages() == null
                        ? List.of()
                        : data.messages();

                if (!bubble.isAlive() && (!participants.isEmpty() || data.ended())) {
                    throw new IllegalStateException("만료된 비눗방울은 신규 참여 또는 종료 처리를 할 수 없습니다: " + data.bubbleKey());
                }

                // 비눗방울 생성 시 자동 등록된 방장 포함
                Map<String, ChatMember> members = new HashMap<>();
                for (ChatMember member : chatMemberService.findAllActiveJoinMemberByChatRoom(chatRoom)) {
                    members.put(
                            member.getUser().getLoginId(),
                            member
                    );
                }

                // 채팅방 참여자 등록
                for (String loginId : participants) {
                    User user = users.get(loginId);
                    if (user == null) {
                        throw new IllegalStateException("존재하지 않는 테스트 사용자: " + loginId);
                    }

                    if (members.containsKey(loginId)) {
                        throw new IllegalStateException("이미 참여한 사용자: " + loginId);
                    }

                    ChatMember member;
                    if (bubble.getIsOpen()) {
                        // 공개 채팅방
                        member = chatRoomService.join(user, bubble);
                    } else {
                        // 비공개 채팅방: 참여 요청 및 승인
                        ChatRequest request = chatRequestService.create(user, bubble);

                        member = chatRequestService.accept(
                                bubble.getOwner(),
                                request.getId()
                        );
                    }

                    members.put(loginId, member);
                }

                // 채팅 메시지 생성
                for (SeedMessage dataMessage : messages) {
                    ChatMember sender = members.get(dataMessage.senderLoginId());
                    if (sender == null) {
                        throw new IllegalStateException("채팅방에 참여하지 않은 메시지 작성자: " + dataMessage.senderLoginId());
                    }

                    chatMessageService.create(
                            chatRoom,
                            sender,
                            dataMessage.content()
                    );
                }

                // 채팅방 종료
                if (data.ended()) {
                    chatRoomService.end(
                            bubble.getOwner(),
                            chatRoom.getId()
                    );
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("채팅 테스트 데이터 로딩 실패", e);
        }
    }
}