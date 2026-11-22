package bubbling.client.ui;

import bubbling.client.ui.model.*;
import bubbling.client.ui.screen.ChatScreen;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
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
 private static RoomView room(String title){
  return new RoomView(title,"일상","함께 이야기해요",1,4,false,false,true,0,.2,.2,120);
 }
 @Test void repeatedRefreshReusesHistoryAndSpeechAndNewMessagesAppend() throws Exception{
  SwingUtilities.invokeAndWait(()->{
   RoomView first=room("첫 방"),second=room("둘째 방");
   UiData data=new UiData(true,List.of(first,second));
   UiSession session=data.session();var state=session.find(first);
   session.send(state,"첫 메시지",true);
   ChatScreen screen=new ChatScreen(data,()->{});screen.showRoom(first);
   var initial=textAreas(screen,"첫 메시지");assertEquals(2,initial.size());
   screen.showRoom(first);
   var unchanged=textAreas(screen,"첫 메시지");
   assertEquals(2,unchanged.size());
   for(var area:initial)assertTrue(unchanged.stream().anyMatch(value->value==area));
   session.send(state,"둘째 메시지",true);screen.showRoom(first);
   assertTrue(textAreas(screen,"첫 메시지").stream().anyMatch(value->value==initial.getLast()));
   assertEquals(2,textAreas(screen,"둘째 메시지").size());
   screen.showRoom(second);assertTrue(textAreas(screen,"첫 메시지").isEmpty());
   screen.showRoom(first);assertEquals(2,textAreas(screen,"첫 메시지").size());
   session.end(state);screen.showRoom(first);
   assertEquals(2,textAreas(screen,"첫 메시지").size());
  });
 }
}