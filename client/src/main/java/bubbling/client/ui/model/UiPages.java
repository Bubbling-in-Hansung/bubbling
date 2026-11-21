package bubbling.client.ui.model;

import java.util.Set;

/** Stable card identifiers shared with preview navigation metadata. */
public final class UiPages {
    public static final String HOME = "home";
    public static final String PROFILE = "mypage";
    public static final String CHAT = "chat";
    public static final String LOGIN = "login";
    public static final String SIGNUP = "signup";
    public static final String CONVERSATIONS = "conversations";
    private static final Set<String> ALL = Set.of(HOME, PROFILE, CHAT, LOGIN, SIGNUP, CONVERSATIONS);
    private UiPages() {
    }

    public static boolean contains(String page) {
        return page != null && ALL.contains(page);
    }

    public static boolean usesShell(String page) {
        return HOME.equals(page) || PROFILE.equals(page) || CONVERSATIONS.equals(page);
    }
}
