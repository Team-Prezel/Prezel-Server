package com.finger.handoff.domain.terms.controller;

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
        // 1. (필수) 이용약관
        termsRepository.save(Terms.builder()
                .title("이용약관")
                .content("이용약관 상세 내용이 들어갈 자리입니다.")
                .isRequired(true)
                .version("1.0")
                .build());

        // 2. (필수) 개인정보 정책
        termsRepository.save(Terms.builder()
                .title("개인정보 정책")
                .content("개인정보 처리방침 상세 내용이 들어갈 자리입니다.")
                .isRequired(true)
                .version("1.0")
                .build());

        // 3. (선택) 데이터 활용 동의
        termsRepository.save(Terms.builder()
                .title("데이터 활용 동의")
                .content("사용자 발음 데이터 등 서비스 품질 향상을 위한 데이터 활용 동의 내용이 들어갈 자리입니다.")
                .isRequired(false)
                .version("1.0")
                .build());

        return ResponseEntity.ok("운영 DB 약관 데이터 세팅 완료!");
    }
}
