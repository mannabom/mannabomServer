package mannabom_server.manabom.policy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_config")
@Getter
@NoArgsConstructor
public class PolicyConfig {

    @Id
    private Long id; // 정책 id는 1로 고정(id가 없으면 JPA 기능 사용 불가)

    // match.policy.*
    @Column(name = "match_cooldown_hours")
    private Integer matchCooldownHours;

    @Column(name = "match_candidate_pool_size")
    private Integer matchCandidatePoolSize;

    @Column(name = "match_pick_pool_size")
    private Integer matchPickPoolSize;

    // ting.policy.*
    @Column(name = "ting_vip_threshold")
    private Integer tingVipThreshold;

    @Column(name = "ting_cost_extra_profile")
    private Integer tingCostExtraProfile;

    @Column(name = "ting_cost_extra_profile_bundle5")
    private Integer tingCostExtraProfileBundle5;

    @Column(name = "ting_cost_message")
    private Integer tingCostMessage;

    @Column(name = "ting_cost_like")
    private Integer tingCostLike;

    @Column(name = "ting_cost_view_extra_photo")
    private Integer tingCostViewExtraPhoto;

    @Column(name = "ting_cost_view_score")
    private Integer tingCostViewScore;

    @Column(name="ting_cost_view_liked_me_profile")
    private Integer tingCostViewLikedMeProfile;

    @Column(name="ting_cost_view_high_score_profile")
    private Integer tingCostViewHighScoreProfile;

    // benefit.policy.*
    @Column(name = "benefit_basic_daily_profile")
    private Integer benefitBasicDailyProfile;
    @Column(name = "benefit_basic_daily_love_view")
    private Integer benefitBasicDailyLoveView;

    @Column(name = "benefit_membership_cycle_extra_profiles")
    private Integer benefitMembershipCycleExtraProfiles;
    @Column(name = "benefit_membership_cycle_free_messages")
    private Integer benefitMembershipCycleFreeMessages;
    @Column(name = "benefit_membership_cycle_free_likes")
    private Integer benefitMembershipCycleFreeLikes;

    @Column(name = "benefit_vip_daily_extra_profiles")
    private Integer benefitVipDailyExtraProfiles;
    @Column(name = "benefit_vip_daily_free_messages")
    private Integer benefitVipDailyFreeMessages;
    @Column(name = "benefit_vip_daily_free_likes")
    private Integer benefitVipDailyFreeLikes;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public PolicyConfig(Long id) {
        this.id = id;
        this.updatedAt = LocalDateTime.now();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    // 관리자 기능 전용

    public void updateMatchCooldownHours(Integer v) { this.matchCooldownHours = v; touch(); }
    public void updateMatchCandidatePoolSize(Integer v) { this.matchCandidatePoolSize = v; touch(); }
    public void updateMatchPickPoolSize(Integer v) { this.matchPickPoolSize = v; touch(); }

    public void updateBenefitBasicDailyProfile(Integer v) { this.benefitBasicDailyProfile = v; touch(); }
    public void updateBenefitBasicDailyLoveView(Integer v) { this.benefitBasicDailyLoveView = v; touch(); }

    public void updateTingVipThreshold(Integer v) { this.tingVipThreshold = v; touch(); }
    public void updateTingCostExtraProfile(Integer v) { this.tingCostExtraProfile = v; touch(); }
    public void updateTingCostExtraProfileBundle5(Integer v) { this.tingCostExtraProfileBundle5 = v; touch(); }
    public void updateTingCostMessage(Integer v) { this.tingCostMessage = v; touch(); }
    public void updateTingCostLike(Integer v) { this.tingCostLike = v; touch(); }
    public void updateTingCostViewExtraPhoto(Integer v) { this.tingCostViewExtraPhoto = v; touch(); }
    public void updateTingCostViewScore(Integer v) { this.tingCostViewScore = v; touch(); }
    public void updateTingCostViewLikedMeProfile(Integer v) { this.tingCostViewLikedMeProfile = v; touch(); }
    public void updateTingCostViewHighScoreProfile(Integer v) { this.tingCostViewHighScoreProfile = v; touch(); }

    public void updateBenefitMembershipCycleExtraProfiles(Integer v) { this.benefitMembershipCycleExtraProfiles = v; touch(); }
    public void updateBenefitMembershipCycleFreeMessages(Integer v) { this.benefitMembershipCycleFreeMessages = v; touch(); }
    public void updateBenefitMembershipCycleFreeLikes(Integer v) { this.benefitMembershipCycleFreeLikes = v; touch(); }
    public void updateBenefitVipDailyExtraProfiles(Integer v) { this.benefitVipDailyExtraProfiles = v; touch(); }
    public void updateBenefitVipDailyFreeMessages(Integer v) { this.benefitVipDailyFreeMessages = v; touch(); }
    public void updateBenefitVipDailyFreeLikes(Integer v) { this.benefitVipDailyFreeLikes = v; touch(); }

    // 기본값으로 되돌리기(override 제거) => 해당 컬럼을 NULL로 만들기
    public void resetMatchCooldownHours() { this.matchCooldownHours = null; touch(); }
    public void resetMatchCandidatePoolSize() { this.matchCandidatePoolSize = null; touch(); }
    public void resetMatchPickPoolSize() { this.matchPickPoolSize = null; touch(); }

    public void resetBenefitBasicDailyProfile() { this.benefitBasicDailyProfile = null; touch(); }
    public void resetBenefitBasicDailyLoveView() { this.benefitBasicDailyLoveView = null; touch(); }

    public void resetTingVipThreshold() { this.tingVipThreshold = null; touch(); }
    public void resetTingCostExtraProfile() { this.tingCostExtraProfile = null; touch(); }
    public void resetTingCostExtraProfileBundle5() { this.tingCostExtraProfileBundle5 = null; touch(); }
    public void resetTingCostMessage() { this.tingCostMessage = null; touch(); }
    public void resetTingCostLike() { this.tingCostLike = null; touch(); }
    public void resetTingCostViewExtraPhoto() { this.tingCostViewExtraPhoto = null; touch(); }
    public void resetTingCostViewScore() { this.tingCostViewScore = null; touch(); }
    public void resetTingCostViewLikedMeProfile() { this.tingCostViewLikedMeProfile = null; touch(); }
    public void resetTingCostViewHighScoreProfile() { this.tingCostViewHighScoreProfile = null; touch(); }

    public void resetBenefitMembershipCycleExtraProfiles() { this.benefitMembershipCycleExtraProfiles = null; touch(); }
    public void resetBenefitMembershipCycleFreeMessages() { this.benefitMembershipCycleFreeMessages = null; touch(); }
    public void resetBenefitMembershipCycleFreeLikes() { this.benefitMembershipCycleFreeLikes = null; touch(); }
    public void resetBenefitVipDailyExtraProfiles() { this.benefitVipDailyExtraProfiles = null; touch(); }
    public void resetBenefitVipDailyFreeMessages() { this.benefitVipDailyFreeMessages = null; touch(); }
    public void resetBenefitVipDailyFreeLikes() { this.benefitVipDailyFreeLikes = null; touch(); }
}
