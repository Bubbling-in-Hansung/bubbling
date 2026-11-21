package bubbling.client.ui.model;

/** Display snapshot only; this is not a network DTO. */
public record RoomView(String title, String category, String mode, int people, int capacity, boolean privateRoom,
        boolean expired, boolean owner, int tint, double x, double y, int size) {
    public boolean full() {
        return people >= capacity;
    }
}
