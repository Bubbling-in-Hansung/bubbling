package bubbling.server.service;

import bubbling.server.constant.RandomNickname;
import bubbling.server.domain.ChatMember;
import bubbling.server.domain.ChatRoom;
import bubbling.server.domain.User;
import bubbling.server.store.ChatMemberStore;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ChatMemberService {

    private final ChatMemberStore store;
    private final RandomNickname randomNickname = new RandomNickname();

    public ChatMemberService(ChatMemberStore store) {
        this.store = store;
    }

    public ChatMember create(User user, ChatRoom chatRoom) {
        Set<String> originSet = store.findAllByChatRoomAndIsActive(chatRoom).stream()
                .map(ChatMember::getRandomNickname).collect(Collectors.toSet());;

        String newNickname;
        while(true) {
            newNickname = randomNickname.getRandomNickname();
            if(!originSet.contains(newNickname)) {
                break;
            }
        }

        ChatMember chatMember = ChatMember.create(
                user,
                chatRoom,
                newNickname
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
