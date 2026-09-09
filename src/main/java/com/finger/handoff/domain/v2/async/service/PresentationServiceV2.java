package com.finger.handoff.domain.v2.async.service;

import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.domain.presentation.entity.AnalysisResult;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.v2.async.dto.PresentationCardResponse;
import com.finger.handoff.domain.presentation.repository.AnalysisResultRepository;
import com.finger.handoff.domain.presentation.repository.PresentationRepository;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.v2.async.entity.AnalysisStatus;
import com.finger.handoff.global.audio.AudioConverter;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import com.finger.handoff.global.s3.S3Service;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresentationServiceV2 {

    private final PresentationRepository presentationRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final S3Service s3Service;
    private final AudioConverter audioConverter;

    @Getter
    @Builder
    public static class InitDataDto {
        private Long presentationId;
        private Long analysisResultId;
        private String localWavPath;
        private String finalScript;
        private boolean isNew;
    }

    @Transactional
    public InitDataDto createInitialData(PresentationDTO.PresentationRequest request, String finalScript, User user) {
        String audioUrl = s3Service.uploadAudioFile(request.getAudio());
        File wavFile = audioConverter.convertToWav(request.getAudio());

        Presentation presentation = Presentation.builder()
                .user(user)
                .title(request.getName())
                .presentationDate(request.getDate())
                .type(request.getType())
                .purpose(request.getPurpose())
                .style(request.getStyle())
                .audience(request.getAudience())
                .script(finalScript)
                .build();
        presentationRepository.save(presentation);

        AnalysisResult analysisResult = AnalysisResult.builder()
                .presentation(presentation)
                .status(AnalysisStatus.ANALYZING)
                .audioUrl(audioUrl)
                .build();
        analysisResultRepository.save(analysisResult);

        return InitDataDto.builder()
                .presentationId(presentation.getId())
                .analysisResultId(analysisResult.getId())
                .localWavPath(wavFile.getAbsolutePath())
                .finalScript(finalScript)
                .isNew(true)
                .build();
    }

    @Transactional
    public InitDataDto prepareReAnalysisData(Long presentationId, MultipartFile newAudio, String newScript, User user) {
        Presentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRESENTATION_NOT_FOUND));

        if (!presentation.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        var historyResults = analysisResultRepository.findByPresentationIdOrderByCreatedAtAsc(presentationId);
        if (historyResults.isEmpty()) throw new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND);

        AnalysisResult latestResult = historyResults.get(historyResults.size() - 1);
        String targetAudioUrl = latestResult.getAudioUrl();
        String finalScript = newScript != null ? newScript : presentation.getScript();
        File wavFile;

        if (newAudio != null && !newAudio.isEmpty()) {
            s3Service.deleteAudioFile(targetAudioUrl);
            targetAudioUrl = s3Service.uploadAudioFile(newAudio);
            wavFile = audioConverter.convertToWav(newAudio);
        } else {
            if (targetAudioUrl == null || targetAudioUrl.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.FILE_IS_EMPTY);
            }
            File downloadedAudio = s3Service.downloadAudioFile(targetAudioUrl);
            wavFile = audioConverter.convertToWav(downloadedAudio);
        }

        if (newScript != null) {
            presentation.updateScript(newScript);
        }

        AnalysisResult newResult = AnalysisResult.builder()
                .presentation(presentation)
                .status(AnalysisStatus.ANALYZING)
                .audioUrl(targetAudioUrl)
                .build();
        analysisResultRepository.save(newResult);

        return InitDataDto.builder()
                .presentationId(presentation.getId())
                .analysisResultId(newResult.getId())
                .localWavPath(wavFile.getAbsolutePath())
                .finalScript(finalScript)
                .isNew(false)
                .build();
    }


    @Transactional(readOnly = true)
    public List<PresentationCardResponse> getUpcomingCards(User user) {
        LocalDate today = LocalDate.now();
        List<Presentation> presentations = presentationRepository
                .findByUserIdAndPresentationDateGreaterThanEqualOrderByPresentationDateAsc(user.getId(), today);

        return presentations.stream()
                .map(p -> mapToCardResponse(p, today))
                .filter(c -> c != null)
                .toList();
    }

    private PresentationCardResponse mapToCardResponse(Presentation presentation, LocalDate today) {
        List<AnalysisResult> historyResults = analysisResultRepository.findByPresentationIdOrderByCreatedAtAsc(presentation.getId());
        if (historyResults.isEmpty()) {
            return null;
        }

        AnalysisResult latestResult = historyResults.get(historyResults.size() - 1);
        AnalysisStatus status = latestResult.getStatus();

        String cardStatus;
        String errorType = null;
        String script = null;
        String audioUrl = null;

        if (status == AnalysisStatus.ANALYZING) {
            cardStatus = "inProcess";
        } else if (status == AnalysisStatus.COMPLETED) {
            cardStatus = Boolean.TRUE.equals(latestResult.getIsViewed()) ? "default" : "complete";
        } else {
            cardStatus = "fail";
            errorType = status.name();
            script = presentation.getScript();
            if (status == AnalysisStatus.ERR_ANALYSIS) {
                audioUrl = latestResult.getAudioUrl();
            }
        }

        LocalDate targetDate = presentation.getPresentationDate();
        String dDay = "";
        if (targetDate != null) {
            long days = ChronoUnit.DAYS.between(today, targetDate);
            if (days == 0) {
                dDay = "D-Day";
            } else if (days > 0) {
                dDay = "D-" + days;
            } else {
                dDay = "D+" + Math.abs(days);
            }
        }

        return PresentationCardResponse.builder()
                .presentationId(presentation.getId())
                .analysisResultId(latestResult.getId())
                .title(presentation.getTitle())
                .presentationDate(presentation.getPresentationDate())
                .dDay(dDay)
                .type(presentation.getType())
                .cardStatus(cardStatus)
                .errorType(errorType)
                .script(script)
                .audioUrl(audioUrl)
                .build();
    }
}