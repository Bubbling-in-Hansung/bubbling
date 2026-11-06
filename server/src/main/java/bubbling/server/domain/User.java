package bubbling.server.domain;

import bubbling.common.enums.ProfileCharacter;

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
        validateId(loginId);
        validateNickname(nickname);
        Objects.requireNonNull(profile, "프로필은 필수입니다.");
        return new User(loginId, password, nickname, profile);
    }

    private static void validateNickname(String nickname) {
        Objects.requireNonNull(nickname, "닉네임은 필수입니다.");
        if (nickname.length() < 2 || nickname.length() > 32) {
            throw new IllegalArgumentException("닉네임은 최소 2자, 최대 32자 이내로 설정해야합니다.");
        }
    }

    private static void validateId(String loginId) {
        Objects.requireNonNull(loginId, "로그인 아이디는 필수입니다.");
        if (loginId.length() < 8 || loginId.length() > 32) {
            throw new IllegalArgumentException("아이디는 최소 8자, 최대 32자 이내로 설정해야합니다.");
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
}
