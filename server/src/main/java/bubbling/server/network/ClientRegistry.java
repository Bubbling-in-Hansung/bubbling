package bubbling.server.network;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRegistry {

    private final Set<ClientConnection> connections = ConcurrentHashMap.newKeySet();

    private Runnable onlineStatusChangedListener = () -> {};

    public void setOnlineStatusChangedListener(Runnable listener) {
        this.onlineStatusChangedListener = Objects.requireNonNull(listener);
    }

    public synchronized boolean register(ClientConnection connection, int maxClients) {
        if (connections.size() >= maxClients) {
            return false;
        }

        return connections.add(connection);
    }

    public void unregister(ClientConnection connection) {
        boolean removed = connections.remove(connection);

        if (removed && connection.getUser() != null) {
            notifyOnlineStatusChanged();
        }
    }

    public List<ClientConnection> getAll() {
        return List.copyOf(connections);
    }

    public int getCount() {
        return connections.size();
    }

    public int getOnlineCount() {
        return (int) connections.stream()
                .filter(ClientConnection::isAuthenticated)
                .count();
    }

    public void closeAll() {
        for (ClientConnection connection : getAll()) {
            connection.close();
        }
    }

    public void notifyOnlineStatusChanged() {
        onlineStatusChangedListener.run();
    }
}
