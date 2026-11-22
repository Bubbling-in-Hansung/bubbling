package bubbling.client.ui.model;

import bubbling.common.enums.ProfileCharacter;

/** Local picker order and labels; never used as protocol ordinals. */
public enum CharacterChoice {
    BUNNY(ProfileCharacter.BUNNY, "토끼"),
    CAT(ProfileCharacter.CAT, "고양이"),
    BEAR(ProfileCharacter.BEAR, "곰"),
    FOX(ProfileCharacter.FOX, "여우"),
    PENGUIN(ProfileCharacter.PENGUIN, "펭귄"),
    FROG(ProfileCharacter.FROG, "개구리");

    private static final CharacterChoice[] ORDER = values();
    private final ProfileCharacter profile;
    private final String label;

    CharacterChoice(ProfileCharacter profile, String label) {
        this.profile = profile;
        this.label = label;
    }

    public String image() {
        return profile.getPath();
    }

    public ProfileCharacter profile() {
        return profile;
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

    public static int indexOf(ProfileCharacter profile) {
        for (int index = 0; index < ORDER.length; index++) {
            if (ORDER[index].profile == profile) return index;
        }
        return BUNNY.ordinal();
    }
}
