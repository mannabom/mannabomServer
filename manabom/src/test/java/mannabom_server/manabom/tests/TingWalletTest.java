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
    @DisplayName("일일 프로필: check로 오늘치 지급 후 consume 가능, check 없이 consume 시 예외")
    void daily_profile_flow() {
        TingWallet w = new TingWallet(1L);
        LocalDate day1 = LocalDate.of(2026, 1, 18);

        assertThrows(IllegalStateException.class, () -> w.consumeDailyProfile(day1));

        int remain = w.checkDailyProfile(day1, 3);
        assertEquals(3, remain);
        assertEquals(3, w.getDailyProfileRemaining());
        assertEquals(day1, w.getDailyProfileGrantedDate());

        w.consumeDailyProfile(day1);
        assertEquals(2, w.getDailyProfileRemaining());

        w.checkDailyProfile(day1, 999);
        assertEquals(2, w.getDailyProfileRemaining());

        LocalDate day2 = day1.plusDays(1);
        w.checkDailyProfile(day2, 4);
        assertEquals(4, w.getDailyProfileRemaining());
        assertEquals(day2, w.getDailyProfileGrantedDate());
    }

    @Test
    @DisplayName("일일 연애관: check로 오늘치 지급 후 consume 가능, 0이면 예외")
    void daily_love_view_flow() {
        TingWallet w = new TingWallet(1L);
        LocalDate day1 = LocalDate.of(2026, 1, 18);

        int remaining = w.checkDailyLoveView(day1, 1);
        assertEquals(1, remaining);

        w.consumeDailyLoveView(day1);
        assertEquals(0, w.getDailyLoveViewRemaining());

        assertThrows(IllegalStateException.class, () -> w.consumeDailyLoveView(day1));
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
        LocalDateTime end = LocalDateTime.of(2026, 2, 1, 0, 0);

        assertThrows(IllegalArgumentException.class,
                () -> w.activateMembership(null, end, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> w.activateMembership(start, start, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> w.activateMembership(start, end, -1, 1, 1));

        w.activateMembership(start, end, 1, 1, 1);

        assertTrue(w.isMembershipActive(LocalDateTime.of(2026, 1, 15, 0, 0)));
        assertFalse(w.isMembershipActive(LocalDateTime.of(2026, 2, 2, 0, 0)));

        LocalDateTime now = LocalDateTime.of(2026, 1, 10, 0, 0);

        w.consumeMembershipExtraProfile(now);
        assertEquals(0, w.getMembershipMonthlyExtraProfileRemaining());
        assertThrows(IllegalStateException.class, () -> w.consumeMembershipExtraProfile(now));

        w.consumeMembershipFreeLike(now);
        assertEquals(0, w.getMembershipMonthlyFreeLikesRemaining());
        assertThrows(IllegalStateException.class, () -> w.consumeMembershipFreeLike(now));

        w.consumeMembershipFreeMessage(now);
        assertEquals(0, w.getMembershipMonthlyFreeMessagesRemaining());
        assertThrows(IllegalStateException.class, () -> w.consumeMembershipFreeMessage(now));

        assertThrows(IllegalStateException.class,
                () -> w.consumeMembershipFreeMessage(LocalDateTime.of(2026, 2, 2, 0, 0)));
    }

    @Test
    @DisplayName("VIP: threshold 이상이면 오늘치 지급, 같은 날은 ting 감소해도 VIP 유지, 다음날은 다시 판정")
    void vip_flow() {
        TingWallet w = new TingWallet(1L);
        int threshold = 10;

        LocalDate day1 = LocalDate.of(2026, 1, 18);
        LocalDate day2 = day1.plusDays(1);

        assertFalse(w.isVip(threshold, 3, 2, 1, day1));

        w.addTing(10);
        assertTrue(w.isVip(threshold, 3, 2, 1, day1));
        assertEquals(day1, w.getVipGrantedDate());
        assertEquals(3, w.getVipDailyExtraProfileRemaining());
        assertEquals(2, w.getVipDailyFreeMessagesRemaining());
        assertEquals(1, w.getVipDailyFreeLikesRemaining());

        w.spendTing(10);
        assertEquals(0, w.getTing());
        assertTrue(w.isVip(threshold, 999, 999, 999, day1));
        assertEquals(3, w.getVipDailyExtraProfileRemaining());

        assertFalse(w.isVip(threshold, 3, 2, 1, day2));
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
        assertEquals(0, w.getVipDailyExtraProfileRemaining());
        assertThrows(IllegalStateException.class, () -> w.consumeVipExtraProfile(day1));
    }
}

