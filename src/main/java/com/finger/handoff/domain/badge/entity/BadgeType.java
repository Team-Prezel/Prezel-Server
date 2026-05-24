package com.finger.handoff.domain.badge.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BadgeType {

    START(
            "출발하기",
            "첫 발표를 등록했어요",
            "첫 발표를 등록했어요. 나의 발표 여정의 첫 걸음이에요.",
            "첫 발표 등록",
            "https://s3.../start.png" // 실제 S3 이미지 URL로 변경
    ),
    ANALYZE_AGAIN(
            "기록쌓기",
            "한번 더 분석했어요",
            "발표를 한 번 더 분석했어요. 반복할수록 성장하는 발표를 경험해보세요.",
            "등록한 발표를 한번 더 녹음하기",
            "https://s3.../analyze_again.png"
    ),
    FIRST_PRACTICE(
            "연습하기",
            "첫 연습을 완료했어요",
            "첫 연습 녹음을 마쳤어요. 연습은 성장의 가장 확실한 시작이에요.",
            "연습 녹음 완료하기",
            "https://s3.../first_practice.png"
    ),
    REVIEW(
            "돌아보기",
            "셀프 피드백을 작성했어요",
            "발표를 끝내고 기록을 남겼어요. 스스로 돌아보며 다음 발표를 더 단단하게 만들어요.",
            "발표일이 지난 후 셀프 피드백 작성하기",
            "https://s3.../review.png"
    ),
    PERFECT_SCORE(
            "성장하기",
            "최고점을 받았어요",
            "연습에서 최고 점수를 기록했어요. 지금의 흐름을 이어가보세요.",
            "연습 녹음 결과 Perfect 달성하기",
            "https://s3.../perfect.png"
    ),
    REPEAT_10(
            "반복하기",
            "10번의 발표를 준비했어요",
            "발표를 10번 등록하고 연습했어요. 나만의 발표 감각을 찾아가고 있어요.",
            "발표 10회 등록하기",
            "https://s3.../repeat_10.png"
    );

    private final String badgeName;
    private final String introduction;
    private final String detailDescription;
    private final String conditionText;
    private final String imageUrl;
}