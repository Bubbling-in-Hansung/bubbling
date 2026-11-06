package bubbling.common.enums;

public enum ProfileCharacter {
    EXAMPLE("/characters/example.png");

    private final String path;

    ProfileCharacter(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
