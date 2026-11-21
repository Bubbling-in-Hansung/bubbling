package bubbling.client.ui.preview;

import bubbling.client.ui.model.*;
import java.util.List;

public final class PreviewData {
    private PreviewData() {
    }

    public static UiData create() {
        return new UiData(true, List.of(new RoomView("요즘 진로 때문에 고민이에요", "진로", "함께 이야기해요", 2, 4, false, false, false,
                0, .12, .18, 150), new RoomView("친구와 멀어진 것 같아 속상해요", "관계", "들어만 주세요", 2, 3, true, false, false, 2, .43, .10,
                128), new RoomView("오늘은 아무것도 하고 싶지 않아요", "일상", "함께 이야기해요", 4, 4, false, false, true, 1, .70, .20, 158), new RoomView("가족에게 솔직한 마음을 말해도 될까요?",
                "가족", "해결책 알려주세요", 1, 2, true, false, false, 3, .27, .55, 118), new RoomView("면접에서 또 떨어졌어요", "진로", "해결책 알려주세요",
                3, 4, false, false, false, 4, .54, .57, 136), new RoomView("시험 결과 때문에 마음이 무거워요", "학업", "들어만 주세요", 2, 3, false,
                true, false, 0, .82, .61, 116), new RoomView("새로운 환경에 잘 적응하고 싶어요", "일상", "함께 이야기해요", 1, 4, true, false, true,
                2, .08, .68, 110)));
    }
}
