package bubbling.client.ui;
import bubbling.client.LiveSession;
import bubbling.client.ui.model.*;
import bubbling.common.dto.user.UserDto;
import bubbling.common.enums.BubbleCategory;
import bubbling.common.enums.ProfileCharacter;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ApplicationViewTest {
 private static java.util.List<JButton> buttons(Container root) {
  var result=new ArrayList<JButton>();
  for(Component c:root.getComponents()){if(c instanceof JButton b)result.add(b);if(c instanceof Container k)result.addAll(buttons(k));}
  return result;
 }
 private static void click(Container root,String text){buttons(root).stream().filter(b->text.equals(b.getText())).findFirst().orElseThrow().doClick();}
 @Test void homeFiltersServerBubblesAfterSignIn() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   var live = new LiveSession(message -> {});
   var data = new UiData(live::send);
   var view = new ApplicationView(data, live);
   data.session().applyBubble(ServerRoomFixtures.bubble("career", "진로 고민", BubbleCategory.CAREER, "2099-01-01T00:00:00"));
   data.session().applyBubble(ServerRoomFixtures.bubble("daily", "일상 고민", BubbleCategory.DAILY, "2099-01-01T00:00:00"));
   view.signedIn(new UserDto("user", "tester123", "테스터", ProfileCharacter.BUNNY, "2026-11-17T00:00:00"));
   click(view,"진로");
   assertTrue(buttons(view).stream().anyMatch(b -> "진로 고민".equals(b.getText())));
   assertTrue(buttons(view).stream().noneMatch(b -> "일상 고민".equals(b.getText())));
   click(view,"전체");
   assertTrue(buttons(view).stream().anyMatch(b -> "일상 고민".equals(b.getText())));
   assertThrows(IllegalArgumentException.class,()->view.showPage("missing"));
  });
 }
 @Test void startsWithEmptyStateAndWaitsForServerConnection() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   var live = new LiveSession(message -> {});
   var data = new UiData(live::send);
   var view = new ApplicationView(data, live);
   assertEquals(UiPages.LOGIN, view.currentPage());
   assertTrue(data.session().rooms().isEmpty());
   assertTrue(data.session().requests().isEmpty());
   JButton login = buttons(view).stream().filter(b -> "로그인".equals(b.getText())).findFirst().orElseThrow();
   assertFalse(login.isEnabled());
   live.connected();
   assertTrue(login.isEnabled());
  });
 }
}
