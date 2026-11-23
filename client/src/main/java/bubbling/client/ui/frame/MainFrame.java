package bubbling.client.ui.frame;

import bubbling.client.LiveSession;
import bubbling.client.config.ClientConfig;
import bubbling.client.ui.ApplicationView;
import bubbling.client.ui.model.UiData;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    final private ClientConfig cc;
    public MainFrame(ClientConfig cc, UiData data, LiveSession live) {
        bubbling.client.ui.style.Styles.install();
        this.cc = cc;
        setTitle("Bubbling");
        setSize(cc.getWidth(), cc.getHeight());
        setMinimumSize(new Dimension(cc.getMinWidth(), cc.getMinHeight()));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new ApplicationView(data, live));
    }
}
