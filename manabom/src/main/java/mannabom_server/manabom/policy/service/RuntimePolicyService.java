package mannabom_server.manabom.policy.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.policy.config.BenefitPolicyProperties;
import mannabom_server.manabom.policy.config.MatchPolicyProperties;
import mannabom_server.manabom.policy.config.TingPolicyProperties;
import mannabom_server.manabom.policy.entity.PolicyConfig;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.repository.PolicyConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class RuntimePolicyService {

    private static final long POLICY_ROW_ID = 1L;

    private final MatchPolicyProperties matchDefaults;
    private final TingPolicyProperties tingDefaults;
    private final BenefitPolicyProperties benefitDefaults;

    private final PolicyConfigRepository policyConfigRepository;

    private final AtomicReference<RuntimePolicySnapshot> cache = new AtomicReference<>();

    // 서버 재시작 시 자동으로 로드(DB 먼저 조회 후 없으면 .yml 값으로 적용)
    @PostConstruct
    public void init() {
        reload();
    }

    // 캐시 처리
    public RuntimePolicySnapshot snapshot() {
        RuntimePolicySnapshot s = cache.get();
        if (s == null) {
            reload();
            return cache.get();
        }
        return s;
    }

    // 기본값 + DB 컬럼 override를 합쳐서 캐시 갱신
    @Transactional(readOnly = true)
    public void reload() {
        RuntimePolicySnapshot base = buildDefaults();

        PolicyConfig row = policyConfigRepository.findById(POLICY_ROW_ID).orElse(null);
        RuntimePolicySnapshot merged = merge(base, row);

        cache.set(merged);
    }

    // application.yml 기본값을 스냅샷으로 변환
    private RuntimePolicySnapshot buildDefaults() {
        return RuntimePolicySnapshot.builder()
                .match(RuntimePolicySnapshot.Match.builder()
                        .cooldownHours(matchDefaults.getCooldownHours())
                        .candidatePoolSize(matchDefaults.getCandidatePoolSize())
                        .pickPoolSize(matchDefaults.getPickPoolSize())
                        .build())
                .ting(RuntimePolicySnapshot.Ting.builder()
                        .vipThreshold(tingDefaults.getVipThreshold())
                        .cost(RuntimePolicySnapshot.Ting.Cost.builder()
                                .extraProfile(tingDefaults.getCost().getExtraProfile())
                                .extraProfileBundle5(tingDefaults.getCost().getExtraProfileBundle5())
                                .message(tingDefaults.getCost().getMessage())
                                .like(tingDefaults.getCost().getLike())
                                .viewExtraPhoto(tingDefaults.getCost().getViewExtraPhoto())
                                .viewScore(tingDefaults.getCost().getViewScore())
                                .viewHighScoreProfile(tingDefaults.getCost().getViewHighScoreProfile())
                                .viewLikedMeProfile(tingDefaults.getCost().getViewLikedMeProfile())
                                .build())
                        .build())
                .benefit(RuntimePolicySnapshot.Benefit.builder()
                        .membership(RuntimePolicySnapshot.Benefit.Membership.builder()
                                .cycleExtraProfiles(benefitDefaults.getMembership().getMonthlyExtraProfiles())
                                .cycleFreeMessages(benefitDefaults.getMembership().getMonthlyFreeMessages())
                                .cycleFreeLikes(benefitDefaults.getMembership().getMonthlyFreeLikes())
                                .build())
                        .vip(RuntimePolicySnapshot.Benefit.Vip.builder()
                                .dailyExtraProfiles(benefitDefaults.getVip().getDailyExtraProfiles())
                                .dailyFreeMessages(benefitDefaults.getVip().getDailyFreeMessages())
                                .dailyFreeLikes(benefitDefaults.getVip().getDailyFreeLikes())
                                .build())
                        .basic(RuntimePolicySnapshot.Benefit.Basic.builder()
                                .dailyProfile(benefitDefaults.getBasic().getDailyProfile())
                                .dailyLoveView(benefitDefaults.getBasic().getDailyLoveView())
                                .build())
                        .build())
                .build();
    }

    // DB row가 있으면 null이 아닌 컬럼만 base 위에 덮어써서 최종값 생성
    private RuntimePolicySnapshot merge(RuntimePolicySnapshot base, PolicyConfig row) {
        if (row == null) return base;

        int cooldown = nvl(row.getMatchCooldownHours(), base.getMatch().getCooldownHours());
        int candidatePool = nvl(row.getMatchCandidatePoolSize(), base.getMatch().getCandidatePoolSize());
        int pickPool = nvl(row.getMatchPickPoolSize(), base.getMatch().getPickPoolSize());

        int vipThreshold = nvl(row.getTingVipThreshold(), base.getTing().getVipThreshold());
        int costExtraProfile = nvl(row.getTingCostExtraProfile(), base.getTing().getCost().getExtraProfile());
        int costExtraProfileBundle5 = nvl(row.getTingCostExtraProfileBundle5(), base.getTing().getCost().getExtraProfileBundle5());
        int costMessage = nvl(row.getTingCostMessage(), base.getTing().getCost().getMessage());
        int costLike = nvl(row.getTingCostLike(), base.getTing().getCost().getLike());
        int costViewExtraPhoto = nvl(row.getTingCostViewExtraPhoto(), base.getTing().getCost().getViewExtraPhoto());
        int costViewScore = nvl(row.getTingCostViewScore(), base.getTing().getCost().getViewScore());
        int costViewHighScoreProfile = nvl(row.getTingCostViewHighScoreProfile(), base.getTing().getCost().getViewHighScoreProfile());
        int costViewLikedMeProfile = nvl(row.getTingCostViewLikedMeProfile(), base.getTing().getCost().getViewLikedMeProfile());

        int membershipCycleExtraProfiles = nvl(row.getBenefitMembershipCycleExtraProfiles(),
                base.getBenefit().getMembership().getCycleExtraProfiles());
        int membershipCycleFreeMessages = nvl(row.getBenefitMembershipCycleFreeMessages(),
                base.getBenefit().getMembership().getCycleFreeMessages());
        int membershipCycleFreeLikes = nvl(row.getBenefitMembershipCycleFreeLikes(),
                base.getBenefit().getMembership().getCycleFreeLikes());
        int vipDailyExtraProfiles = nvl(row.getBenefitVipDailyExtraProfiles(),
                base.getBenefit().getVip().getDailyExtraProfiles());
        int vipDailyFreeMessages = nvl(row.getBenefitVipDailyFreeMessages(),
                base.getBenefit().getVip().getDailyFreeMessages());
        int vipDailyFreeLikes = nvl(row.getBenefitVipDailyFreeLikes(),
                base.getBenefit().getVip().getDailyFreeLikes());
        int basicDailyProfile = nvl(row.getBenefitBasicDailyProfile(),
                base.getBenefit().getBasic().getDailyProfile());
        int basicDailyLoveView = nvl(row.getBenefitBasicDailyLoveView(),
                base.getBenefit().getBasic().getDailyLoveView());

        return RuntimePolicySnapshot.builder()
                .match(RuntimePolicySnapshot.Match.builder()
                        .cooldownHours(cooldown)
                        .candidatePoolSize(candidatePool)
                        .pickPoolSize(pickPool)
                        .build())
                .ting(RuntimePolicySnapshot.Ting.builder()
                        .vipThreshold(vipThreshold)
                        .cost(RuntimePolicySnapshot.Ting.Cost.builder()
                                .extraProfile(costExtraProfile)
                                .extraProfileBundle5(costExtraProfileBundle5)
                                .message(costMessage)
                                .like(costLike)
                                .viewExtraPhoto(costViewExtraPhoto)
                                .viewScore(costViewScore)
                                .viewHighScoreProfile(costViewHighScoreProfile)
                                .viewLikedMeProfile(costViewLikedMeProfile)
                                .build())
                        .build())
                .benefit(RuntimePolicySnapshot.Benefit.builder()
                        .membership(RuntimePolicySnapshot.Benefit.Membership.builder()
                                .cycleExtraProfiles(membershipCycleExtraProfiles)
                                .cycleFreeMessages(membershipCycleFreeMessages)
                                .cycleFreeLikes(membershipCycleFreeLikes)
                                .build())
                        .vip(RuntimePolicySnapshot.Benefit.Vip.builder()
                                .dailyExtraProfiles(vipDailyExtraProfiles)
                                .dailyFreeMessages(vipDailyFreeMessages)
                                .dailyFreeLikes(vipDailyFreeLikes)
                                .build())
                        .basic(RuntimePolicySnapshot.Benefit.Basic.builder()
                                .dailyProfile(basicDailyProfile)
                                .dailyLoveView(basicDailyLoveView)
                                .build())
                        .build())
                .build();
    }

    private int nvl(Integer override, int base) {
        return override != null ? override : base;
    }

    // ------------------------------
    // 관리자 업데이트용 (필드 하나만 수정)
    // ------------------------------


    // 공통: id=1 row가 없으면 만들어서 반환
    @Transactional
    public PolicyConfig ensureRow() {
        return policyConfigRepository.findById(POLICY_ROW_ID)
                .orElseGet(() -> policyConfigRepository.save(new PolicyConfig(POLICY_ROW_ID)));
    }

    // --------- match.policy ---------

    @Transactional
    public void updateMatchCooldownHours(int hours) {
        if (hours < 0) throw new IllegalArgumentException("match.cooldownHours는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateMatchCooldownHours(hours);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetMatchCooldownHoursToDefault() {
        PolicyConfig row = ensureRow();
        row.resetMatchCooldownHours();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateMatchCandidatePoolSize(int size) {
        if (size < 0) throw new IllegalArgumentException("match.candidatePoolSize는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateMatchCandidatePoolSize(size);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetMatchCandidatePoolSizeToDefault() {
        PolicyConfig row = ensureRow();
        row.resetMatchCandidatePoolSize();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateMatchPickPoolSize(int size) {
        if (size < 0) throw new IllegalArgumentException("match.pickPoolSize는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateMatchPickPoolSize(size);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetMatchPickPoolSizeToDefault() {
        PolicyConfig row = ensureRow();
        row.resetMatchPickPoolSize();
        policyConfigRepository.save(row);
        reload();
    }

    // --------- ting.policy ---------

    @Transactional
    public void updateTingVipThreshold(int threshold) {
        if (threshold < 0) throw new IllegalArgumentException("ting.vipThreshold는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateTingVipThreshold(threshold);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetTingVipThresholdToDefault() {
        PolicyConfig row = ensureRow();
        row.resetTingVipThreshold();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateTingCostExtraProfile(int cost) {
        if (cost < 0) throw new IllegalArgumentException("ting.cost.extraProfile는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateTingCostExtraProfile(cost);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetTingCostExtraProfileToDefault() {
        PolicyConfig row = ensureRow();
        row.resetTingCostExtraProfile();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateTingCostExtraProfileBundle5(int cost) {
        if (cost < 0) throw new IllegalArgumentException("ting.cost.extraProfileBundle5는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateTingCostExtraProfileBundle5(cost);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetTingCostExtraProfileBundle5ToDefault() {
        PolicyConfig row = ensureRow();
        row.resetTingCostExtraProfileBundle5();
        policyConfigRepository.save(row);
        reload();
    }


    @Transactional
    public void updateTingCostMessage(int cost) {
        if (cost < 0) throw new IllegalArgumentException("ting.cost.message는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateTingCostMessage(cost);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetTingCostMessageToDefault() {
        PolicyConfig row = ensureRow();
        row.resetTingCostMessage();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateTingCostLike(int cost) {
        if (cost < 0) throw new IllegalArgumentException("ting.cost.like는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateTingCostLike(cost);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetTingCostLikeToDefault() {
        PolicyConfig row = ensureRow();
        row.resetTingCostLike();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateTingCostViewExtraPhoto(int cost) {
        if (cost < 0) throw new IllegalArgumentException("ting.cost.viewExtraPhoto는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateTingCostViewExtraPhoto(cost);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetTingCostViewExtraPhotoToDefault() {
        PolicyConfig row = ensureRow();
        row.resetTingCostViewExtraPhoto();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateTingCostViewLikedMeProfile(int cost) {
        if (cost < 0) throw new IllegalArgumentException("ting.cost.viewLikedMeProfile는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateTingCostViewLikedMeProfile(cost);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetTingCostViewLikedMeProfileToDefault() {
        PolicyConfig row = ensureRow();
        row.resetTingCostViewLikedMeProfile();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateTingCostViewHighScoreProfile(int cost) {
        if (cost < 0) throw new IllegalArgumentException("ting.cost.viewHighScoreProfile는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateTingCostViewHighScoreProfile(cost);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetTingCostViewHighScoreProfileToDefault() {
        PolicyConfig row = ensureRow();
        row.resetTingCostViewHighScoreProfile();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateTingCostViewScore(int cost) {
        if (cost < 0) throw new IllegalArgumentException("ting.cost.viewScore는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateTingCostViewScore(cost);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetTingCostViewScoreToDefault() {
        PolicyConfig row = ensureRow();
        row.resetTingCostViewScore();
        policyConfigRepository.save(row);
        reload();
    }


    // --------- benefit.policy ---------

    @Transactional
    public void updateBenefitBasicDailyProfile(int count) {
        if (count < 0) throw new IllegalArgumentException("benefit.basic.dailyProfile은 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateBenefitBasicDailyProfile(count);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetBenefitBasicDailyProfileToDefault() {
        PolicyConfig row = ensureRow();
        row.resetBenefitBasicDailyProfile();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateBenefitBasicDailyLoveView(int count) {
        if (count < 0) throw new IllegalArgumentException("benefit.basic.dailyLoveView은 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateBenefitBasicDailyLoveView(count);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetBenefitBasicDailyLoveViewToDefault() {
        PolicyConfig row = ensureRow();
        row.resetBenefitBasicDailyLoveView();
        policyConfigRepository.save(row);
        reload();
    }


    @Transactional
    public void updateBenefitMembershipCycleExtraProfiles(int count) {
        if (count < 0) throw new IllegalArgumentException("benefit.membership.cycleExtraProfiles는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateBenefitMembershipCycleExtraProfiles(count);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetBenefitMembershipCycleExtraProfilesToDefault() {
        PolicyConfig row = ensureRow();
        row.resetBenefitMembershipCycleExtraProfiles();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateBenefitMembershipCycleFreeMessages(int count) {
        if (count < 0) throw new IllegalArgumentException("benefit.membership.cycleFreeMessages는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateBenefitMembershipCycleFreeMessages(count);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetBenefitMembershipCycleFreeMessagesToDefault() {
        PolicyConfig row = ensureRow();
        row.resetBenefitMembershipCycleFreeMessages();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateBenefitMembershipCycleFreeLikes(int count) {
        if (count < 0) throw new IllegalArgumentException("benefit.membership.cycleFreeLikes는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateBenefitMembershipCycleFreeLikes(count);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetBenefitMembershipCycleFreeLikesToDefault() {
        PolicyConfig row = ensureRow();
        row.resetBenefitMembershipCycleFreeLikes();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateBenefitVipDailyExtraProfiles(int count) {
        if (count < 0) throw new IllegalArgumentException("benefit.vip.dailyExtraProfiles는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateBenefitVipDailyExtraProfiles(count);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetBenefitVipDailyExtraProfilesToDefault() {
        PolicyConfig row = ensureRow();
        row.resetBenefitVipDailyExtraProfiles();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateBenefitVipDailyFreeMessages(int count) {
        if (count < 0) throw new IllegalArgumentException("benefit.vip.dailyFreeMessages는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateBenefitVipDailyFreeMessages(count);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetBenefitVipDailyFreeMessagesToDefault() {
        PolicyConfig row = ensureRow();
        row.resetBenefitVipDailyFreeMessages();
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void updateBenefitVipDailyFreeLikes(int count) {
        if (count < 0) throw new IllegalArgumentException("benefit.vip.dailyFreeLikes는 0 이상이어야 합니다.");
        PolicyConfig row = ensureRow();
        row.updateBenefitVipDailyFreeLikes(count);
        policyConfigRepository.save(row);
        reload();
    }

    @Transactional
    public void resetBenefitVipDailyFreeLikesToDefault() {
        PolicyConfig row = ensureRow();
        row.resetBenefitVipDailyFreeLikes();
        policyConfigRepository.save(row);
        reload();
    }
}
