package bubbling.client.ui;
import bubbling.client.ui.model.*;
import bubbling.common.enums.ChatRequestStatus;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UiSessionTest {
 static class Time extends Clock {
  Instant value=Instant.parse("2026-11-17T00:00:00Z");
  public ZoneId getZone(){return ZoneOffset.UTC;}
  public Clock withZone(ZoneId zone){return this;}
  public Instant instant(){return value;}
  void advance(long seconds){value=value.plusSeconds(seconds);}
 }
 static RoomView view(boolean owner,boolean privateRoom,int people){
  return new RoomView("같은 제목","일상","함께 이야기해요",people,4,privateRoom,false,owner,0,.2,.2,140);
 }
 @Test void rejectionKeepsStatusAndDeadlineAcrossRepeatedResults(){
  Time clock=new Time();UiSession s=new UiSession(true,List.of(view(false,true,1)),clock);var r=s.rooms().getFirst();
  s.request(r);var request=s.outgoing(r);s.decide(request,false);
  assertEquals(60,s.retrySeconds(r));clock.advance(30);s.decide(request,false);
  assertEquals(30,s.retrySeconds(r));assertEquals(ChatRequestStatus.REJECTED,request.status);
  assertThrows(IllegalStateException.class,()->s.request(r));
  clock.advance(30);assertEquals(0,s.retrySeconds(r));s.request(r);
  assertNotSame(request,s.outgoing(r));
 }
 @Test void fullRoomRejectsRemainingRequestsAndKeepsRecentOwner(){
  UiSession s=new UiSession(true,List.of(view(true,false,3),view(true,false,1)));var first=s.rooms().getFirst();var second=s.rooms().getLast();
  s.visit(second);s.visit(first);var pending=s.requests().stream().filter(q->q.roomId.equals(first.id)).toList();
  s.decide(pending.getFirst(),true);
  assertTrue(s.hidden(first));assertSame(first,s.recent());assertEquals(ChatRequestStatus.REJECTED,pending.getLast().status);
  s.enter(first);assertEquals(4,first.people);s.leave(first);assertFalse(s.hidden(first));
  assertNotEquals(first.id,second.id);
 }
 @Test void speechExpiresWithoutDeletingHistoryAndEndedRoomIsReadOnly(){
  Time clock=new Time();UiSession s=new UiSession(true,List.of(view(true,false,1)),clock);var r=s.rooms().getFirst();
  for(int i=0;i<4;i++)s.send(r,"message "+i,true);
  assertEquals(3,s.speech(r,"익명의 고래").size());assertEquals("message 1",s.speech(r,"익명의 고래").getFirst().text());
  clock.advance(7);assertTrue(s.speech(r,"익명의 고래").isEmpty());assertEquals(4,r.messages.size());
  s.end(r);s.enter(r);assertFalse(r.joined);assertThrows(IllegalStateException.class,()->s.send(r,"blocked",true));
 }
 @Test void expirationDoesNotEndExistingConversationAndOrdinaryStateIsDisabled(){
  Time clock=new Time();UiSession s=new UiSession(true,List.of(view(true,false,1)),clock);var r=s.rooms().getFirst();
  clock.advance(1800);s.tick();assertTrue(s.hidden(r));assertFalse(r.ended);s.send(r,"still joined",true);
  assertTrue(s.requests().stream().allMatch(q->q.status==ChatRequestStatus.REJECTED));
  UiSession ordinary=new UiSession(false,List.of(view(true,false,1)));
  assertThrows(IllegalStateException.class,()->ordinary.send(ordinary.rooms().getFirst(),"blocked",true));
 }

 @Test void previewMessageRefreshesTheCurrentDurationUntilTheBubbleExpires(){
  Time clock = new Time();
  UiSession session = new UiSession(true, List.of(), clock);
  RoomView owned = view(true, false, 1);
  var room = session.create(owned, 10);
  session.update(room, owned, 20);
  clock.advance(15);
  session.send(room, "new message", true);
  var refreshed = clock.instant().plusSeconds(20 * 60L);
  assertEquals(refreshed, room.expiresAt);
  clock.advance(20 * 60L);
  session.send(room, "existing conversation", true);
  assertEquals(refreshed, room.expiresAt);
 }
}
