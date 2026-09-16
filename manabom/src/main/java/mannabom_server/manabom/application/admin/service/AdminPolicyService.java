package mannabom_server.manabom.application.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminUpdatePolicyRequest;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.repository.GifticonProductRepository;
import mannabom_server.manabom.domain.gifticon.service.GifticonPriceCalculator;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminPolicyService {

    private final RuntimePolicyService runtimePolicyService;
    private final AdminAuditService adminAuditService;
    private final ObjectMapper objectMapper;
    private final GifticonProductRepository gifticonProductRepository;
    private final GifticonPriceCalculator gifticonPriceCalculator;

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
        if ("gifticon.pricing.markupPercent".equals(request.getKey())) {
            recalculateAllGifticonPrices(after.getGifticon().getPricing().getMarkupPercent());
        }
        adminAuditService.log(admin.adminId(), AdminAuditActionType.POLICY_UPDATE,
                AdminAuditTargetType.POLICY, null, toJson(before), toJson(after), request.getReason(), ipAddress);
        return after;
    }

    private void updatePolicy(String key, BigDecimal value) {
        switch (key) {
            case "match.cooldownHours" -> runtimePolicyService.updateMatchCooldownHours(toInt(key, value));
            case "match.candidatePoolSize" -> runtimePolicyService.updateMatchCandidatePoolSize(toInt(key, value));
            case "match.pickPoolSize" -> runtimePolicyService.updateMatchPickPoolSize(toInt(key, value));
            case "ting.vipThreshold" -> runtimePolicyService.updateTingVipThreshold(toInt(key, value));
            case "ting.cost.extraProfile" -> runtimePolicyService.updateTingCostExtraProfile(toInt(key, value));
            case "ting.cost.extraProfileBundle5" -> runtimePolicyService.updateTingCostExtraProfileBundle5(toInt(key, value));
            case "ting.cost.message" -> runtimePolicyService.updateTingCostMessage(toInt(key, value));
            case "ting.cost.like" -> runtimePolicyService.updateTingCostLike(toInt(key, value));
            case "ting.cost.viewExtraPhoto" -> runtimePolicyService.updateTingCostViewExtraPhoto(toInt(key, value));
            case "ting.cost.viewScore" -> runtimePolicyService.updateTingCostViewScore(toInt(key, value));
            case "ting.cost.viewLikedMeProfile" -> runtimePolicyService.updateTingCostViewLikedMeProfile(toInt(key, value));
            case "ting.cost.viewHighScoreProfile" -> runtimePolicyService.updateTingCostViewHighScoreProfile(toInt(key, value));
            case "benefit.basic.dailyProfile" -> runtimePolicyService.updateBenefitBasicDailyProfile(toInt(key, value));
            case "benefit.basic.dailyLoveView" -> runtimePolicyService.updateBenefitBasicDailyLoveView(toInt(key, value));
            case "benefit.membership.cycleExtraProfiles" -> runtimePolicyService.updateBenefitMembershipCycleExtraProfiles(toInt(key, value));
            case "benefit.membership.cycleFreeMessages" -> runtimePolicyService.updateBenefitMembershipCycleFreeMessages(toInt(key, value));
            case "benefit.membership.cycleFreeLikes" -> runtimePolicyService.updateBenefitMembershipCycleFreeLikes(toInt(key, value));
            case "benefit.vip.dailyExtraProfiles" -> runtimePolicyService.updateBenefitVipDailyExtraProfiles(toInt(key, value));
            case "benefit.vip.dailyFreeMessages" -> runtimePolicyService.updateBenefitVipDailyFreeMessages(toInt(key, value));
            case "benefit.vip.dailyFreeLikes" -> runtimePolicyService.updateBenefitVipDailyFreeLikes(toInt(key, value));
            case "gifticon.pricing.markupPercent" -> runtimePolicyService.updateGifticonMarkupPercent(value);
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
            case "gifticon.pricing.markupPercent" -> runtimePolicyService.resetGifticonMarkupPercentToDefault();
            default -> throw new IllegalArgumentException("지원하지 않는 정책 key입니다: " + key);
        }
    }

    private int toInt(String key, BigDecimal value) {
        try {
            return value.intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(key + "는 정수여야 합니다.");
        }
    }

    private void recalculateAllGifticonPrices(BigDecimal markupPercent) {
        for (GifticonProduct product : gifticonProductRepository.findAll()) {
            product.updateSalePrice(gifticonPriceCalculator.calculateSalePrice(
                    product.getProductPrice(),
                    markupPercent
            ));
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
