package bubbling.server.util;

import java.util.regex.Pattern;

public class StringValidator {

    private static final Pattern VALID_LOGIN_ID = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");
    private static final Pattern VALID_PASSWORD = Pattern.compile("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*])[a-zA-Z0-9!@#$%^&*]+$");
    private static final Pattern VALID_NICKNAME = Pattern.compile("^[가-힣a-zA-Z0-9]+$");

    public static boolean validateLoginId(String str) {
        return VALID_LOGIN_ID.matcher(str).matches();
    }

    public static boolean validatePassword(String str) {
        return VALID_PASSWORD.matcher(str).matches();
    }

    public static boolean validateNickname(String str) {
        return VALID_NICKNAME.matcher(str).matches();
    }
}
