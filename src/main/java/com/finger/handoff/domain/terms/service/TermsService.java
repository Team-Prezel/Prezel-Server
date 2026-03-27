package com.finger.handoff.domain.terms.service;

import com.finger.handoff.domain.terms.dto.TermsAgreementRequest;
import com.finger.handoff.domain.terms.entity.Terms;
import com.finger.handoff.domain.terms.entity.UserTermsAgreement;
import com.finger.handoff.domain.terms.repository.TermsRepository;
import com.finger.handoff.domain.terms.repository.UserTermsAgreementRepository;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.user.repository.UserRepository;
import com.finger.handoff.global.error.exception.BusinessException; // 🌟 임포트 확인
import com.finger.handoff.global.error.model.ErrorCode; // 🌟 임포트 확인
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermsService {

    private final UserRepository userRepository;
    private final TermsRepository termsRepository;
    private final UserTermsAgreementRepository agreementRepository;

    @Transactional
    public void saveAgreements(Long userId, List<TermsAgreementRequest> requests) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Long> agreedTermIds = requests.stream()
                .filter(TermsAgreementRequest::getIsAgreed)
                .map(TermsAgreementRequest::getTermsId)
                .collect(Collectors.toList());

        List<Terms> requiredTerms = termsRepository.findByIsRequiredTrue();

        for (Terms requiredTerm : requiredTerms) {
            if (!agreedTermIds.contains(requiredTerm.getId())) {
                throw new BusinessException(ErrorCode.REQUIRED_TERMS_DISAGREED);
            }
        }

        List<UserTermsAgreement> agreements = requests.stream().map(request -> {
            Terms terms = termsRepository.findById(request.getTermsId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

            return UserTermsAgreement.builder()
                    .user(user)
                    .terms(terms)
                    .isAgreed(request.getIsAgreed())
                    .build();
        }).collect(Collectors.toList());

        agreementRepository.saveAll(agreements);
    }
}