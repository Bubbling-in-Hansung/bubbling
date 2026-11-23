package bubbling.client.ui;

import bubbling.client.ui.drawing.BubbleButton;
import bubbling.client.ui.drawing.BubbleMotion;
import bubbling.client.ui.drawing.BubblePlacement;
import bubbling.client.ui.model.*;
import bubbling.common.enums.BubbleCategory;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BubbleMotionTest {
 private static JButton bubble(JPanel field,int x,int y,double vx,double vy){
  var session = new UiData((type, payload) -> {}).session();
  var room = session.applyBubble(ServerRoomFixtures.bubble("room", "방", BubbleCategory.DAILY, "2099-01-01T00:00:00"));
  room.view = new RoomView("방","일상","함께 이야기해요",1,4,false,false,false,0,0,0,100);
  JButton b=new JButton();b.setBounds(x,y,136,136);
  b.putClientProperty("room",room);
  b.putClientProperty("position",new double[]{x,y});b.putClientProperty("velocity",new double[]{vx,vy});field.add(b);return b;
 }
 private static void separated(JButton a,JButton b){
  double[] p=(double[])a.getClientProperty("position"),q=(double[])b.getClientProperty("position");
  assertTrue(Math.hypot(p[0]-q[0],p[1]-q[1])>=100-.1,"Visible circles must not overlap");
 }
 @Test void fastHeadOnBubblesStaySeparated() throws Exception{
  SwingUtilities.invokeAndWait(()->{
   JPanel field=new JPanel(null);field.setSize(700,400);
   JButton a=bubble(field,100,130,.59,0),b=bubble(field,400,130,-.59,0);
   for(int i=0;i<100;i++){BubbleMotion.advance(field,10);separated(a,b);}
  });
 }
 @Test void hoveringBubbleStaysFixedWhileOtherMovesAway() throws Exception{
  SwingUtilities.invokeAndWait(()->{
   JPanel field=new JPanel(null);field.setSize(700,400);
   JButton a=bubble(field,200,130,.4,0),b=bubble(field,290,130,-.4,0);a.getModel().setRollover(true);
   for(int i=0;i<30;i++){BubbleMotion.advance(field,10);separated(a,b);assertEquals(200,a.getX());assertEquals(130,a.getY());}
  });
 }
 @Test void wallBlockedBodyGivesSeparationToOtherBody() throws Exception{
  SwingUtilities.invokeAndWait(()->{
   JPanel field=new JPanel(null);field.setSize(600,300);
   JButton a=bubble(field,0,80,0,0),b=bubble(field,90,80,0,0);
   BubbleMotion.advance(field,10);separated(a,b);assertEquals(0,a.getX());assertTrue(b.getX()>=108);
  });
 }
 @Test void crowdedFieldKeepsBodiesInsideAndCoordinatesFinite() throws Exception{
  SwingUtilities.invokeAndWait(()->{
   JPanel field=new JPanel(null);field.setSize(180,180);
   JButton a=bubble(field,0,0,.5,.3),b=bubble(field,30,30,-.5,-.3);
   for(int i=0;i<30;i++)BubbleMotion.advance(field,10);
   for(JButton c:List.of(a,b)){double[] p=(double[])c.getClientProperty("position");assertTrue(Double.isFinite(p[0])&&Double.isFinite(p[1]));assertTrue(p[0]>=0&&p[0]<=44);assertTrue(p[1]>=0&&p[1]<=44);}
  });
 }
 @Test void relayoutKeepsLiveCoordinatesAndResizeRecoveryIsGradual() throws Exception{
  SwingUtilities.invokeAndWait(()->{
   JPanel field=new JPanel(null);field.setSize(900,400);
   JButton a=bubble(field,620,130,.5,.3);
   double[] position=(double[])a.getClientProperty("position");
   bubbling.client.ui.drawing.BubblePlacement.layout(field);
   assertSame(position,a.getClientProperty("position"));assertEquals(620,a.getX());
   field.setSize(600,400);bubbling.client.ui.drawing.BubblePlacement.layout(field);
   assertEquals(620,a.getX());
   BubbleMotion.advance(field,10);
   assertTrue(a.getX()<620&&a.getX()>=616,"Resize recovery must not snap to boundary");
   for(int i=0;i<50;i++)BubbleMotion.advance(field,10);
   assertTrue(a.getX()<=464);
  });
 }
 @Test void pressedBubbleStaysStillEvenWhenPointerLeaves() throws Exception{
  SwingUtilities.invokeAndWait(()->{
   JPanel field=new JPanel(null);field.setSize(700,400);
   JButton a=bubble(field,200,130,.5,.3);a.getModel().setPressed(true);
   BubbleMotion.advance(field,10);assertEquals(200,a.getX());assertEquals(130,a.getY());
  });
 }

 @Test void reactionThresholdsResizeTheHitAreaAndKeepTheBubbleInsideTheField() throws Exception {
  SwingUtilities.invokeAndWait(() -> {
   var session = new UiData((type, payload) -> {}).session();
   var snapshot = ServerRoomFixtures.bubble("growth", "방", BubbleCategory.DAILY, "2099-01-01T00:00:00");
   var room = session.applyBubble(snapshot);
   var field = new JPanel(null);
   field.setSize(500, 400);
   var button = new BubbleButton(room);
   button.putClientProperty("room", room);
   double[] position = {310, 120};
   button.putClientProperty("position", position);
   field.add(button);
   BubblePlacement.layout(field);

   session.updateReactionCount(room.id, 4);
   BubblePlacement.layout(field);
   assertEquals(152, room.bubbleDiameter());
   assertEquals(310, button.getX());
   assertFalse(button.contains(177, 102));

   session.updateReactionCount(room.id, 5);
   BubblePlacement.layout(field);
   assertEquals(168, room.bubbleDiameter());
   assertEquals(204, button.getWidth());
   assertEquals(500, button.getX() + button.getWidth());
   assertEquals(112, button.getY());
   assertTrue(button.contains(177, 102));
   assertSame(position, button.getClientProperty("position"));

   int[][] remainingThresholds = {{9, 168}, {10, 184}, {19, 184}, {20, 200}, {21, 200}};
   for (int[] threshold : remainingThresholds) {
    session.updateReactionCount(room.id, threshold[0]);
    assertEquals(threshold[1], room.bubbleDiameter());
   }
   session.applyBubble(snapshot);
   assertEquals(200, room.bubbleDiameter());
  });
 }
}
