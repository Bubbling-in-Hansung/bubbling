package bubbling.client.ui.model;

/** Local picker order and labels; never used as protocol ordinals. */
public enum CharacterChoice {
    BUNNY("bunny", "토끼"),
    CAT("cat", "고양이"),
    BEAR("bear", "곰"),
    FOX("fox", "여우"),
    PENGUIN("penguin", "펭귄"),
    FROG("frog", "개구리");

    private static final CharacterChoice[] ORDER = values();
    private final String image;
    private final String label;

    CharacterChoice(String filename, String label) {
        this.image = "/character/" + filename + ".png";
        this.label = label;
    }

    public String image() {
        return image;
    }

    public String label() {
        return label;
    }

    public static int count() {
        return ORDER.length;
    }

    public static CharacterChoice at(int index) {
        return ORDER[Math.floorMod(index, ORDER.length)];
    }

}
