package bubbling.client.ui;
import bubbling.client.ui.model.*;
import bubbling.client.ui.preview.*;
import com.formdev.flatlaf.FlatLightLaf;
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
 @Test void previewNavigationAndFiltersAreLocal() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   FlatLightLaf.setup();ApplicationView view=new ApplicationView(PreviewData.create());
   click(view,"진로");
   var previewRooms=PreviewData.create().session().rooms();
   var roomButtons=buttons(view).stream().filter(b->previewRooms.stream().anyMatch(r->r.view.title().equals(b.getText()))).toList();
   assertEquals(2,roomButtons.size());
   click(view,"전체");
   click(view,"요즘 진로 때문에 고민이에요");click(view,"대화 참여하기");assertEquals("chat",view.currentPage());
   for(String p:new String[]{"home","mypage","login","signup","conversations"}){view.showPage(p);assertEquals(p,view.currentPage());}
   assertThrows(IllegalArgumentException.class,()->view.showPage("missing"));
   for(JButton b:buttons(view))if(java.util.List.of("가입 완료").contains(b.getText()))assertFalse(b.isEnabled());
  });
 }
 @Test void ordinaryLaunchDoesNotUsePreviewRooms() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   ApplicationView view=new ApplicationView(UiData.empty());
   var previewRooms=PreviewData.create().session().rooms();
   assertTrue(buttons(view).stream().noneMatch(b->previewRooms.stream().anyMatch(r->r.view.title().equals(b.getText())))); for(JButton b:buttons(view))if(java.util.List.of("전송","퇴장","대화 종료").contains(b.getText()))assertFalse(b.isEnabled());
  });
 }
}
