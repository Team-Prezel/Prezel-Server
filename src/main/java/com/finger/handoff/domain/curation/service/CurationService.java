package com.finger.handoff.domain.curation.service;

import com.finger.handoff.domain.curation.dto.CurationResponse;
import com.finger.handoff.domain.curation.entity.CurationData;
import com.finger.handoff.domain.curation.entity.DDayRange;
import com.finger.handoff.domain.curation.entity.PresentationType;
import com.finger.handoff.domain.curation.repository.CurationRepository;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.presentation.repository.PresentationRepository;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurationService {

    private final PresentationRepository presentationRepository;
    private final CurationRepository curationRepository;

    public List<CurationResponse> getCurationList(Long presentationId) {

        Presentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRESENTATION_NOT_FOUND));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate targetDate = presentation.getPresentationDate();

        if (targetDate == null) {
            log.warn("Presentation ID {}의 presentationDate가 null", presentationId);
            targetDate = today;
        }

        long daysLeft = ChronoUnit.DAYS.between(today, targetDate);

        DDayRange currentRange = determineDDayRange(daysLeft);

        if (presentation.getType() == null) {
            log.error("Presentation ID {}의 발표 유형이 null", presentationId);
            throw new BusinessException(ErrorCode.CURATION_NOT_FOUND);
        }

        PresentationType type = PresentationType.valueOf(presentation.getType().name());

        long totalCount = curationRepository.count();
        log.info("[DB 전체 데이터 개수 : {} ]", totalCount);
        log.info("[검색 조건: Type = {}, DDay = {} ]", type, currentRange);

        List<CurationData> curationDataList = curationRepository
                .findByPresentationTypeAndDDayRangeOrderByRecommendOrderAsc(type, currentRange);

        if (curationDataList == null || curationDataList.isEmpty()) {
            throw new BusinessException(ErrorCode.CURATION_NOT_FOUND);
        }

        return curationDataList.stream()
                .map(CurationResponse::from)
                .toList();
    }

    private DDayRange determineDDayRange(long daysLeft) {
        if (daysLeft >= 7) {
            return DDayRange.D_7_PLUS;
        } else if (daysLeft >= 3) {
            return DDayRange.D_6_TO_3;
        } else if (daysLeft >= 1) {
            return DDayRange.D_2_TO_1;
        } else {
            return DDayRange.D_DAY;
        }
    }
}