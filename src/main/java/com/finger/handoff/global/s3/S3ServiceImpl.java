package com.finger.handoff.global.s3;

import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Override
    public String uploadProfileImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_IS_EMPTY);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);

        String s3FileName = "profile/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3FileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return s3Client.utilities().getUrl(GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(s3FileName)
                    .build()).toExternalForm();

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void deleteProfileImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty() || !imageUrl.contains("amazonaws.com")) {
            return;
        }

        try {
            String s3Key = extractKeyFromUrl(imageUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (Exception e) {
            /**
             * 예외 터져도 그냥 냅둠 -> 프로필 사진 안 지워져도 일단 바꾸긴 해야함
             * (사진 등록 로직에 기존 사진 있을시 삭제하라는 로직 넣을거 => 등록 및 수정 로직을 하나로 합침)
             * 이거 안잡으면 @Transactional 땜에 롤백 당함 -> 유저는 평생 사진 못바꾸는 버그 발생
             */
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    private String extractKeyFromUrl(String imageUrl) {
        String splitStr = ".com/";
        if (imageUrl.contains(splitStr)) {
            return imageUrl.substring(imageUrl.indexOf(splitStr) + splitStr.length());
        }
        return imageUrl;
    }
}

