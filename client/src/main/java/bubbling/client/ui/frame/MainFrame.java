package bubbling.client.ui.frame;

import bubbling.client.config.ClientConfig;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame(ClientConfig cc) {
        setTitle("Bubbling");
        setSize(cc.getWidth(), cc.getHeight());
        setMinimumSize(new Dimension(cc.getMinWidth(), cc.getMinHeight()));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());

        JLabel label = new JLabel("Bubbling", SwingConstants.CENTER);

        panel.add(label, BorderLayout.CENTER);

        setContentPane(panel);
    }
}