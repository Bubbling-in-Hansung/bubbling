package bubbling.server.domain;

import bubbling.common.enums.ProfileCharacter;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.UserExceptionCode;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class User {
    private final String id = UUID.randomUUID().toString();
    private final String loginId;
    private String password;
    private String nickname;
    private ProfileCharacter profile;
    private final LocalDateTime createdAt = LocalDateTime.now();

    private User(String loginId, String password, String nickname, ProfileCharacter profile) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
        this.profile = profile;
    }

    public static User create(String loginId, String password, String nickname, ProfileCharacter profile) {
        validateLoginId(loginId);
        validateNickname(nickname);
        Objects.requireNonNull(profile, "프로필은 필수입니다.");
        return new User(loginId, password, nickname, profile);
    }

    private static void validateNickname(String nickname) {
        Objects.requireNonNull(nickname, "닉네임은 필수입니다.");
        if (nickname.length() < 2 || nickname.length() > 32) {
            throw new BaseException(UserExceptionCode.INVALID_NICKNAME);
        }
    }

    private static void validateLoginId(String loginId) {
        Objects.requireNonNull(loginId, "로그인 아이디는 필수입니다.");
        if (loginId.length() < 8 || loginId.length() > 32) {
            throw new BaseException(UserExceptionCode.INVALID_LOGIN_ID);
        }
    }

    public static void validatePassword(String originalPassword) {
        Objects.requireNonNull(originalPassword);
        if (originalPassword.length() < 8 || originalPassword.length() > 64) {
            throw new BaseException(UserExceptionCode.INVALID_PASSWORD_LENGTH);
        }

        for (int i = 0; i < originalPassword.length(); i++) {
            if (originalPassword.charAt(i) > 127) {
                throw new BaseException(UserExceptionCode.INVALID_PASSWORD_CHARACTER);
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }

    public ProfileCharacter getProfile() {
        return profile;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setProfile(ProfileCharacter profile) {
        this.profile = profile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
