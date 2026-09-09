package com.finger.handoff.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;

    @Value("${firebase.credential.path:classpath:firebase-adminsdk.json}")
    private String credentialPath;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            Resource resource = resourceLoader.getResource(credentialPath);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(is))
                            .build();
                    FirebaseApp.initializeApp(options);
                    log.info("FirebaseApp 초기화 성공 (키 경로: {})", credentialPath);
                }
            } else {
                log.warn("Firebase 서비스 계정 키 파일을 찾을 수 없습니다 (경로: {}). FCM 푸시 발송은 모의(Mock) 로그 모드로 동작합니다.", credentialPath);
            }
        } catch (Exception e) {
            log.error("Firebase 초기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
