package com.finger.handoff.domain.user.entity;

public enum ReasonCategory {
    NOT_USED_OFTEN("자주 이용하지 않아요"),
    NO_LONGER_NEEDED("더 이상 필요하지 않아요"),
    TOO_COMPLEX("사용법이 어렵거나 복잡해요"),
    INACCURATE_ANALYSIS("분석 결과가 정확하지 않다고 느꼈어요"),
    MANY_ERRORS("오류가 많아요"),
    ETC("기타");

    private final String description;

    ReasonCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
