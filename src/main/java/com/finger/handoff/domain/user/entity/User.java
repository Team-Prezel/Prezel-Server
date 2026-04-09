package com.finger.handoff.domain.user.entity;

import com.finger.handoff.domain.terms.entity.UserTermsAgreement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String nickname;

    private String profileImgUrl;

    private String refreshToken;

    private Boolean isTermsAgreement;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserTermsAgreement> termsAgreements = new ArrayList<>();

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void deleteRefreshToken() {
        this.refreshToken = null;
    }
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
    public void updateProfile(String nickname, String profileImgUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileImgUrl != null) {
            this.profileImgUrl = profileImgUrl;
        }
    }

    public void updateTermsAgreement(Boolean isTermsAgreement) {
        this.isTermsAgreement = isTermsAgreement;
    }
}
