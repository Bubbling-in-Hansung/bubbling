import bubbling.client.config.ClientConfig;
import bubbling.client.network.ClientMessageDispatcher;
import bubbling.client.network.TcpClient;
import bubbling.client.ui.frame.MainFrame;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class ClientBootstrap {

    private volatile boolean closing = false;

    public void start() {

        FlatLightLaf.setup();

        ClientConfig config = new ClientConfig();

        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();

        TcpClient client = new TcpClient(
                config.getHost(),
                config.getPort(),
                dispatcher
        );

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setDefaultCloseOperation(
                    JFrame.DISPOSE_ON_CLOSE
            );
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    closing = true;
                    client.close();
                }
            });
            frame.setVisible(true);
        });

        Thread connectionThread = new Thread(() -> {
            try {
                client.connect();
                if (closing) {
                    client.close();
                }
            } catch (IOException e) {
                if (closing) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            null,
                            "서버 연결에 실패했습니다.\n" + e.getMessage(),
                            "연결 오류",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
            }

        }, "tcp-connect");

        connectionThread.setDaemon(true);
        connectionThread.start();
    }
}
