package com.finger.handoff.domain.terms.dto;

import com.finger.handoff.domain.terms.entity.Terms;
import lombok.Getter;

@Getter
public class TermsResponse {
    private Long termsId;
    private String title;
    private String summary;
    private String content;
    private Boolean isRequired;
    private String version;

    public TermsResponse(Terms terms) {
        this.termsId = terms.getId();
        this.title = terms.getTitle();
        this.summary = terms.getSummary();
        this.content = terms.getContent();
        this.isRequired = terms.getIsRequired();
        this.version = terms.getVersion();
    }
}
