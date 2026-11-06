import bubbling.server.config.ServerComponent;
import bubbling.server.config.ServerConfig;
import bubbling.server.network.TcpServer;

import java.io.IOException;

public class ServerBootstrap {
    public void start() throws IOException {
        ServerConfig config = new ServerConfig();
        ServerComponent component = new ServerComponent();

        TcpServer server = new TcpServer(
                config.getPort(),
                config.getMaxClients()
        );

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        server.start();
    }
}
