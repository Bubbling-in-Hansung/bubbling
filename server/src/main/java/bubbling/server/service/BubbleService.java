package bubbling.server.service;

import bubbling.server.store.BubbleStore;

public class BubbleService {

    private final BubbleStore store;

    public BubbleService(BubbleStore bubbleStore) {
        this.store = bubbleStore;
    }
}
