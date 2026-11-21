package bubbling.server.security;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordService {
    public String hash(String password) {
        return BCrypt.hashpw(
                password,
                BCrypt.gensalt(12)
        );
    }

    public boolean matches(String password, String passwordHash) {
        return BCrypt.checkpw(
                password,
                passwordHash
        );
    }
}
