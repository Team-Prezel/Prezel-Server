package com.finger.handoff;

import com.finger.handoff.domain.terms.entity.Terms;
import com.finger.handoff.domain.terms.repository.TermsRepository;
import com.finger.handoff.domain.terms.repository.UserTermsAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InitController {

    private final TermsRepository termsRepository;
    private final UserTermsAgreementRepository agreementRepository;

    @PostMapping("/init-dummy-terms")
    public ResponseEntity<String> initTerms() {
        agreementRepository.deleteAll();
        termsRepository.deleteAll();

        // 1. 이용약관 (구글 사이트 링크 적용)
        termsRepository.save(Terms.builder()
                .title("이용약관")
                .summary("서비스 이용과 관련한 기본적인 권리·의무 및 책임사항을 규정합니다.")
                .content("https://sites.google.com/view/prezel-terms-of-service/%ED%99%88")
                .isRequired(true)
                .version("1.0")
                .build());

        // 2. 개인정보처리방침 (구글 사이트 링크 적용)
        termsRepository.save(Terms.builder()
                .title("개인정보처리방침")
                .summary("서비스 제공을 위한 음성 녹음 및 분석 등 개인정보 수집·이용 안내입니다.")
                .content("https://sites.google.com/view/prezel-privacy-policy/%ED%99%88")
                .isRequired(true)
                .version("1.0")
                .build());

        return ResponseEntity.ok("운영 DB 약관 데이터 및 구글 사이트 링크 세팅 완벽하게 완료!");
    }
}