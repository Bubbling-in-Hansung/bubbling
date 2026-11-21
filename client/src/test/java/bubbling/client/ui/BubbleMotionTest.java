package bubbling.client.ui;

import bubbling.client.ui.drawing.BubbleMotion;
import bubbling.client.ui.model.*;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BubbleMotionTest {
 private static JButton bubble(JPanel field,int x,int y,double vx,double vy){
  RoomView view=new RoomView("방","일상","함께 이야기해요",1,4,false,false,false,0,0,0,100);
  JButton b=new JButton();b.setBounds(x,y,136,136);
  b.putClientProperty("room",new UiSession(true,List.of(view)).rooms().getFirst());
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
}