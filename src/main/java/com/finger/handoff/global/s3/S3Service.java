package com.finger.handoff.global.s3;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {

    String uploadProfileImage(MultipartFile file);

    void deleteProfileImage(String imageUrl);

    String uploadAudioFile(MultipartFile file);

    void deleteAudioFile(String fileUrl);

    String uploadBadgeImage(MultipartFile file, String badgeName);
}
