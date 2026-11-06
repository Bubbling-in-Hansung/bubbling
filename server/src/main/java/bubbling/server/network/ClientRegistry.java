package bubbling.server.network;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRegistry {

    private final Set<ClientConnection> connections = ConcurrentHashMap.newKeySet();

    public synchronized boolean register(ClientConnection connection, int maxClients) {
        if (connections.size() >= maxClients) {
            return false;
        }

        return connections.add(connection);
    }

    public void unregister(ClientConnection connection) {
        connections.remove(connection);
    }

    public List<ClientConnection> getAll() {
        return List.copyOf(connections);
    }

    public int getCount() {
        return connections.size();
    }

    public void closeAll() {
        for (ClientConnection connection : getAll()) {
            connection.close();
        }
    }
}
