package com.finger.handoff.domain.review.dto;

import com.finger.handoff.domain.review.entity.Review;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class ReviewDto {
    public record Request(
            @NotBlank(message = "회고 내용을 입력해주세요.")
            @Size(max = 200, message = "셀프 피드백은 최대 200자까지 입력 가능합니다.")
            String content
    ) {
    }

    public record Response(
            Long presentationId,
            String content,
            LocalDateTime createdAt
    ) {
        public static Response from(Review review) {
            return new Response(
                    review.getId(),
                    review.getContent(),
                    review.getCreatedAt()
            );
        }
    }
}