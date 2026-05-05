package com.finger.handoff.domain.user.service;

import com.finger.handoff.domain.user.dto.UserDto;
import com.finger.handoff.domain.user.dto.UserProfileRequest;
import com.finger.handoff.domain.user.dto.UserWithdrawRequest;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.user.entity.UserLeaveLog;
import com.finger.handoff.domain.user.repository.UserLeaveLogRepository;
import com.finger.handoff.domain.user.repository.UserRepository;
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

        UserLeaveLog leaveLog = UserLeaveLog.builder()
                .userId(user.getId())
                .reasonCategory(request.getReasonCategory())
                .reasonText(request.getReasonText())
                .build();
        userLeaveLogRepository.save(leaveLog);

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

        if (Boolean.TRUE.equals(request.getIsImageDeleted())) {
            if (user.getProfileImgUrl() != null) {
                s3UploadService.deleteProfileImage(user.getProfileImgUrl());
            }
            user.updateProfile(user.getNickname(), null);
        }
        else if (file != null && !file.isEmpty()) {
            if (user.getProfileImgUrl() != null) {
                s3UploadService.deleteProfileImage(user.getProfileImgUrl());
            }
            String uploadedUrl = s3UploadService.uploadProfileImage(file);
            user.updateProfile(user.getNickname(), uploadedUrl);
        }
    }

    public boolean isNicknameAvailable(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return false;
        }

        return !userRepository.existsByNickname(nickname);
    }
}
