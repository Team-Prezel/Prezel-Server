package com.finger.handoff;

import com.finger.handoff.domain.terms.entity.Terms;
import com.finger.handoff.domain.terms.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InitController {

    private final TermsRepository termsRepository;

    @PostMapping("/init-dummy-terms")
    public ResponseEntity<String> initTerms() {
        // 💡 [선택사항] 기존에 DB에 잘못 들어갔거나 중복된 약관 데이터가 있다면
        // 아래 주석을 풀어 기존 데이터를 먼저 싹 지우고 새로 넣으시는 것을 추천합니다!
        termsRepository.deleteAll();

        // 1. 이용약관 (구글 사이트 링크 적용)
        termsRepository.save(Terms.builder()
                .title("이용약관")
                .summary("이용약관 주소")
                .content("https://sites.google.com/view/prezel-terms-of-service/%ED%99%88")
                .isRequired(true)
                .version("1.0")
                .build());

        // 2. 개인정보 정책 (추후 전용 주소가 생기면 동일하게 content에 넣으시면 됩니다)
        termsRepository.save(Terms.builder()
                .title("개인정보처리방침")
                .summary("개인정보처리방침 주소")
                .content("https://sites.google.com/view/prezel-privacy-policy/%ED%99%88")
                .isRequired(true)
                .version("1.0")
                .build());

        return ResponseEntity.ok("운영 DB 약관 데이터 세팅 완료 (이용약관 구글 링크 적용)");
    }
}