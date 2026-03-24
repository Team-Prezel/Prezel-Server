package com.finger.handoff.domain.user.service;

import com.finger.handoff.domain.user.dto.UserDto;
import com.finger.handoff.domain.user.dto.UserWithdrawRequest;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.user.entity.UserLeaveLog;
import com.finger.handoff.domain.user.repository.UserLeaveLogRepository;
import com.finger.handoff.domain.user.repository.UserRepository;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserLeaveLogRepository userLeaveLogRepository;

    @Transactional
    public User findOrCreateUser(String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
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

        return UserDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImgUrl(user.getProfileImgUrl())
                .build();
    }

    @Transactional
    public UserDto updateUserNickname(Long userId, String newNickname) {
        if (userRepository.existsByNickname(newNickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateNickname(newNickname);
        return UserDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImgUrl(user.getProfileImgUrl())
                .build();
    }
}
