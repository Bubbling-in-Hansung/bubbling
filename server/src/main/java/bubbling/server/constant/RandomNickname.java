package bubbling.server.constant;

import java.util.List;

public class RandomNickname {

    private static final List<String> FIRST_NAME = List.of(
            "말랑한", "폭신한", "동글한", "귀여운", "수줍은",
            "졸린", "신나는", "엉뚱한", "깜찍한", "포근한",
            "느긋한", "새침한", "쪼꼬만", "배고픈", "행복한",
            "따뜻한", "소심한", "활발한", "통통한", "순수한",
            "방긋한", "꼬물꼬물한", "몽글몽글한", "장난꾸러기", "반짝이는"
    );

    private static final List<String> LAST_NAME = List.of(
            "토끼", "햄스터", "고양이", "강아지", "다람쥐",
            "병아리", "펭귄", "판다", "수달", "쿼카",
            "여우", "너구리", "고슴도치", "물개", "아기곰",
            "오리", "치즈", "푸딩", "마카롱", "솜사탕",
            "알파카", "친칠라", "미어캣", "아기새", "복숭아"
    );

    public String getRandomNickname() {
        int firstSize = FIRST_NAME.size();
        int lastSize = LAST_NAME.size();

        int firstRandom = (int)(Math.random() * firstSize);
        int lastRandom = (int)(Math.random() * lastSize);

        return FIRST_NAME.get(firstRandom) + LAST_NAME.get(lastRandom);
    }
}
