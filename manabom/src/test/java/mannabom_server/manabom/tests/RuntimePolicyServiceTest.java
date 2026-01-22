package mannabom_server.manabom.tests;

import mannabom_server.manabom.policy.config.BenefitPolicyProperties;
import mannabom_server.manabom.policy.config.MatchPolicyProperties;
import mannabom_server.manabom.policy.config.TingPolicyProperties;
import mannabom_server.manabom.policy.entity.PolicyConfig;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.repository.PolicyConfigRepository;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RuntimePolicyServiceTest {

    private RuntimePolicyService createService(PolicyConfigRepository repo) {
        MatchPolicyProperties match = new MatchPolicyProperties();
        match.setCooldownHours(72);
        match.setCandidatePoolSize(200);
        match.setPickPoolSize(50);

        TingPolicyProperties ting = new TingPolicyProperties();
        ting.setVipThreshold(200);
        TingPolicyProperties.Cost cost = new TingPolicyProperties.Cost();
        cost.setExtraProfile(5);
        cost.setExtraProfileBundle5(20);
        cost.setMessage(0);
        cost.setLike(0);
        cost.setViewExtraPhoto(3);
        cost.setViewScore(3);
        cost.setViewLikedMeProfile(5);
        cost.setViewHighScoreProfile(5);
        ting.setCost(cost);

        BenefitPolicyProperties benefit = new BenefitPolicyProperties();
        BenefitPolicyProperties.Membership membership = new BenefitPolicyProperties.Membership();
        membership.setMonthlyExtraProfiles(30);
        membership.setMonthlyFreeMessages(10);
        membership.setMonthlyFreeLikes(0);

        BenefitPolicyProperties.Vip vip = new BenefitPolicyProperties.Vip();
        vip.setDailyExtraProfiles(3);
        vip.setDailyFreeMessages(0);
        vip.setDailyFreeLikes(0);

        benefit.setMembership(membership);
        benefit.setVip(vip);

        return new RuntimePolicyService(match, ting, benefit, repo);
    }

    @Test
    @DisplayName("DB row가 없으면(application 기본값) snapshot이 기본값으로 생성된다")
    void snapshot_defaults_when_db_row_missing() {
        PolicyConfigRepository repo = mock(PolicyConfigRepository.class);
        when(repo.findById(1L)).thenReturn(Optional.empty());

        RuntimePolicyService service = createService(repo);
        service.reload();

        RuntimePolicySnapshot s = service.snapshot();

        assertEquals(72, s.getMatch().getCooldownHours());
        assertEquals(200, s.getMatch().getCandidatePoolSize());
        assertEquals(50, s.getMatch().getPickPoolSize());

        assertEquals(200, s.getTing().getVipThreshold());
        assertEquals(5, s.getTing().getCost().getExtraProfile());
        assertEquals(20, s.getTing().getCost().getExtraProfileBundle5());
        assertEquals(3, s.getTing().getCost().getViewExtraPhoto());

        assertEquals(30, s.getBenefit().getMembership().getCycleExtraProfiles());
        assertEquals(10, s.getBenefit().getMembership().getCycleFreeMessages());
        assertEquals(0, s.getBenefit().getVip().getDailyFreeLikes());
    }

    @Test
    @DisplayName("DB row가 있으면 null이 아닌 컬럼만 기본값 위에 override 된다")
    void reload_merges_overrides() {
        PolicyConfig row = new PolicyConfig(1L);
        row.updateMatchCooldownHours(10);
        row.updateTingVipThreshold(999);
        row.updateTingCostLike(7);
        row.updateBenefitVipDailyExtraProfiles(11);

        PolicyConfigRepository repo = mock(PolicyConfigRepository.class);
        when(repo.findById(1L)).thenReturn(Optional.of(row));

        RuntimePolicyService service = createService(repo);
        service.reload();

        RuntimePolicySnapshot s = service.snapshot();

        assertEquals(10, s.getMatch().getCooldownHours());
        assertEquals(200, s.getMatch().getCandidatePoolSize());

        assertEquals(999, s.getTing().getVipThreshold());
        assertEquals(7, s.getTing().getCost().getLike());
        assertEquals(5, s.getTing().getCost().getExtraProfile());

        assertEquals(11, s.getBenefit().getVip().getDailyExtraProfiles());
        assertEquals(10, s.getBenefit().getMembership().getCycleFreeMessages());
    }

    @Test
    @DisplayName("ensureRow: id=1 row가 없으면 새로 생성해서 save하고 반환한다")
    void ensureRow_creates_when_missing() {
        AtomicReference<PolicyConfig> store = new AtomicReference<>(null);

        PolicyConfigRepository repo = mock(PolicyConfigRepository.class);
        when(repo.findById(1L)).thenAnswer(inv -> Optional.ofNullable(store.get()));
        when(repo.save(any(PolicyConfig.class))).thenAnswer(inv -> {
            PolicyConfig saved = inv.getArgument(0);
            store.set(saved);
            return saved;
        });

        RuntimePolicyService service = createService(repo);

        PolicyConfig row = service.ensureRow();
        assertNotNull(row);
        assertEquals(1L, row.getId());

        verify(repo, times(1)).save(any(PolicyConfig.class));
    }

    @Test
    @DisplayName("updateMatchCooldownHours: 음수면 예외")
    void update_match_cooldown_negative_throws() {
        PolicyConfigRepository repo = mock(PolicyConfigRepository.class);
        RuntimePolicyService service = createService(repo);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateMatchCooldownHours(-1)
        );
        assertTrue(ex.getMessage().contains("0 이상"));
    }

    @Test
    @DisplayName("update/reset이 저장 + reload까지 수행해서 snapshot 값이 바뀐다")
    void update_and_reset_affects_snapshot() {
        AtomicReference<PolicyConfig> store = new AtomicReference<>(null);

        PolicyConfigRepository repo = mock(PolicyConfigRepository.class);
        when(repo.findById(1L)).thenAnswer(inv -> Optional.ofNullable(store.get()));
        when(repo.save(any(PolicyConfig.class))).thenAnswer(inv -> {
            PolicyConfig saved = inv.getArgument(0);
            store.set(saved);
            return saved;
        });

        RuntimePolicyService service = createService(repo);

        service.updateMatchCooldownHours(9);
        assertEquals(9, service.snapshot().getMatch().getCooldownHours());

        service.resetMatchCooldownHoursToDefault();
        assertEquals(72, service.snapshot().getMatch().getCooldownHours());
    }
}
