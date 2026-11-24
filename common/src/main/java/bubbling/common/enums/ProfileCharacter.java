package bubbling.common.enums;

public enum ProfileCharacter {
    EXAMPLE("/characters/example.png"),
    BUNNY("/character/bunny.png"),
    CAT("/character/cat.png"),
    BEAR("/character/bear.png"),
    FOX("/character/fox.png"),
    PENGUIN("/character/penguin.png"),
    FROG("/character/frog.png");

    private final String path;

    ProfileCharacter(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
