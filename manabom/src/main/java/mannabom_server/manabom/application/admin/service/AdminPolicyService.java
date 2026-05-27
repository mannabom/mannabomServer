package mannabom_server.manabom.application.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminUpdatePolicyRequest;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPolicyService {

    private final RuntimePolicyService runtimePolicyService;
    private final AdminAuditService adminAuditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public RuntimePolicySnapshot getPolicy(AdminPrincipal admin) {
        requireRole(admin, AdminRole.SUPER_ADMIN);
        return runtimePolicyService.snapshot();
    }

    @Transactional
    public RuntimePolicySnapshot updatePolicy(AdminPrincipal admin,
                                              AdminUpdatePolicyRequest request,
                                              String ipAddress) {
        requireRole(admin, AdminRole.SUPER_ADMIN);
        RuntimePolicySnapshot before = runtimePolicyService.snapshot();

        if (request.isResetToDefault()) {
            resetPolicy(request.getKey());
        } else {
            if (request.getValue() == null) {
                throw new IllegalArgumentException("value는 필수입니다.");
            }
            updatePolicy(request.getKey(), request.getValue());
        }

        RuntimePolicySnapshot after = runtimePolicyService.snapshot();
        adminAuditService.log(admin.adminId(), AdminAuditActionType.POLICY_UPDATE,
                AdminAuditTargetType.POLICY, null, toJson(before), toJson(after), request.getReason(), ipAddress);
        return after;
    }

    private void updatePolicy(String key, int value) {
        switch (key) {
            case "match.cooldownHours" -> runtimePolicyService.updateMatchCooldownHours(value);
            case "match.candidatePoolSize" -> runtimePolicyService.updateMatchCandidatePoolSize(value);
            case "match.pickPoolSize" -> runtimePolicyService.updateMatchPickPoolSize(value);
            case "ting.vipThreshold" -> runtimePolicyService.updateTingVipThreshold(value);
            case "ting.cost.extraProfile" -> runtimePolicyService.updateTingCostExtraProfile(value);
            case "ting.cost.extraProfileBundle5" -> runtimePolicyService.updateTingCostExtraProfileBundle5(value);
            case "ting.cost.message" -> runtimePolicyService.updateTingCostMessage(value);
            case "ting.cost.like" -> runtimePolicyService.updateTingCostLike(value);
            case "ting.cost.viewExtraPhoto" -> runtimePolicyService.updateTingCostViewExtraPhoto(value);
            case "ting.cost.viewScore" -> runtimePolicyService.updateTingCostViewScore(value);
            case "ting.cost.viewLikedMeProfile" -> runtimePolicyService.updateTingCostViewLikedMeProfile(value);
            case "ting.cost.viewHighScoreProfile" -> runtimePolicyService.updateTingCostViewHighScoreProfile(value);
            case "benefit.basic.dailyProfile" -> runtimePolicyService.updateBenefitBasicDailyProfile(value);
            case "benefit.basic.dailyLoveView" -> runtimePolicyService.updateBenefitBasicDailyLoveView(value);
            case "benefit.membership.cycleExtraProfiles" -> runtimePolicyService.updateBenefitMembershipCycleExtraProfiles(value);
            case "benefit.membership.cycleFreeMessages" -> runtimePolicyService.updateBenefitMembershipCycleFreeMessages(value);
            case "benefit.membership.cycleFreeLikes" -> runtimePolicyService.updateBenefitMembershipCycleFreeLikes(value);
            case "benefit.vip.dailyExtraProfiles" -> runtimePolicyService.updateBenefitVipDailyExtraProfiles(value);
            case "benefit.vip.dailyFreeMessages" -> runtimePolicyService.updateBenefitVipDailyFreeMessages(value);
            case "benefit.vip.dailyFreeLikes" -> runtimePolicyService.updateBenefitVipDailyFreeLikes(value);
            default -> throw new IllegalArgumentException("지원하지 않는 정책 key입니다: " + key);
        }
    }

    private void resetPolicy(String key) {
        switch (key) {
            case "match.cooldownHours" -> runtimePolicyService.resetMatchCooldownHoursToDefault();
            case "match.candidatePoolSize" -> runtimePolicyService.resetMatchCandidatePoolSizeToDefault();
            case "match.pickPoolSize" -> runtimePolicyService.resetMatchPickPoolSizeToDefault();
            case "ting.vipThreshold" -> runtimePolicyService.resetTingVipThresholdToDefault();
            case "ting.cost.extraProfile" -> runtimePolicyService.resetTingCostExtraProfileToDefault();
            case "ting.cost.extraProfileBundle5" -> runtimePolicyService.resetTingCostExtraProfileBundle5ToDefault();
            case "ting.cost.message" -> runtimePolicyService.resetTingCostMessageToDefault();
            case "ting.cost.like" -> runtimePolicyService.resetTingCostLikeToDefault();
            case "ting.cost.viewExtraPhoto" -> runtimePolicyService.resetTingCostViewExtraPhotoToDefault();
            case "ting.cost.viewScore" -> runtimePolicyService.resetTingCostViewScoreToDefault();
            case "ting.cost.viewLikedMeProfile" -> runtimePolicyService.resetTingCostViewLikedMeProfileToDefault();
            case "ting.cost.viewHighScoreProfile" -> runtimePolicyService.resetTingCostViewHighScoreProfileToDefault();
            case "benefit.basic.dailyProfile" -> runtimePolicyService.resetBenefitBasicDailyProfileToDefault();
            case "benefit.basic.dailyLoveView" -> runtimePolicyService.resetBenefitBasicDailyLoveViewToDefault();
            case "benefit.membership.cycleExtraProfiles" -> runtimePolicyService.resetBenefitMembershipCycleExtraProfilesToDefault();
            case "benefit.membership.cycleFreeMessages" -> runtimePolicyService.resetBenefitMembershipCycleFreeMessagesToDefault();
            case "benefit.membership.cycleFreeLikes" -> runtimePolicyService.resetBenefitMembershipCycleFreeLikesToDefault();
            case "benefit.vip.dailyExtraProfiles" -> runtimePolicyService.resetBenefitVipDailyExtraProfilesToDefault();
            case "benefit.vip.dailyFreeMessages" -> runtimePolicyService.resetBenefitVipDailyFreeMessagesToDefault();
            case "benefit.vip.dailyFreeLikes" -> runtimePolicyService.resetBenefitVipDailyFreeLikesToDefault();
            default -> throw new IllegalArgumentException("지원하지 않는 정책 key입니다: " + key);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private void requireRole(AdminPrincipal admin, AdminRole role) {
        if (!admin.hasRole(role)) {
            throw new IllegalStateException("해당 관리자 권한으로 수행할 수 없는 작업입니다.");
        }
    }
}
