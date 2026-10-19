package com.finger.handoff.domain.user.service;

import com.finger.handoff.domain.badge.repository.UserBadgeRepository;
import com.finger.handoff.domain.presentation.repository.PresentationRepository;
import com.finger.handoff.domain.presentation.service.PresentationService;
import com.finger.handoff.domain.review.repository.ReviewRepository;
import com.finger.handoff.domain.terms.repository.UserTermsAgreementRepository;
import com.finger.handoff.domain.user.dto.UserDto;
import com.finger.handoff.domain.user.dto.UserProfileRequest;
import com.finger.handoff.domain.user.dto.UserWithdrawRequest;
import com.finger.handoff.domain.user.entity.ReasonCategory;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.user.entity.UserLeaveLog;
import com.finger.handoff.domain.user.repository.UserLeaveLogRepository;
import com.finger.handoff.domain.user.repository.UserRepository;
import com.finger.handoff.domain.v2.async.repository.FcmTokenRepository;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import com.finger.handoff.global.s3.S3Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserLeaveLogRepository userLeaveLogRepository;
    private final S3Service s3UploadService;
    private final PresentationRepository presentationRepository;
    private final PresentationService presentationService;
    private final UserBadgeRepository userBadgeRepository;
    private final ReviewRepository reviewRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserTermsAgreementRepository userTermsAgreementRepository;

    @Transactional
    public User findOrCreateUser(String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .isTermsAgreement(false)
                                .isProfileComplete(false)
                                .build()
                ));
    }

    @Transactional
    public void updateRefreshToken(Long id, String refreshToken) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateRefreshToken(refreshToken);
    }

    @Transactional
    public void withdraw(Long userId, UserWithdrawRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String profileImgUrl = user.getProfileImgUrl();

        if (profileImgUrl != null && !profileImgUrl.isEmpty()) {
            s3UploadService.deleteProfileImage(profileImgUrl);
        }

        ReasonCategory reasonCategory = (request != null) ? request.getReasonCategory() : null;
        String reasonText = (request != null) ? request.getReasonText() : null;

        UserLeaveLog leaveLog = UserLeaveLog.builder()
                .userId(user.getId())
                .reasonCategory(reasonCategory)
                .reasonText(reasonText)
                .build();
        userLeaveLogRepository.save(leaveLog);

        // 1. 약관 동의 내역 삭제 및 컬렉션 동기화 (외래키 제약조건 방지)
        user.getTermsAgreements().clear();
        userTermsAgreementRepository.deleteAllByUserId(userId);

        // 2. FCM 디바이스 토큰 삭제 (외래키 제약조건 방지)
        fcmTokenRepository.deleteAllByUserId(userId);

        // 3. 발표 및 연관 데이터(회고, 분석결과, S3 오디오 파일, 연습일자 등) 완전 삭제
        presentationService.deleteAllByUser(user);

        // 4. 잔여 회고(Review) 및 뱃지(UserBadge) 삭제
        reviewRepository.deleteAllByUserId(userId);
        userBadgeRepository.deleteAllByUserId(userId);

        // 5. 회원 엔티티 최종 삭제
        userRepository.delete(user);
    }

    public UserDto getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Boolean isProfileComplete = (user.getNickname() != null && !user.getNickname().trim().isEmpty());

        String imageUrl = user.getProfileImgUrl();
        boolean isDefault = (imageUrl == null || imageUrl.trim().isEmpty());

        UserDto.ProfileImageDto profileImgDto = UserDto.ProfileImageDto.builder()
                .url(imageUrl)
                .isDefault(isDefault)
                .build();

        return UserDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImgUrl(profileImgDto)
                .isTermsAgreement(user.getIsTermsAgreement())
                .isProfileComplete(isProfileComplete)
                .build();
    }

    @Transactional
    public void updateProfile(Long userId, UserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String newNickname = request.getNickname();
        if (newNickname != null && !newNickname.trim().isEmpty()) {
            if (user.getNickname() == null || !user.getNickname().equals(newNickname)) {
                if (userRepository.existsByNickname(newNickname)) {
                    throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
                }
                user.updateNickname(newNickname);
            }
        }

        MultipartFile file = request.getProfileImage();
        String currentImgUrl = user.getProfileImgUrl();

        if (Boolean.TRUE.equals(request.getDeleteImage())) {
            if (currentImgUrl != null) {
                s3UploadService.deleteProfileImage(currentImgUrl);
            }
            user.updateProfileImgUrl(null);
        }
        else if (file != null && !file.isEmpty()) {
            if (currentImgUrl != null) {
                s3UploadService.deleteProfileImage(currentImgUrl);
            }
            String uploadedUrl = s3UploadService.uploadProfileImage(file);
            user.updateProfileImgUrl(uploadedUrl);
        }
    }

    public boolean isNicknameAvailable(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return false;
        }

        return !userRepository.existsByNickname(nickname);
    }
}
