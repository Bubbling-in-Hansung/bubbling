package bubbling.client.ui;

import bubbling.client.ui.model.*;
import bubbling.client.ui.screen.ChatScreen;
import bubbling.common.dto.chat.ChatEndResponse;
import bubbling.common.dto.chat.ChatMessageDto;
import bubbling.common.enums.BubbleCategory;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ChatScreenTest {
 private static List<JTextArea> textAreas(Container root,String text){
  List<JTextArea> found=new ArrayList<>();
  for(Component child:root.getComponents()){
   if(child instanceof JTextArea area&&text.equals(area.getText()))found.add(area);
   if(child instanceof Container nested)found.addAll(textAreas(nested,text));
  }
  return found;
 }
 private static RoomView room(UiSession session, String id, String title){
  var bubble = ServerRoomFixtures.bubble(id, title, BubbleCategory.DAILY, "2099-01-01T00:00:00");
  return session.applyChatRoom(ServerRoomFixtures.chat(bubble)).view;
 }
 private static void receive(UiSession session, String id, String text) {
  session.applyMessage(new ChatMessageDto(id, "chat-first", "me", "테스터", text, LocalDateTime.now().toString()));
 }
 @Test void repeatedRefreshReusesHistoryAndSpeechAndNewMessagesAppend() throws Exception{
  SwingUtilities.invokeAndWait(()->{
   UiData data=new UiData((type, payload) -> {});
   UiSession session=data.session();
   RoomView first=room(session, "first", "첫 방"),second=room(session, "second", "둘째 방");
   ChatScreen screen=new ChatScreen(data,()->{});
   receive(session, "first-message", "첫 메시지");
   screen.showRoom(first);
   var initial=textAreas(screen,"첫 메시지");assertEquals(2,initial.size());
   screen.showRoom(first);
   var unchanged=textAreas(screen,"첫 메시지");
   assertEquals(2,unchanged.size());
   for(var area:initial)assertTrue(unchanged.stream().anyMatch(value->value==area));
   receive(session, "second-message", "둘째 메시지");screen.showRoom(first);
   for(var area:initial)assertTrue(textAreas(screen,"첫 메시지").stream().anyMatch(value->value==area));
   assertEquals(2,textAreas(screen,"둘째 메시지").size());
   screen.showRoom(second);assertTrue(textAreas(screen,"첫 메시지").isEmpty());
   screen.showRoom(first);assertEquals(2,textAreas(screen,"첫 메시지").size());
   session.markEnded(new ChatEndResponse("chat-first", "first"));screen.showRoom(first);
   assertEquals(1,textAreas(screen,"첫 메시지").size());
  });
 }
}
