package mannabom_server.manabom.tests;

import mannabom_server.manabom.domain.currency.entity.TingWallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TingWalletTest {

    @Test
    @DisplayName("addTing/spendTing: 정상 케이스 및 예외(0 이하, 잔액 부족)")
    void ting_add_and_spend() {
        TingWallet w = new TingWallet(1L);

        assertThrows(IllegalArgumentException.class, () -> w.addTing(0));
        assertThrows(IllegalArgumentException.class, () -> w.spendTing(-1));

        w.addTing(10);
        assertEquals(10, w.getTing());

        assertThrows(IllegalStateException.class, () -> w.spendTing(11));

        w.spendTing(3);
        assertEquals(7, w.getTing());
    }

    @Test
    @DisplayName("addEventTing/spendEventTing: 정상 케이스 및 예외")
    void event_ting_add_and_spend() {
        TingWallet w = new TingWallet(1L);

        assertThrows(IllegalArgumentException.class, () -> w.addEventTing(0));

        w.addEventTing(5);
        assertEquals(5, w.getEventTing());

        assertThrows(IllegalStateException.class, () -> w.spendEventTing(6));

        w.spendEventTing(2);
        assertEquals(3, w.getEventTing());
    }

    @Test
    @DisplayName("일일 프로필: check로 오늘치 지급 후 consume 가능, check 없이 consume 시 예외, 다음날 재지급")
    void daily_profile_flow() {
        TingWallet w = new TingWallet(1L);
        LocalDate day1 = LocalDate.of(2026, 1, 18);
        LocalDate day2 = day1.plusDays(1);

        // check 없이 consume -> 예외
        assertThrows(IllegalStateException.class, () -> w.consumeDailyProfile(day1));

        // day1 지급
        assertEquals(3, w.checkDailyProfile(day1, 3));

        // 소비
        w.consumeDailyProfile(day1);
        assertEquals(2, w.checkDailyProfile(day1, 999)); // 같은 날 다시 check해도 값 유지

        w.consumeDailyProfile(day1);
        assertEquals(1, w.checkDailyProfile(day1, 999));

        w.consumeDailyProfile(day1);
        assertEquals(0, w.checkDailyProfile(day1, 999));

        // 더 소비 -> 예외
        assertThrows(IllegalStateException.class, () -> w.consumeDailyProfile(day1));

        // day2 날짜 바뀌면 재지급
        assertEquals(4, w.checkDailyProfile(day2, 4));
    }

    @Test
    @DisplayName("일일 연애관: check로 오늘치 지급 후 consume 가능, 0이면 예외, 다음날 재지급")
    void daily_love_view_flow() {
        TingWallet w = new TingWallet(1L);
        LocalDate day1 = LocalDate.of(2026, 1, 18);
        LocalDate day2 = day1.plusDays(1);

        // day1 지급
        assertEquals(1, w.checkDailyLoveView(day1, 1));

        // 소비
        w.consumeDailyLoveView(day1);
        assertEquals(0, w.checkDailyLoveView(day1, 999));

        // 더 소비 -> 예외
        assertThrows(IllegalStateException.class, () -> w.consumeDailyLoveView(day1));

        // day2 재지급
        assertEquals(2, w.checkDailyLoveView(day2, 2));
    }

    @Test
    @DisplayName("팅으로 산 추가 프로필: add/consume 및 예외")
    void extra_profile_by_ting() {
        TingWallet w = new TingWallet(1L);

        assertThrows(IllegalArgumentException.class, () -> w.addExtraProfileByTing(0));
        assertThrows(IllegalStateException.class, w::consumeExtraProfileByTing);

        w.addExtraProfileByTing(2);
        assertEquals(2, w.checkExtraProfileByTing());

        w.consumeExtraProfileByTing();
        assertEquals(1, w.checkExtraProfileByTing());

        w.consumeExtraProfileByTing();
        assertEquals(0, w.checkExtraProfileByTing());

        assertThrows(IllegalStateException.class, w::consumeExtraProfileByTing);
    }

    @Test
    @DisplayName("멤버십: 활성화/만료 판정 및 혜택 소모 예외")
    void membership_flow() {
        TingWallet w = new TingWallet(1L);

        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime now = LocalDateTime.of(2026, 1, 10, 0, 0);
        LocalDateTime afterExpire = LocalDateTime.of(2026, 2, 2, 0, 0);

        assertThrows(IllegalArgumentException.class,
                () -> w.activateMembership(null, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> w.activateMembership(start, -1, 1, 1));

        // 정상 활성화
        w.activateMembership(start, 1, 1, 1);

        assertTrue(w.isMembershipActive(LocalDateTime.of(2026, 1, 15, 0, 0)));
        assertFalse(w.isMembershipActive(afterExpire));

        // 혜택 소모 + 남은 횟수 check (check가 ensureMembershipActive를 타는 버전이라면 now는 활성 기간이어야 함)
        w.consumeMembershipExtraProfile(now);
        assertEquals(0, w.checkMembershipExtraProfilesRemaining(now));
        assertThrows(IllegalStateException.class, () -> w.consumeMembershipExtraProfile(now));

        w.consumeMembershipFreeLike(now);
        assertEquals(0, w.checkMembershipFreeLikesRemaining(now));
        assertThrows(IllegalStateException.class, () -> w.consumeMembershipFreeLike(now));

        w.consumeMembershipFreeMessage(now);
        assertEquals(0, w.checkMembershipFreeMessagesRemaining(now));
        assertThrows(IllegalStateException.class, () -> w.consumeMembershipFreeMessage(now));

        // 만료 후 사용 -> 예외
        assertThrows(IllegalStateException.class,
                () -> w.consumeMembershipFreeMessage(afterExpire));
    }

    @Test
    @DisplayName("VIP: isVip를 먼저 호출했다고 가정. 오늘 확인 안 했으면 check/consume 예외. 확인 후에는 check/consume 가능")
    void vip_flow_requires_check_then_check_remaining_and_consume() {
        TingWallet w = new TingWallet(1L);

        int threshold = 10;
        LocalDate day1 = LocalDate.of(2026, 1, 18);
        LocalDate day2 = day1.plusDays(1);

        // isVip 안 했는데 check하면 예외(오늘 확인 절차 강제)
        assertThrows(IllegalStateException.class, () -> w.checkVipExtraProfilesRemaining(day1));
        assertThrows(IllegalStateException.class, () -> w.checkVipFreeLikesRemaining(day1));
        assertThrows(IllegalStateException.class, () -> w.checkVipFreeMessagesRemaining(day1));

        // day1: threshold 미만이면 VIP 아님
        assertFalse(w.isVip(threshold, 3, 2, 1, day1));
        assertThrows(IllegalStateException.class, () -> w.checkVipExtraProfilesRemaining(day1));

        // day1: VIP 만들기
        w.addTing(10);
        assertTrue(w.isVip(threshold, 3, 2, 1, day1));

        // 잔여량 check -> consume -> check
        assertEquals(3, w.checkVipExtraProfilesRemaining(day1));
        w.consumeVipExtraProfile(day1);
        assertEquals(2, w.checkVipExtraProfilesRemaining(day1));
        w.consumeVipExtraProfile(day1);
        assertEquals(1, w.checkVipExtraProfilesRemaining(day1));
        w.consumeVipExtraProfile(day1);
        assertEquals(0, w.checkVipExtraProfilesRemaining(day1));
        assertThrows(IllegalStateException.class, () -> w.consumeVipExtraProfile(day1));

        // 같은 날 ting 감소해도, "오늘 이미 확인"이 됐으면 VIP 유지(정책 그대로라면)
        w.spendTing(10);
        assertEquals(0, w.getTing());
        assertTrue(w.isVip(threshold, 999, 999, 999, day1)); // 같은 날이라 true 유지
        // 이미 소진했으니 0 유지(999로 리셋되면 안 됨)
        assertEquals(0, w.checkVipExtraProfilesRemaining(day1));

        // 다음날(day2): ting이 threshold 미만이면 VIP 아님
        assertFalse(w.isVip(threshold, 3, 2, 1, day2));
        // day2는 확인이 false니까 check하면 예외(오늘 확인 절차 강제)
        assertThrows(IllegalStateException.class, () -> w.checkVipExtraProfilesRemaining(day2));
    }

    @Test
    @DisplayName("VIP consume: 오늘 isVip 호출로 vipGrantedDate가 세팅되지 않으면 예외")
    void vip_consume_requires_check() {
        TingWallet w = new TingWallet(1L);
        LocalDate day1 = LocalDate.of(2026, 1, 18);

        assertThrows(IllegalStateException.class, () -> w.consumeVipExtraProfile(day1));
        assertThrows(IllegalStateException.class, () -> w.consumeVipFreeLike(day1));
        assertThrows(IllegalStateException.class, () -> w.consumeVipFreeMessage(day1));

        w.addTing(100);
        assertTrue(w.isVip(50, 1, 1, 1, day1));

        w.consumeVipExtraProfile(day1);
        assertEquals(0, w.checkVipExtraProfilesRemaining(day1));
        assertThrows(IllegalStateException.class, () -> w.consumeVipExtraProfile(day1));
    }
}
