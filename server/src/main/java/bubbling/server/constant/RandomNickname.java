package bubbling.server.constant;

import java.util.List;

public class RandomNickname {

    private final static List<String> FIRST_NAME = List.of(
            "abcd"
    );

    private final static List<String> LAST_NAME = List.of(
            "efgh"
    );

    public String getRandomNickname() {
        int firstSize = FIRST_NAME.size();
        int lastSize = LAST_NAME.size();

        int firstRandom = (int)(Math.random() * firstSize);
        int lastRandom = (int)(Math.random() * lastSize);

        return FIRST_NAME.get(firstRandom) + LAST_NAME.get(lastRandom);
    }
}
