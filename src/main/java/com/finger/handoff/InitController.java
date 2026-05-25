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
        termsRepository.save(Terms.builder()
                .title("이용약관")
                .summary("본 약관은 서비스 이용과 관련한 기본적인 권리·의무 및 책임사항을 규정합니다.")
                .content("")
                .isRequired(true)
                .version("1.0")
                .build());

        termsRepository.save(Terms.builder()
                .title("개인정보 정책")
                .summary("""
                    서비스 제공을 위해 개인정보를 수집·이용합니다. 발표 연습을 위한 음성 녹음 및 분석 데이터 처리 내용이 포함됩니다.
                    
                    수집 항목 : 계정 정보, 음성 녹음 파일, 음성 분석 결과, 발표 대본, 서비스 이용 기록 등
                    
                    수집 목적: 발표 분석, 개인 맞춤 피드백 제공, 연습 기록 관리
                    """.trim())
                .content("")
                .isRequired(true)
                .version("1.0")
                .build());

        termsRepository.save(Terms.builder()
                .title("데이터 활용 동의")
                .summary("서비스 품질 향상 및 기능 개선을 위해 비식별 처리된 분석 데이터를 활용할 수 있습니다.")
                .content("")
                .isRequired(false)
                .version("1.0")
                .build());

        return ResponseEntity.ok("운영 DB 약관 데이터 세팅 완료");
    }
}
