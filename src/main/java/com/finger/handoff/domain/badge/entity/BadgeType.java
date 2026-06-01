package com.finger.handoff.domain.badge.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BadgeType {

    START(
            "시작이 반",
            "시작이 반 뱃지를 획득했어요!",
            "첫 발표를 등록하며 연습을 시작했어요.나의 발표 여정의 첫 걸음이에요.",
            "첫 발표 등록하기",
            "https://handoff-profile-639135896142-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/badges/START01.png"
    ),
    ANALYZE_AGAIN(
            "감 잡는 중",
            "감 잡는 중 뱃지를 획득했어요!",
            "발표를 한 번 더 분석했어요. 반복할수록 성장하는 발표를 경험해보세요.",
            "등록한 발표 3회 분석하기",
            "https://handoff-profile-639135896142-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/badges/ANALYZE_AGAIN01.png"
    ),
    FIRST_PRACTICE(
            "워밍업",
            "워밍업 뱃지를 획득했어요!",
            "첫 연습 녹음을 마쳤어요. 연습은 성장의 가장 확실한 시작이에요.",
            "연습 녹음 완료하기",
            "https://handoff-profile-639135896142-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/badges/FIRST_PRACTICE01.png"
    ),
    REVIEW(
            "끝까지 해냄",
            "끝까지 해냄 뱃지를 획득했어요!",
            "발표를 끝내고 기록을 남겼어요. 스스로 돌아보며 다음 발표를 더 단단하게 만들어요.",
            "발표일이 지난 후 피드백 작성하기",
            "https://handoff-profile-639135896142-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/badges/REVIEW01.png"
    ),
    PERFECT_SCORE(
            "컨디션 최고",
            "컨디션 최고 뱃지를 획득했어요!",
            "연습에서 최고 점수를 기록했어요. 지금의 흐름을 이어가보세요.",
            "연습 녹음 결과 Perfect 달성하기",
            "https://handoff-profile-639135896142-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/badges/PERFECT_SCORE01.png"
    ),
    REPEAT_10(
            "감 잡았다",
            "감 잡았다 뱃지를 획득했어요!",
            "발표를 10번 등록하고 연습했어요. 나만의 발표 감각을 찾아가고 있어요.",
            "발표 10회 등록하기",
            "https://handoff-profile-639135896142-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/badges/REPEAT_1001.png"
    );

    private final String badgeName;
    private final String introduction;
    private final String detailDescription;
    private final String conditionText;
    private final String imageUrl;
}