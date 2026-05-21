package com.finger.handoff;

import com.finger.handoff.domain.practice.entity.PracticeScript;
import com.finger.handoff.domain.practice.repository.PracticeScriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PracticeDataInitController {
    private final PracticeScriptRepository practiceScriptRepository;

    @GetMapping("/api/init-practice-data")
    public String insertPracticeData() {

        if (practiceScriptRepository.count() > 0) {
            return "이미 DB에 연습용 대본 데이터가 존재합니다.";
        }

        List<PracticeScript> scripts = List.of(
                new PracticeScript("오늘 아침에는 비가 조금 내렸지만 오후부터는 맑아진다고 합니다. 우산을 챙겨야 할지 고민이 되는 날씨네요."),
                new PracticeScript("주말에는 주로 카페에 가서 평소 읽고 싶었던 소설책을 읽습니다. 조용한 음악을 들으며 차를 마시면 마음이 편안해집니다."),
                new PracticeScript("건강을 위해서 매일 저녁 삼십 분씩 가볍게 산책을 하고 있습니다. 꾸준히 걷다 보면 스트레스도 풀리고 체력도 좋아지는 것 같아요."),
                new PracticeScript("어제 저녁에는 마트에서 신선한 재료를 사 와서 직접 파스타를 요리해 먹었습니다. 처음 시도해 본 레시피였는데 생각보다 아주 맛있게 완성되었습니다."),
                new PracticeScript("다가오는 휴가철에는 가족들과 함께 가까운 바다로 여행을 떠날 계획입니다. 오랜만에 일상에서 벗어나 푹 쉬고 올 수 있었으면 좋겠습니다.")
        );

        practiceScriptRepository.saveAll(scripts);

        return "✅ 성공적으로 " + scripts.size() + "개의 연습용 대본이 DB에 저장되었습니다!";
    }
}
