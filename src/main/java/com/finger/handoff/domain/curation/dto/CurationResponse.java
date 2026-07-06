package com.finger.handoff.domain.curation.dto;

import com.finger.handoff.domain.curation.entity.CurationData;
import lombok.Builder;

@Builder
public record CurationResponse(
        String guideMessage,
        String materialType,
        String title,
        String sourceChannel,
        String linkUrl,
        String imageUrl
) {
    public static CurationResponse from(CurationData curationData) {
        return CurationResponse.builder()
                .guideMessage(curationData.getGuideMessage())
                .materialType(curationData.getMaterialType())
                .title(curationData.getTitle())
                .sourceChannel(curationData.getSourceChannel())
                .linkUrl(curationData.getLinkUrl())
                .imageUrl(curationData.getImageUrl())
                .build();
    }
}