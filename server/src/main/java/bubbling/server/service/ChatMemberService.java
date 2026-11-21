package bubbling.server.service;

import bubbling.server.constant.RandomNickname;
import bubbling.server.domain.ChatMember;
import bubbling.server.domain.ChatRoom;
import bubbling.server.domain.User;
import bubbling.server.store.ChatMemberStore;

import java.util.List;

public class ChatMemberService {

    private final ChatMemberStore store;
    private final RandomNickname randomNickname = new RandomNickname();

    public ChatMemberService(ChatMemberStore store) {
        this.store = store;
    }

    public ChatMember create(User user, ChatRoom chatRoom) {
        ChatMember chatMember = ChatMember.create(
                user,
                chatRoom,
                randomNickname.getRandomNickname()
        );

        return store.save(chatMember);
    }

    public ChatMember findByChatRoomAndUserAndIsActive(ChatRoom chatRoom, User user) {
        return store.findByChatRoomAndUserAndIsActive(chatRoom, user)
                .orElse(null);
    }

    public List<ChatMember> findAllActiveJoinMemberByChatRoom(ChatRoom chatRoom) {
        return store.findAllByChatRoomAndIsActive(chatRoom);
    }

    public int countAllActiveJoinMemberByChatRoom(ChatRoom chatRoom) {
        return findAllActiveJoinMemberByChatRoom(chatRoom).size();
    }

    public void deleteAllByChatRoom(ChatRoom chatRoom) {
        List<ChatMember> chatMembers = store.findAllByChatRoom(chatRoom);
        store.deleteAll(chatMembers.stream().map(ChatMember::getId).toList());
    }

    public List<ChatMember> findAllActiveJoinMemberByUser(User user) {
        return store.findAllByUserAndIsActive(user);
    }
}
