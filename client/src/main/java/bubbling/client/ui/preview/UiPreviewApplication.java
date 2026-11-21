package bubbling.client.ui.preview;

import bubbling.client.ui.frame.MainFrame;
import bubbling.client.config.ClientConfig;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;

public final class UiPreviewApplication {
    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> {
            var data = PreviewData.create();
            var frame = new MainFrame(new ClientConfig(), data);
            PreviewSession.attach(frame, data);
            frame.setVisible(true);
        });
    }
}
