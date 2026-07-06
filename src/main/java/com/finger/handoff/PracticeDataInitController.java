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

        practiceScriptRepository.deleteAll();

        List<PracticeScript> scripts = List.of(
                new PracticeScript("리포트 작성의 가장 중요한 부분은 정확한 정보와 논리적인 구성이 있습니다. 리포트를 작성하면서, 각 항목의 내용을 어떻게 연결할 지에 대해 많은 고민을 했습니다. 리포트의 내용이 분명하고 논리적일수록, 읽는 사람들이 이해하기 쉬워집니다."),
                new PracticeScript("음악을 틀어두면 같은 공간도 전혀 다르게 느껴집니다. 빠른 음악은 활기찬 분위기를 만들고, 잔잔한 음악은 마음을 차분하게 해줍니다. 그래서 음악은 일상 속 분위기를 바꾸는 쉬운 방법입니다"),
                new PracticeScript("계절이 바뀌면 하루를 보내는 방식도 달라집니다. 여름에는 시원한 곳을 찾게 되고, 겨울에는 따뜻한 음식을 더 자주 찾게 됩니다. 생활의 작은 선택들이 계절에 맞춰 변합니다."),
                new PracticeScript("발표에서는 완벽하게 말하는 것보다 중요한 내용을 정확하게 전달하는 것이 더 중요합니다. 말이 조금 느리거나 중간에 잠시 멈추더라도, 핵심 메시지가 분명하면 청중은 발표의 내용을 충분히 이해할 수 있습니다."),
                new PracticeScript("데이터를 다룰 때는 단순히 숫자를 보는 것에서 그치지 않아야 합니다. 데이터 뒤에 어떤 행동과 의도가 담겨 있는지 해석해야 더 설득력 있는 결론을 만들 수 있습니다. 따라서 분석 과정에서는 결과보다 맥락을 함께 들여다보는 태도가 필요합니다."),
                new PracticeScript("효과적인 학습 환경을 만들기 위해서는 혼자서도 현재 상태를 확인할 수 있어야 합니다. 학습자는 자신이 어떤 부분에서 흔들리는지 알아야 다음 연습의 방향을 정할 수 있습니다. 확인과 회고가 반복될수록 학습 효과도 높아집니다."),
                new PracticeScript("실습을 통해 확인한 사실은 설명을 많이 하는 것보다 쉽게 말하는 것이 더 어렵다는 점이었습니다. 쓰는 사람은 익숙한 표현이라고 생각하지만, 듣는 사람에게는 낯설게 느껴질 수 있습니다. 그래서 쉬운 단어를 선택하는 과정이 반드시 필요합니다."),
                new PracticeScript("서비스를 설계할 때 사용자의 시선에서 상황을 살펴보는 과정이 필요했습니다. 사용자가 실제로 어떤 순간에 불편을 느끼는지 조사하고, 그 속에서 반복적으로 나타나는 문제를 찾았습니다. 사소해 보이는 사용 경험도 서비스 전체의 만족도에 큰 영향을 줄 수 있습니다."),
                new PracticeScript("자료를 정리할 때 가장 어려운 부분은 여러 정보를 하나의 흐름으로 연결하는 일이었습니다. 관련 내용을 분류하고, 불필요한 설명을 줄이면서 발표의 논리가 더 분명해졌습니다. 좋은 발표는 필요한 내용을 알맞은 순서로 전달하는 데에서 시작됩니다."),
                new PracticeScript("방을 정리하다 보면 필요한 물건과 그렇지 않은 물건이 분명히 보입니다. 물건을 줄이고 자리를 정하면 공간을 더 편하게 사용할 수 있습니다. 정돈된 환경은 생활의 흐름까지 바꿔줍니다."),
                new PracticeScript("여행은 새로운 장소를 방문하는 경험이지만, 동시에 익숙한 생활에서 잠시 벗어나는 시간이기도 합니다. 낯선 거리와 풍경은 평소와 다른 생각을 하게 만듭니다. 그래서 여행은 휴식과 발견을 함께 제공합니다."),
                new PracticeScript("바다를 바라보면 복잡했던 생각이 조금씩 부드러워집니다. 파도가 반복해서 밀려오고 물러가는 모습은 마음을 편안하게 만들어 줍니다. 바쁜 일상에서 벗어나 잠시 바람을 맞는 것만으로도 충분한 휴식이 됩니다."),
                new PracticeScript("수면 시간이 부족하면 생각보다 쉽게 집중력이 흐트러집니다. 스마트폰을 손에 쥔 채 잠드는 습관은 숙면을 방해할 수 있습니다. 잠들기 전에는 조명을 낮추고, 시선을 쉬게 하는 시간이 필요합니다."),
                new PracticeScript("공공장소에서는 개인의 편리함보다 함께 사용하는 공간의 기준을 먼저 생각해야 합니다. 큰 소리로 통화하거나 길을 가로막는 행동은 다른 사람에게 불편을 줄 수 있습니다. 결국 기본적인 배려가 공간의 분위기를 결정합니다."),
                new PracticeScript("체육 활동을 꾸준히 하면 체력뿐만 아니라 자신감도 함께 커집니다. 처음에는 짧은 시간만 움직여도 충분하고, 차츰 운동량을 늘려가면 됩니다. 천천히 지속하는 습관이 몸의 변화를 만들어 냅니다.")
        );

        practiceScriptRepository.saveAll(scripts);

        return "✅ 성공적으로 " + scripts.size() + "개의 연습용 대본이 DB에 저장되었습니다!";
    }
}
