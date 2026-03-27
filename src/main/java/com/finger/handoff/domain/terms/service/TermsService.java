package com.finger.handoff.domain.terms.service;

import com.finger.handoff.domain.terms.dto.TermsAgreementRequest;
import com.finger.handoff.domain.terms.entity.Terms;
import com.finger.handoff.domain.terms.entity.UserTermsAgreement;
import com.finger.handoff.domain.terms.repository.TermsRepository;
import com.finger.handoff.domain.terms.repository.UserTermsAgreementRepository;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.user.repository.UserRepository;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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

        long uniqueTermCount = requests.stream()
                .map(TermsAgreementRequest::getTermsId)
                .distinct()
                .count();

        if (uniqueTermCount != requests.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

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

        for (TermsAgreementRequest request : requests) {
            Terms terms = termsRepository.findById(request.getTermsId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

            Optional<UserTermsAgreement> existingAgreement = agreementRepository.findByUserAndTerms(user, terms);

            if (existingAgreement.isPresent()) {
                existingAgreement.get().updateAgreement(request.getIsAgreed());
            } else {
                UserTermsAgreement newAgreement = UserTermsAgreement.builder()
                        .user(user)
                        .terms(terms)
                        .isAgreed(request.getIsAgreed())
                        .build();
                agreementRepository.save(newAgreement);
            }
        }
    }
}