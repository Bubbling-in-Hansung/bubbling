package bubbling.client.ui.preview;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class PreviewSessionTest {
    @TempDir Path directory;

    @Test void storesUnicodeRoomAndReplacesPreviousSnapshot() throws Exception {
        Path path=directory.resolve("session.properties");
        Properties first=new Properties();first.setProperty("page","chat");first.setProperty("room.title","마음 나누기");
        PreviewSession.store(path,first);
        assertEquals(first,PreviewSession.load(path));
        Properties next=new Properties();next.setProperty("page","mypage");
        PreviewSession.store(path,next);
        assertEquals(next,PreviewSession.load(path));
        try(var files=Files.list(directory)){assertEquals(1,files.count());}
    }

    @Test void missingOrMalformedSessionDoesNotPreventLaunch() throws Exception {
        Path path=directory.resolve("session.properties");
        assertTrue(PreviewSession.load(path).isEmpty());
        Files.writeString(path,"page=\\uINVALID");
        assertTrue(PreviewSession.load(path).isEmpty());
        assertEquals("home",PreviewSession.validPage(null));
        assertEquals("home",PreviewSession.validPage("removed-screen"));
        assertEquals("chat",PreviewSession.validPage("chat"));
    }
}
