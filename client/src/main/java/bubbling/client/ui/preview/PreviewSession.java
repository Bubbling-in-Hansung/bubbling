package bubbling.client.ui.preview;

import bubbling.client.ui.ApplicationView;
import bubbling.client.ui.frame.MainFrame;
import bubbling.client.ui.model.UiData;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;
import bubbling.client.ui.model.UiPages;

/** Live-preview navigation metadata only; never stores service data or form contents. */
final class PreviewSession {
    private final MainFrame frame;
    private final ApplicationView view;
    private final UiData data;
    private final Path statePath;
    private final Path stopPath;
    private final Timer timer;
    private Properties previous;
    static void attach(MainFrame frame, UiData data) {
        String state = System.getProperty("bubbling.preview.state");
        if (state == null || !data.preview()) {
            return;
        }
        new PreviewSession(frame, data, Path.of(state), System.getProperty("bubbling.preview.stop"));
    }

    private PreviewSession(MainFrame frame, UiData data, Path statePath, String stop) {
        this.frame = frame;
        this.view = (ApplicationView) frame.getContentPane();
        this.data = data;
        this.statePath = statePath;
        this.stopPath = stop == null ? null : Path.of(stop);
        frame.setTitle("Bubbling — Live Preview");
        restore(load(statePath));
        timer = new Timer(150, e -> {
            save();
            if (stopPath != null && Files.exists(stopPath)) {
                timerStop();
                frame.dispose();
                System.exit(0);
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                save();
                timerStop();
            }
            @Override
            public void windowClosed(WindowEvent event) {
                timerStop();
            }
        });
        timer.start();
    }

    private void timerStop() {
        timer.stop();
    }

    static Properties load(Path path) {
        Properties state = new Properties();
        if (!Files.isRegularFile(path)) {
            return state;
        }
        try (var input = Files.newInputStream(path)) {
            state.load(input);
        } catch (IOException | IllegalArgumentException error) {
            System.err.println("[preview] Ignoring invalid session: " + error.getMessage());
            state.clear();
        }
        return state;
    }

    private void restore(Properties state) {
        String page = validPage(state.getProperty("page"));
        var restoredRoom = data.session().room(state.getProperty("room.id"));
        if (restoredRoom != null) {
            view.enterRoom(restoredRoom.view);
        } else if (page.equals(UiPages.CHAT)) {
            page = UiPages.HOME;
        }
        view.showPage(page);
        try {
            Rectangle bounds = new Rectangle(Integer.parseInt(state.getProperty("x")), Integer.parseInt(state.getProperty("y")),
                    Math.max(frame.getMinimumSize().width, Integer.parseInt(state.getProperty("width"))), Math.max(frame.getMinimumSize().height,
                    Integer.parseInt(state.getProperty("height"))));
            // A disconnected monitor must not leave the restored preview inaccessible.
            for (GraphicsDevice screen : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                Rectangle available = screen.getDefaultConfiguration().getBounds();
                if (available.contains(bounds.x + 40, bounds.y + 20)) {
                    frame.setBounds(bounds);
                    break;
                }
            }
        } catch (NumberFormatException ignored) {
            /* First launch or partial metadata: use normal defaults. */
        }
    }

    static String validPage(String page) {
        return UiPages.contains(page) ? page : UiPages.HOME;
    }

    private Properties snapshot() {
        Properties state = new Properties();
        state.setProperty("page", view.currentPage());
        if (view.currentRoom() != null) {
            var room = data.session().find(view.currentRoom());
            if (room != null) {
                state.setProperty("room.id", room.id);
            }
        }
        Rectangle bounds = frame.getBounds();
        state.setProperty("x", Integer.toString(bounds.x));
        state.setProperty("y", Integer.toString(bounds.y));
        state.setProperty("width", Integer.toString(bounds.width));
        state.setProperty("height", Integer.toString(bounds.height));
        return state;
    }

    private void save() {
        Properties state = snapshot();
        if (state.equals(previous)) {
            return;
        }
        try {
            store(statePath, state);
            previous = state;
        } catch (IOException error) {
            System.err.println("[preview] Cannot save navigation: " + error.getMessage());
        }
    }

    static void store(Path path, Properties state) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "preview-session-", ".tmp");
        try {
            try (var output = Files.newOutputStream(temporary)) {
                state.store(output, "Preview navigation only");
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
