/*
package com.finger.handoff.domain.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.presentation.dto.PresentationDTO.SentenceAnalysisDetail;
import com.finger.handoff.domain.presentation.dto.PresentationDTO.WordAnalysisDetail;
import com.finger.handoff.domain.presentation.entity.*;
import com.finger.handoff.domain.presentation.repository.AnalysisResultRepository;
import com.finger.handoff.domain.presentation.repository.PresentationRepository;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.user.repository.UserRepository;
import com.finger.handoff.domain.v2.async.entity.AnalysisStatus;
import com.finger.handoff.global.common.ApiResponse;
import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Admin Dummy", description = "어드민/개발용 테스트 더미 데이터 관리 API")
@RestController
@RequestMapping("/admin/dummy")
@RequiredArgsConstructor
public class AdminDummyController {

    private final UserRepository userRepository;
    private final PresentationRepository presentationRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final ObjectMapper objectMapper;

    private static final String ADMIN_EMAIL = "admin@admin.com";
    private static final String TITLE_SUCCESS = "[테스트] 에러 유형별 분석 리포트";
    private static final String TITLE_ERR_VOICE = "[테스트] 음성 인식 실패 카드 (ERR_VOICE_RECOG)";
    private static final String TITLE_ERR_FILE = "[테스트] 음성 파일 에러 카드 (ERR_FILE_RECOG)";
    private static final String TITLE_ERR_ANALYSIS = "[테스트] 분석 중 문제 발생 카드 (ERR_ANALYSIS)";

    private static final List<String> DUMMY_TITLES = List.of(
            TITLE_SUCCESS,
            TITLE_ERR_VOICE,
            TITLE_ERR_FILE,
            TITLE_ERR_ANALYSIS
    );

    @Operation(
            summary = "에러 유형별 더미 발표 리포트 및 카드 4종 생성",
            description = "로그인 유저 또는 지정된 email(미입력시 admin@admin.com)에 '성공 리포트' 및 '실패 카드 3종(ERR_VOICE_RECOG, ERR_FILE_RECOG, ERR_ANALYSIS)'을 생성합니다. 기존 더미 데이터가 있다면 삭제 후 새로 생성합니다."
    )
    @PostMapping("/error-types")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> createErrorTypesDummy(
            @RequestParam(required = false) String email,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) throws JsonProcessingException {

        // 1. 대상 유저 결정 (email 파라미터 우선 -> 로그인 유저 -> admin@admin.com)
        User targetUser = resolveAdminUser(email, customUserDetails);

        // 2. 기존 동일 제목의 더미 데이터가 있으면 멱등성을 위해 삭제
        List<Presentation> existingList = presentationRepository.findByUserId(targetUser.getId());
        for (Presentation p : existingList) {
            if (DUMMY_TITLES.contains(p.getTitle())) {
                List<AnalysisResult> results = analysisResultRepository.findByPresentationIdOrderByCreatedAtAsc(p.getId());
                analysisResultRepository.deleteAll(results);
                analysisResultRepository.flush();
                presentationRepository.delete(p);
                presentationRepository.flush();
            }
        }

        List<Map<String, Object>> createdItems = new ArrayList<>();

        // 3. [케이스 1: 성공 리포트 (4종 칩 포함)]
        String script = "안녕하세요 오늘 프로젝트 발표를 시작하겠습니다. " +
                "저희 팀은 인공지능 기반 음성 분석 솔루션을 만듭니다. " +
                "대본에 있는 내용을 끝까지 전달하는 것이 매우 중요합니다. " +
                "지금부터 주요 기능 세 가지를 차례대로 소개해 드리겠습니다.";

        Presentation successPres = presentationRepository.save(Presentation.builder()
                .user(targetUser)
                .title(TITLE_SUCCESS)
                .script(script)
                .presentationDate(LocalDate.now().plusDays(3))
                .type(PresentationType.WORK)
                .audience(PresentationAudience.GENERAL)
                .purpose(PresentationPurpose.INFO)
                .style(PresentationStyle.FORMAL)
                .build());

        List<SentenceAnalysisDetail> sentenceDetails = createSentenceDetails();
        String wordDetailsJson = objectMapper.writeValueAsString(sentenceDetails);
        String scriptDetailsJson = createScriptDetailsJson();
        String expectedQuestionsJson = createExpectedQuestionsJson();

        AnalysisResult successResult = analysisResultRepository.save(AnalysisResult.builder()
                .presentation(successPres)
                .status(AnalysisStatus.COMPLETED)
                .isViewed(false)
                .accuracyScore(84.5)
                .scriptMatchRate(75.0)
                .spm(235)
                .speedEval("적당해요")
                .durationSeconds(65)
                .spellErrorCount(1)
                .grammarErrorCount(1)
                .audioUrl("https://axscunf5cln9.compat.objectstorage.ap-chuncheon-1.oraclecloud.com/handoff-bucket/audio/test.wav")
                .summaryFeedback("전반적인 말하기 속도와 전달력은 훌륭합니다. 일부 발음의 정확도를 높이고, 대본에서 누락된 문장 및 불필요한 추임새를 줄이면 더욱 완성도 높은 발표가 될 것입니다.")
                .wordDetailsJson(wordDetailsJson)
                .scriptDetailsJson(scriptDetailsJson)
                .expectedQuestionsJson(expectedQuestionsJson)
                .build());

        createdItems.add(Map.of(
                "title", TITLE_SUCCESS,
                "presentationId", successPres.getId(),
                "analysisResultId", successResult.getId(),
                "status", "COMPLETED",
                "cardStatus", "complete",
                "description", "4종 칩(훌륭해요, 발음, 누락, 불필요한 표현) 완성형 리포트"
        ));

        // 4. [케이스 2: 실패 유형 1 - 음성 인식 실패 (ERR_VOICE_RECOG)]
        Presentation voiceErrPres = presentationRepository.save(Presentation.builder()
                .user(targetUser)
                .title(TITLE_ERR_VOICE)
                .script("음성 인식 실패 테스트용 대본입니다.")
                .presentationDate(LocalDate.now().plusDays(2))
                .type(PresentationType.EDUCATION)
                .audience(PresentationAudience.TEAMMATE)
                .purpose(PresentationPurpose.UNDERSTANDING)
                .style(PresentationStyle.CASUAL)
                .build());

        AnalysisResult voiceErrResult = analysisResultRepository.save(AnalysisResult.builder()
                .presentation(voiceErrPres)
                .status(AnalysisStatus.ERR_VOICE_RECOG)
                .isViewed(false)
                .build());

        createdItems.add(Map.of(
                "title", TITLE_ERR_VOICE,
                "presentationId", voiceErrPres.getId(),
                "analysisResultId", voiceErrResult.getId(),
                "status", "ERR_VOICE_RECOG",
                "cardStatus", "fail",
                "errorType", "ERR_VOICE_RECOG"
        ));

        // 5. [케이스 3: 실패 유형 2 - 음성 파일 문제 (ERR_FILE_RECOG)]
        Presentation fileErrPres = presentationRepository.save(Presentation.builder()
                .user(targetUser)
                .title(TITLE_ERR_FILE)
                .script("음성 파일 에러 테스트용 대본입니다.")
                .presentationDate(LocalDate.now().plusDays(1))
                .type(PresentationType.OFFER)
                .audience(PresentationAudience.PROFESSIONAL)
                .purpose(PresentationPurpose.EMPATHY)
                .style(PresentationStyle.FORMAL)
                .build());

        AnalysisResult fileErrResult = analysisResultRepository.save(AnalysisResult.builder()
                .presentation(fileErrPres)
                .status(AnalysisStatus.ERR_FILE_RECOG)
                .isViewed(false)
                .build());

        createdItems.add(Map.of(
                "title", TITLE_ERR_FILE,
                "presentationId", fileErrPres.getId(),
                "analysisResultId", fileErrResult.getId(),
                "status", "ERR_FILE_RECOG",
                "cardStatus", "fail",
                "errorType", "ERR_FILE_RECOG"
        ));

        // 6. [케이스 4: 실패 유형 3 - 분석 오류 (ERR_ANALYSIS, 음성 재사용 URL 포함)]
        Presentation analysisErrPres = presentationRepository.save(Presentation.builder()
                .user(targetUser)
                .title(TITLE_ERR_ANALYSIS)
                .script("분석 오류 테스트용 대본입니다. 음성 재사용이 가능합니다.")
                .presentationDate(LocalDate.now())
                .type(PresentationType.WORK)
                .audience(PresentationAudience.GENERAL)
                .purpose(PresentationPurpose.INFO)
                .style(PresentationStyle.FORMAL)
                .build());

        AnalysisResult analysisErrResult = analysisResultRepository.save(AnalysisResult.builder()
                .presentation(analysisErrPres)
                .status(AnalysisStatus.ERR_ANALYSIS)
                .isViewed(false)
                .audioUrl("https://axscunf5cln9.compat.objectstorage.ap-chuncheon-1.oraclecloud.com/handoff-bucket/audio/test.wav")
                .build());

        createdItems.add(Map.of(
                "title", TITLE_ERR_ANALYSIS,
                "presentationId", analysisErrPres.getId(),
                "analysisResultId", analysisErrResult.getId(),
                "status", "ERR_ANALYSIS",
                "cardStatus", "fail",
                "errorType", "ERR_ANALYSIS",
                "audioUrl", "https://axscunf5cln9.compat.objectstorage.ap-chuncheon-1.oraclecloud.com/handoff-bucket/audio/test.wav"
        ));

        log.info("[AdminDummy] 더미 발표 4종 생성 완료 - 대상 유저: {} (id={})", targetUser.getEmail(), targetUser.getId());

        Map<String, Object> responseData = Map.of(
                "targetUserEmail", targetUser.getEmail(),
                "targetUserId", targetUser.getId(),
                "totalCreated", createdItems.size(),
                "items", createdItems
        );

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @Operation(
            summary = "에러 유형별 더미 발표 리포트 및 카드 4종 삭제",
            description = "대상 유저의 더미 발표 4종 및 분석 결과를 모두 삭제합니다."
    )
    @DeleteMapping("/error-types")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteErrorTypesDummy(
            @RequestParam(required = false) String email,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User targetUser = resolveAdminUser(email, customUserDetails);
        List<Presentation> list = presentationRepository.findByUserId(targetUser.getId());
        int deletedCount = 0;
        for (Presentation p : list) {
            if (DUMMY_TITLES.contains(p.getTitle())) {
                List<AnalysisResult> results = analysisResultRepository.findByPresentationIdOrderByCreatedAtAsc(p.getId());
                analysisResultRepository.deleteAll(results);
                analysisResultRepository.flush();
                presentationRepository.delete(p);
                presentationRepository.flush();
                deletedCount++;
            }
        }
        return ResponseEntity.ok(ApiResponse.success("더미 발표 데이터 " + deletedCount + "건이 삭제되었습니다."));
    }

    private User resolveAdminUser(String email, CustomUserDetails customUserDetails) {
        if (email != null && !email.trim().isEmpty()) {
            return userRepository.findByEmail(email.trim()).orElseGet(() -> {
                User newUser = User.builder()
                        .email(email.trim())
                        .nickname("테스트유저")
                        .isTermsAgreement(true)
                        .isProfileComplete(true)
                        .build();
                return userRepository.save(newUser);
            });
        }
        if (customUserDetails != null && customUserDetails.getUser() != null) {
            return customUserDetails.getUser();
        }
        return userRepository.findByEmail(ADMIN_EMAIL).orElseGet(() -> {
            User newUser = User.builder()
                    .email(ADMIN_EMAIL)
                    .nickname("관리자")
                    .isTermsAgreement(true)
                    .isProfileComplete(true)
                    .build();
            return userRepository.save(newUser);
        });
    }

    private List<SentenceAnalysisDetail> createSentenceDetails() {
        List<SentenceAnalysisDetail> list = new ArrayList<>();

        // 1. [훌륭해요] - 발음 선명하고 일치도 높음
        list.add(SentenceAnalysisDetail.builder()
                .sentence("안녕하세요 오늘 프로젝트 발표를 시작하겠습니다.")
                .guideScript("안녕하세요 오늘 프로젝트 발표를 시작하겠습니다.")
                .status("훌륭해요")
                .mainFeedback("발음이 선명하게 잘 들려요.")
                .subFeedback("다음 문장에서도 지금처럼 또렷한 발음을 유지해보세요.")
                .accuracy(96.0)
                .startTimeMs(0L)
                .endTimeMs(3500L)
                .wordDetails(List.of(
                        WordAnalysisDetail.builder().word("안녕하세요").status("Excellent").accuracy(98.0).startTimeMs(0L).endTimeMs(800L).build(),
                        WordAnalysisDetail.builder().word("오늘").status("Excellent").accuracy(95.0).startTimeMs(810L).endTimeMs(1200L).build(),
                        WordAnalysisDetail.builder().word("프로젝트").status("Excellent").accuracy(96.0).startTimeMs(1210L).endTimeMs(1800L).build(),
                        WordAnalysisDetail.builder().word("발표를").status("Good").accuracy(92.0).startTimeMs(1810L).endTimeMs(2300L).build(),
                        WordAnalysisDetail.builder().word("시작하겠습니다.").status("Excellent").accuracy(97.0).startTimeMs(2310L).endTimeMs(3500L).build()
                ))
                .build());

        // 2. [발음] (불일치) - 발음 오류(Mispronunciation) 포함
        list.add(SentenceAnalysisDetail.builder()
                .sentence("저희 팀은 인공지능 기반 음성 분석 솔루션을 만듭니다.")
                .guideScript("저희 팀은 인공지능 기반 음성 분석 솔루션을 만듭니다.")
                .status("발음")
                .mainFeedback("일부 단어가 대본과 다르게 들려요.")
                .subFeedback("단어의 발음이 명확하지 않습니다. 다시 한 번 또박또박 연습해 보세요.")
                .accuracy(78.5)
                .startTimeMs(3800L)
                .endTimeMs(8200L)
                .wordDetails(List.of(
                        WordAnalysisDetail.builder().word("저희").status("Good").accuracy(92.0).startTimeMs(3800L).endTimeMs(4200L).build(),
                        WordAnalysisDetail.builder().word("팀은").status("Good").accuracy(90.0).startTimeMs(4210L).endTimeMs(4600L).build(),
                        WordAnalysisDetail.builder().word("인공지능").status("Good").accuracy(91.0).startTimeMs(4610L).endTimeMs(5300L).build(),
                        WordAnalysisDetail.builder().word("기반").status("Mispronunciation").accuracy(52.0).startTimeMs(5310L).endTimeMs(5800L).build(),
                        WordAnalysisDetail.builder().word("음성").status("Good").accuracy(89.0).startTimeMs(5810L).endTimeMs(6300L).build(),
                        WordAnalysisDetail.builder().word("분석").status("Good").accuracy(90.0).startTimeMs(6310L).endTimeMs(6800L).build(),
                        WordAnalysisDetail.builder().word("솔루션을").status("Mispronunciation").accuracy(48.0).startTimeMs(6810L).endTimeMs(7500L).build(),
                        WordAnalysisDetail.builder().word("만듭니다.").status("Good").accuracy(93.0).startTimeMs(7510L).endTimeMs(8200L).build()
                ))
                .build());

        // 3. [누락] - 대본 문장을 통째로 건너뛰어 누락됨 (accuracy 0.0, Omission 단어)
        list.add(SentenceAnalysisDetail.builder()
                .sentence("대본에 있는 내용을 끝까지 전달하는 것이 매우 중요합니다.")
                .guideScript("대본에 있는 내용을 끝까지 전달하는 것이 매우 중요합니다.")
                .status("누락")
                .mainFeedback("대본의 일부 내용이 빠졌어요.")
                .subFeedback("문장을 끝까지 읽을 수 있도록 대본에 집중해 보세요.")
                .accuracy(0.0)
                .startTimeMs(8500L)
                .endTimeMs(8500L)
                .wordDetails(List.of(
                        WordAnalysisDetail.builder().word("대본에").status("Omission").accuracy(0.0).startTimeMs(8500L).endTimeMs(8500L).build(),
                        WordAnalysisDetail.builder().word("있는").status("Omission").accuracy(0.0).startTimeMs(8500L).endTimeMs(8500L).build(),
                        WordAnalysisDetail.builder().word("내용을").status("Omission").accuracy(0.0).startTimeMs(8500L).endTimeMs(8500L).build(),
                        WordAnalysisDetail.builder().word("끝까지").status("Omission").accuracy(0.0).startTimeMs(8500L).endTimeMs(8500L).build(),
                        WordAnalysisDetail.builder().word("전달하는").status("Omission").accuracy(0.0).startTimeMs(8500L).endTimeMs(8500L).build(),
                        WordAnalysisDetail.builder().word("것이").status("Omission").accuracy(0.0).startTimeMs(8500L).endTimeMs(8500L).build(),
                        WordAnalysisDetail.builder().word("매우").status("Omission").accuracy(0.0).startTimeMs(8500L).endTimeMs(8500L).build(),
                        WordAnalysisDetail.builder().word("중요합니다.").status("Omission").accuracy(0.0).startTimeMs(8500L).endTimeMs(8500L).build()
                ))
                .build());

        // 4. [불필요한 표현] - 추임새('어...', '그') 및 더듬거림 포함
        list.add(SentenceAnalysisDetail.builder()
                .sentence("지금부터 주요 기능 세 가지를 차례대로 소개해 드리겠습니다.")
                .guideScript("지금부터 주요 기능 세 가지를 차례대로 소개해 드리겠습니다.")
                .status("불필요한 표현")
                .mainFeedback("말 사이에 불필요한 표현이 들어갔어요.")
                .subFeedback("생각할 시간이 필요할 때는 잠깐 멈춘 뒤 이어서 말해보세요.")
                .accuracy(89.0)
                .startTimeMs(8800L)
                .endTimeMs(14500L)
                .wordDetails(List.of(
                        WordAnalysisDetail.builder().word("어...").status("Stutter").accuracy(80.0).startTimeMs(8800L).endTimeMs(9300L).build(),
                        WordAnalysisDetail.builder().word("지금부터").status("Good").accuracy(92.0).startTimeMs(9310L).endTimeMs(9900L).build(),
                        WordAnalysisDetail.builder().word("그").status("Stutter").accuracy(75.0).startTimeMs(9910L).endTimeMs(10300L).build(),
                        WordAnalysisDetail.builder().word("주요").status("Good").accuracy(90.0).startTimeMs(10310L).endTimeMs(10800L).build(),
                        WordAnalysisDetail.builder().word("기능").status("Good").accuracy(91.0).startTimeMs(10810L).endTimeMs(11300L).build(),
                        WordAnalysisDetail.builder().word("세").status("Good").accuracy(92.0).startTimeMs(11310L).endTimeMs(11700L).build(),
                        WordAnalysisDetail.builder().word("가지를").status("Good").accuracy(93.0).startTimeMs(11710L).endTimeMs(12300L).build(),
                        WordAnalysisDetail.builder().word("차례대로").status("Good").accuracy(94.0).startTimeMs(12310L).endTimeMs(13100L).build(),
                        WordAnalysisDetail.builder().word("소개해").status("Good").accuracy(95.0).startTimeMs(13110L).endTimeMs(13800L).build(),
                        WordAnalysisDetail.builder().word("드리겠습니다.").status("Good").accuracy(95.0).startTimeMs(13810L).endTimeMs(14500L).build()
                ))
                .build());

        return list;
    }

    private String createScriptDetailsJson() {
        return """
                [
                  {
                    "errorType": "SPELLING",
                    "sentence": "대본에 있는 내용을 끝까지 전달하는 것이 매우 중요합니다.",
                    "originalText": "매우 중요합니다",
                    "correctedText": "매우 중요합니다.",
                    "reason": "문장의 끝맺음 기호가 누락되지 않도록 명확하게 표기하는 것이 좋습니다.",
                    "startIndex": 0,
                    "endIndex": 10
                  },
                  {
                    "errorType": "GRAMMAR",
                    "sentence": "지금부터 주요 기능 세 가지를 차례대로 소개해 드리겠습니다.",
                    "originalText": "소개해 드리겠습니다",
                    "correctedText": "소개해 드리겠습니다",
                    "reason": "청중에게 정중한 어조로 내용을 전달하고 있습니다.",
                    "startIndex": 0,
                    "endIndex": 10
                  }
                ]
                """.trim();
    }

    private String createExpectedQuestionsJson() {
        return """
                [
                  {
                    "question": "음성 분석 과정에서 누락된 문장을 판별하는 기준은 무엇인가요?",
                    "answer": "대본의 원본 문장 리스트와 실제 발화된 음성 인식 토큰의 시간 정보 및 일치도를 비교하여, 발화되지 않은 문장을 실시간으로 추적 및 감지합니다."
                  },
                  {
                    "question": "발음 정확도 점수는 어떤 지표를 기반으로 산출되나요?",
                    "answer": "Azure AI 음성 평가 엔진의 음소(Phoneme) 단위 분석과 종합 신뢰도 점수를 기반으로 100점 만점으로 정량화하여 산출합니다."
                  },
                  {
                    "question": "불필요한 추임새는 어떻게 감지하나요?",
                    "answer": "'어', '음', '그' 등 반복되는 필러 단어와 말더듬 패턴을 토큰 레벨에서 필터링하여 감지합니다."
                  }
                ]
                """.trim();
    }
}
*/
