package com.finger.handoff.domain.curation.dto;

import com.finger.handoff.domain.curation.entity.CurationData;
import com.finger.handoff.domain.curation.entity.PresentationType;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.presentation.entity.PresentationAudience;
import com.finger.handoff.domain.presentation.entity.PresentationPurpose;
import com.finger.handoff.domain.presentation.entity.PresentationStyle;
import lombok.Builder;

@Builder
public record CurationResponse(
        String guideMessage,
        PresentationType type,
        PresentationPurpose purpose,
        PresentationStyle style,
        PresentationAudience audience,
        String materialType,
        String title,
        String sourceChannel,
        String linkUrl,
        String imageUrl
) {
    public static CurationResponse from(CurationData curationData, Presentation presentation) {
        return CurationResponse.builder()
                .guideMessage(curationData.getGuideMessage())
                .type(curationData.getPresentationType())
                .purpose(presentation.getPurpose())
                .style(presentation.getStyle())
                .audience(presentation.getAudience())
                .materialType(curationData.getMaterialType())
                .title(curationData.getTitle())
                .sourceChannel(curationData.getSourceChannel())
                .linkUrl(curationData.getLinkUrl())
                .imageUrl(curationData.getImageUrl())
                .build();
    }
}