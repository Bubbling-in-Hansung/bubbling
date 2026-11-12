import bubbling.server.config.ServerComponent;
import bubbling.server.config.ServerConfig;
import bubbling.server.network.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(ServerBootstrap.class);

    public void start() {
        try {
            ServerConfig config = new ServerConfig();
            ServerComponent component = new ServerComponent();

            TcpServer server = new TcpServer(
                    config.getPort(),
                    config.getMaxClients(),
                    component.getClientRegistry(),
                    component.getMessageDispatcher()
            );

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                component.stopSchedulers();
                server.stop();
            }));

            server.start();
        } catch (Exception e) {
            log.error("서버 실행 중 오류가 발생했습니다.", e);
            System.exit(1);
        }
    }
}
