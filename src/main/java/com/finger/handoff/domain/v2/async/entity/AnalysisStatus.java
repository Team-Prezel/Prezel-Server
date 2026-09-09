package com.finger.handoff.domain.v2.async.entity;

public enum AnalysisStatus {
    ANALYZING,          // 분석중
    COMPLETED,          // 분석 완료
    ERR_VOICE_RECOG,    // 실패 유형1: 음성 인식 실패
    ERR_FILE_RECOG,     // 실패 유형2: 음성 파일 문제
    ERR_ANALYSIS        // 실패 유형3: 분석 오류
}
