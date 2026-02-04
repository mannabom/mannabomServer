package mannabom_server.manabom.domain.currency.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Slf4j
@Entity
@Table(name = "ting_wallet")
@Getter
@NoArgsConstructor
public class TingWallet {
    // 소유주의 userId
    @Id
    @Column(name = "user_id")
    private Long userId;
    // 일반 팅 보유 갯수
    @Column(name = "ting", nullable = false)
    private int ting;
    // 이벤트 팅 보유 갯수
    @Column(name = "event_ting", nullable = false)
    private int eventTing;

    /*---기본 지급(일반 유저)---*/
    // 오늘(해당 날짜)에 사용할 수 있는 기본 제공 프로필(소개팅) 남은 횟수
    @Getter(AccessLevel.NONE)
    @Column(name = "daily_profile_remaining", nullable = false)
    private int dailyProfileRemaining;
    // 오늘(해당 날짜)에 사용할 수 있는 기본 제공 연애관(소개팅) 남은 횟수
    @Getter(AccessLevel.NONE)
    @Column(name = "daily_love_view_remaining", nullable = false)
    private int dailyLoveViewRemaining;
    // 기본 제공 프로필 횟수를 마지막으로 "지급/초기화"한 날짜
    @Column(name = "daily_profile_granted_date")
    private LocalDate dailyProfileGrantedDate;
    // 기본 제공 연애관 보기 횟수를 마지막으로 "지급/초기화"한 날짜
    @Column(name = "daily_love_view_granted_date")
    private LocalDate dailyLoveViewGrantedDate;
    /*----------------*/

    //팅으로 결제한 추가 소개팅 갯수(프로필, 연애관 통합)
    @Getter(AccessLevel.NONE)
    @Column(name = "extra_profiles_by_ting_remaining", nullable = false)
    private int extraProfilesByTingRemaining;

    /*---맵버쉽 관련---*/
    // 멤버십: 이번 주기에서 추가 프로필(소개팅) 남은 횟수
    @Getter(AccessLevel.NONE)
    @Column(name = "m_monthly_extra_profiles_remaining", nullable = false)
    private int membershipMonthlyExtraProfileRemaining;
    // 멤버십: 이번 주기에서 무료 메시지 남은 횟수
    @Getter(AccessLevel.NONE)
    @Column(name = "m_monthly_extra_messages_remaining", nullable = false)
    private int membershipMonthlyFreeMessagesRemaining;
    // 멤버십: 이번 주기에서 무료 호감(Like) 남은 횟수
    @Getter(AccessLevel.NONE)
    @Column(name = "m_monthly_free_likes_remaining", nullable = false)
    private int membershipMonthlyFreeLikesRemaining;
    // 맴버쉽 구독 주기 시작 시각(결제 시각)
    @Column(name = "m_cycle_start_at")
    private LocalDateTime membershipCycleStartAt;
    // 맴버쉽 구독 만료 시각(결제 시각 + 1개월)
    @Column(name = "m_active_until")
    private LocalDateTime membershipActiveUntil;
    /*----------------*/

    /*---vip 관련---*/
    // VIP: 오늘 사용할 수 있는 추가 프로필 혜택 남은 횟수
    @Column(name = "vip_daily_extra_profiles_remaining", nullable = false)
    @Getter(AccessLevel.NONE)
    private int vipDailyExtraProfileRemaining;
    // VIP: 오늘 사용할 수 있는 무료 호감 혜택 남은 횟수
    @Column(name = "vip_daily_free_likes_remaining", nullable = false)
    @Getter(AccessLevel.NONE)
    private int vipDailyFreeLikesRemaining;
    // VIP: 오늘 사용할 수 있는 무료 메시지 혜택 남은 횟수
    @Column(name = "vip_daily_free_messages_remaining", nullable = false)
    @Getter(AccessLevel.NONE)
    private int vipDailyFreeMessagesRemaining;
    // VIP 일 혜택을 마지막으로 지급한 날짜
    @Column(name = "vip_granted_date")
    private LocalDate vipGrantedDate;
    /*----------------*/

    //동시 접근 방지를 위해 낙관적 락
    @Version
    private Long version;

    // 지갑 생성자: 모든 잔여 횟수/날짜/혜택 정보를 초기 상태로 세팅
    public TingWallet(Long userId) {
        this.userId = userId;
        this.ting = 0;
        this.eventTing = 0;

        this.dailyProfileRemaining = 0;
        this.dailyLoveViewRemaining = 0;
        this.extraProfilesByTingRemaining = 0;
        this.dailyProfileGrantedDate = null;
        this.dailyLoveViewGrantedDate = null;

        this.membershipMonthlyExtraProfileRemaining = 0;
        this.membershipMonthlyFreeMessagesRemaining = 0;
        this.membershipMonthlyFreeLikesRemaining = 0;

        this.membershipCycleStartAt = null;
        this.membershipActiveUntil = null;

        this.vipDailyExtraProfileRemaining = 0;
        this.vipDailyFreeLikesRemaining = 0;
        this.vipDailyFreeMessagesRemaining = 0;

        this.vipGrantedDate = null;
    }

    /**
     * 일반 팅을 증가시킵니다. (0 이하 금지)
     *
     * @param amount 증가시킬 팅 개수 (양수)
     * @throws IllegalArgumentException amount가 0 이하인 경우
     */
    public void addTing(int amount){
        if(amount <= 0){
            throw new IllegalArgumentException("추가하는 팅의 갯수는 양수여야 합니다.");
        }
        this.ting += amount;
    }

    /**
     * 일반 팅을 소모합니다. (0 이하 금지, 잔액 부족 시 예외)
     *
     * @param amount 소모할 팅 개수 (양수)
     * @throws IllegalArgumentException amount가 0 이하인 경우
     * @throws IllegalStateException 보유 팅이 amount보다 적은 경우
     */
    public void spendTing(int amount){
        if(amount <= 0){
            throw new IllegalArgumentException("소모하는 팅의 갯수는 양수여야 합니다.");
        }
        if(this.ting < amount){
            throw new IllegalStateException("소모하려는 팅보다 보유팅이 더 적습니다. 팅 보유 갯수 : " + this.ting);
        }
        this.ting -= amount;
    }

    /**
     * 이벤트 팅을 증가시킵니다. (0 이하 금지)
     *
     * @param amount 증가시킬 이벤트 팅 개수 (양수)
     * @throws IllegalArgumentException amount가 0 이하인 경우
     */
    public void addEventTing(int amount){
        if(amount <= 0){
            throw new IllegalArgumentException("추가하는 이벤트 팅의 갯수는 양수여야 합니다.");
        }
        this.eventTing += amount;
    }

    /**
     * 이벤트 팅을 소모합니다. (0 이하 금지, 잔액 부족 시 예외)
     *
     * @param amount 소모할 이벤트 팅 개수 (양수)
     * @throws IllegalArgumentException amount가 0 이하인 경우
     * @throws IllegalStateException 보유 이벤트 팅이 amount보다 적은 경우
     */
    public void spendEventTing(int amount){
        if(amount <= 0){
            throw new IllegalArgumentException("소모하는 이벤트 팅의 갯수는 양수여야 합니다.");
        }
        if(this.eventTing < amount){
            throw new IllegalStateException("소모하려는 이벤트 팅보다 보유팅이 더 적습니다. 이벤트 팅 보유 갯수 : " + this.eventTing);
        }
        this.eventTing -= amount;
    }

    /**
     * 오늘치 기본 프로필 지급(checkDailyProfile)을 먼저 호출했는지 검증합니다.
     * grantedDate가 오늘 날짜로 세팅되어 있지 않으면 예외를 던집니다.
     *
     * @param now 기준 날짜 (null이면 LocalDate.now() 사용)
     * @throws IllegalStateException 오늘 날짜로 지급 확인이 되어 있지 않은 경우
     */
    private void ensureDailyProfileGrantedToday(LocalDate now) {
        if (now == null) now = LocalDate.now();
        if (dailyProfileGrantedDate == null || !dailyProfileGrantedDate.equals(now)) {
            throw new IllegalStateException("잔여 갯수 확인 후 실행하여야합니다.");
        }
    }

    /**
     * 오늘치 기본 연애관 지급(checkDailyLoveView)을 먼저 호출했는지 검증합니다.
     * grantedDate가 오늘 날짜로 세팅되어 있지 않으면 예외를 던집니다.
     *
     * @param now 기준 날짜 (null이면 LocalDate.now() 사용)
     * @throws IllegalStateException 오늘 날짜로 지급 확인이 되어 있지 않은 경우
     */
    private void ensureDailyLoveViewGrantedToday(LocalDate now) {
        if (now == null) now = LocalDate.now();
        if (dailyLoveViewGrantedDate == null || !dailyLoveViewGrantedDate.equals(now)) {
            throw new IllegalStateException("잔여 갯수 확인 후 실행하여야합니다.");
        }
    }

    /**
     * 기본 제공 프로필 잔여 횟수를 조회하며, 날짜가 바뀌면 하루 1회 자동으로 초기화합니다.
     *
     * @param now          기준 날짜 (null이면 LocalDate.now() 사용)
     * @param profileCount 오늘 지급할 기본 제공 프로필 횟수(초기화 시 적용)
     * @return 오늘 기준 남은 기본 제공 프로필 횟수
     */
    public int checkDailyProfile(LocalDate now, int profileCount){
        if (now == null) now = LocalDate.now();
        if (dailyProfileGrantedDate == null || dailyProfileGrantedDate.isBefore(now)) {
            dailyProfileGrantedDate = now;
            dailyProfileRemaining = profileCount;
        }
        return dailyProfileRemaining;
    }

    /**
     * 기본 제공 프로필을 1회 소모합니다.
     * 반드시 checkDailyProfile을 통해 "오늘치 지급 확인"을 먼저 해야 합니다.
     *
     * @param now 기준 날짜 (null이면 LocalDate.now() 사용)
     * @throws IllegalStateException 오늘 지급 확인이 되어있지 않거나, 잔여 횟수가 0 이하인 경우
     */
    public void consumeDailyProfile(LocalDate now) {
        ensureDailyProfileGrantedToday(now);
        if (dailyProfileRemaining <= 0) throw new IllegalStateException("오늘 프로필 기본 제공 횟수가 없습니다.");
        dailyProfileRemaining--;
    }

    /**
     * 기본 제공 연애관 잔여 횟수를 조회하며, 날짜가 바뀌면 하루 1회 자동으로 초기화합니다.
     *
     * @param now            기준 날짜 (null이면 LocalDate.now() 사용)
     * @param loveViewCount  오늘 지급할 기본 제공 연애관 보기 횟수(초기화 시 적용)
     * @return 오늘 기준 남은 기본 제공 연애관 보기 횟수
     */
    public int checkDailyLoveView(LocalDate now, int loveViewCount){
        if (now == null) now = LocalDate.now();
        if (dailyLoveViewGrantedDate == null || dailyLoveViewGrantedDate.isBefore(now)) {
            dailyLoveViewGrantedDate = now;
            dailyLoveViewRemaining = loveViewCount;
        }
        return dailyLoveViewRemaining;
    }

    /**
     * 기본 제공 연애관 보기를 1회 소모합니다.
     * 반드시 checkDailyLoveView를 통해 "오늘치 지급 확인"을 먼저 해야 합니다.
     *
     * @param now 기준 날짜 (null이면 LocalDate.now() 사용)
     * @throws IllegalStateException 오늘 지급 확인이 되어있지 않거나, 잔여 횟수가 0 이하인 경우
     */
    public void consumeDailyLoveView(LocalDate now) {
        ensureDailyLoveViewGrantedToday(now);
        if (dailyLoveViewRemaining <= 0) throw new IllegalStateException("오늘 연애관 기본 제공 횟수가 없습니다.");
        dailyLoveViewRemaining--;
    }

    /**
     * 팅으로 결제한 추가 프로필(통합) 남은 횟수를 조회합니다.
     *
     * @return 팅 결제 추가 프로필 남은 횟수
     */
    public int checkExtraProfileByTing(){
        return this.extraProfilesByTingRemaining;
    }

    /**
     * 팅으로 결제한 추가 프로필(통합) 횟수를 추가합니다. (0 이하 금지)
     * 남은 횟수가 있는데도 호출되면 비정상 흐름으로 보고 로그를 남깁니다.
     *
     * @param amount 추가할 횟수 (양수)
     * @throws IllegalArgumentException amount가 0 이하인 경우
     */
    public void addExtraProfileByTing(int amount){
        if(amount <= 0)
            throw new IllegalArgumentException("추가하는 프로필의 갯수는 0 초과여야합니다.");
        if(extraProfilesByTingRemaining > 0)
            log.error("다 안 썼는데 이 매소드가 호출될 수 있나..?");
        this.extraProfilesByTingRemaining += amount;
    }

    /**
     * 팅으로 결제한 추가 프로필(통합)을 1회 소모합니다.
     *
     * @throws IllegalStateException 잔여 횟수가 0 이하인 경우
     */
    public void consumeExtraProfileByTing(){
        if(this.extraProfilesByTingRemaining <= 0)
            throw new IllegalStateException("추가 프로필 횟수가 없습니다.");
        this.extraProfilesByTingRemaining--;
    }

    /**
     * 멤버십을 활성화하고, 주기 정보 및 혜택 잔여량을 세팅합니다.
     *
     * @param cycleStartAt  멤버십 주기 시작 시각 (결제 시각 등)
     * @param extraProfiles 이번 주기 제공 추가 프로필 횟수 (0 이상)
     * @param freeMessages  이번 주기 제공 무료 메시지 횟수 (0 이상)
     * @param freeLikes     이번 주기 제공 무료 호감 횟수 (0 이상)
     * @throws IllegalArgumentException 필수 값이 null이거나, 기간/혜택 값이 유효하지 않은 경우
     */
    public void activateMembership(LocalDateTime cycleStartAt,
                                   int extraProfiles,
                                   int freeMessages,
                                   int freeLikes){
        if(cycleStartAt == null)
            throw new IllegalArgumentException("맵버쉽 시작 날짜는 필수로 포함되어야 합니다.");
        if(extraProfiles < 0 || freeMessages < 0 || freeLikes < 0)
            throw new IllegalArgumentException("혜택은 음수가 될 수 없습니다.");
        if(this.membershipActiveUntil != null && cycleStartAt.isBefore(this.membershipActiveUntil))
            throw new IllegalStateException("아직 맴버쉽 혜택이 끝나지 않은 상태입니다.");

        this.membershipCycleStartAt = cycleStartAt;
        this.membershipActiveUntil = cycleStartAt.plusMonths(1).minusDays(1);
        this.membershipMonthlyExtraProfileRemaining = extraProfiles;
        this.membershipMonthlyFreeMessagesRemaining = freeMessages;
        this.membershipMonthlyFreeLikesRemaining = freeLikes;
    }

    /**
     * 멤버십 활성 여부를 반환합니다.
     *
     * @param now 기준 시각 (null이면 LocalDateTime.now() 사용)
     * @return 만료 시각이 존재하고 now가 만료 시각 이전이면 true
     */
    public boolean isMembershipActive(LocalDateTime now){
        if(now == null)
            now = LocalDateTime.now();
        return this.membershipActiveUntil != null && now.isBefore(this.membershipActiveUntil);
    }

    public int checkMembershipFreeMessagesRemaining(LocalDateTime now) {
        ensureMembershipActive(now);
        return membershipMonthlyFreeMessagesRemaining;
    }

    public int checkMembershipFreeLikesRemaining(LocalDateTime now) {
        ensureMembershipActive(now);
        return membershipMonthlyFreeLikesRemaining;
    }

    public int checkMembershipExtraProfilesRemaining(LocalDateTime now) {
        ensureMembershipActive(now);
        return membershipMonthlyExtraProfileRemaining;
    }


    /**
     * 멤버십이 활성 상태인지 강제합니다.
     *
     * @param now 기준 시각 (null이면 LocalDateTime.now() 사용)
     * @throws IllegalStateException 멤버십이 만료되었거나 활성화되지 않은 경우
     */
    private void ensureMembershipActive(LocalDateTime now){
        if(!isMembershipActive(now)){
            throw new IllegalStateException("맴버쉽이 만료되었거나 활성화되지 않았습니다.");
        }
    }

    /**
     * 멤버십 추가 프로필 혜택을 1회 소모합니다.
     *
     * @param now 기준 시각 (null이면 LocalDateTime.now() 사용)
     * @throws IllegalStateException 멤버십이 비활성/만료이거나, 잔여 혜택이 0 이하인 경우
     */
    public void consumeMembershipExtraProfile(LocalDateTime now){
        ensureMembershipActive(now);
        if(this.membershipMonthlyExtraProfileRemaining <= 0){
            throw new IllegalStateException("맴버쉽 추가 프로필 혜택이 부족합니다.");
        }
        this.membershipMonthlyExtraProfileRemaining--;
    }

    /**
     * 멤버십 무료 호감 혜택을 1회 소모합니다.
     *
     * @param now 기준 시각 (null이면 LocalDateTime.now() 사용)
     * @throws IllegalStateException 멤버십이 비활성/만료이거나, 잔여 혜택이 0 이하인 경우
     */
    public void consumeMembershipFreeLike(LocalDateTime now){
        ensureMembershipActive(now);
        if(this.membershipMonthlyFreeLikesRemaining <= 0){
            throw new IllegalStateException("맴버쉽 무료 호감 갯수가 부족합니다.");
        }
        this.membershipMonthlyFreeLikesRemaining--;
    }

    /**
     * 멤버십 무료 메시지 혜택을 1회 소모합니다.
     *
     * @param now 기준 시각 (null이면 LocalDateTime.now() 사용)
     * @throws IllegalStateException 멤버십이 비활성/만료이거나, 잔여 혜택이 0 이하인 경우
     */
    public void consumeMembershipFreeMessage(LocalDateTime now){
        ensureMembershipActive(now);
        if(this.membershipMonthlyFreeMessagesRemaining <= 0){
            throw new IllegalStateException("맴버쉽 무료 메시지 혜택이 부족합니다.");
        }
        this.membershipMonthlyFreeMessagesRemaining--;
    }

    /**
     * VIP 여부를 확인하고, VIP라면 "하루 1회" 혜택을 지급합니다.
     * - 이미 오늘 vipGrantedDate가 세팅되어 있으면 VIP로 간주(true)
     * - ting >= vipThreshold이면 VIP, 날짜가 바뀐 첫 호출에서 혜택을 오늘치로 세팅
     *
     * @param vipThreshold VIP 기준 팅 보유량(이상일 때 VIP)
     * @param extraProfiles 오늘 지급할 VIP 추가 프로필 혜택 횟수
     * @param freeMessages  오늘 지급할 VIP 무료 메시지 혜택 횟수
     * @param freeLikes     오늘 지급할 VIP 무료 호감 혜택 횟수
     * @param now           기준 날짜 (null이면 LocalDate.now() 사용)
     * @return VIP면 true, 아니면 false
     */
    public boolean isVip(int vipThreshold,
                         int extraProfiles,
                         int freeMessages,
                         int freeLikes,
                         LocalDate now){
        if(now == null)
            now = LocalDate.now();
        if(vipGrantedDate != null && vipGrantedDate.isEqual(now))
            return true;
        if(this.ting >= vipThreshold){
            if(vipGrantedDate == null || vipGrantedDate.isBefore(now)){
                vipGrantedDate = now;
                vipDailyExtraProfileRemaining = extraProfiles;
                vipDailyFreeMessagesRemaining = freeMessages;
                vipDailyFreeLikesRemaining = freeLikes;
            }
            return true;
        }else
            return false;
    }

    /**
     * VIP 혜택 사용 전 검증: 오늘 VIP 확인 절차(isVip 호출로 vipGrantedDate 세팅)를 거쳤는지 확인합니다.
     *
     * @param now 기준 날짜 (null이면 LocalDate.now() 사용)
     * @throws IllegalStateException VIP가 아니거나, 오늘 날짜로 지급 확인이 되어 있지 않은 경우
     */
    private void ensureVip(LocalDate now){
        if(now == null)
            now = LocalDate.now();
        if(vipGrantedDate == null || !vipGrantedDate.equals(now)){
            throw new IllegalStateException("Vip가 아니거나 확인 절차를 거치지 않았습니다.");
        }
    }

    public int checkVipFreeMessagesRemaining(LocalDate now) {
        ensureVip(now);
        return vipDailyFreeMessagesRemaining;
    }

    public int checkVipFreeLikesRemaining(LocalDate now) {
        ensureVip(now);
        return vipDailyFreeLikesRemaining;
    }

    public int checkVipExtraProfilesRemaining(LocalDate now) {
        ensureVip(now);
        return vipDailyExtraProfileRemaining;
    }

    /**
     * VIP 추가 프로필 혜택을 1회 소모합니다.
     *
     * @param now 기준 날짜 (null이면 LocalDate.now() 사용)
     * @throws IllegalStateException 오늘 VIP 확인이 되어있지 않거나, 잔여 혜택이 0 이하인 경우
     */
    public void consumeVipExtraProfile(LocalDate now){
        ensureVip(now);
        if (this.vipDailyExtraProfileRemaining <= 0) {
            throw new IllegalStateException("VIP 추가 프로필 혜택이 부족합니다.");
        }
        this.vipDailyExtraProfileRemaining--;
    }

    /**
     * VIP 무료 호감 혜택을 1회 소모합니다.
     *
     * @param now 기준 날짜 (null이면 LocalDate.now() 사용)
     * @throws IllegalStateException 오늘 VIP 확인이 되어있지 않거나, 잔여 혜택이 0 이하인 경우
     */
    public void consumeVipFreeLike(LocalDate now){
        ensureVip(now);
        if (this.vipDailyFreeLikesRemaining <= 0) {
            throw new IllegalStateException("VIP 무료 호감 혜택이 부족합니다.");
        }
        this.vipDailyFreeLikesRemaining--;
    }

    /**
     * VIP 무료 메시지 혜택을 1회 소모합니다.
     *
     * @param now 기준 날짜 (null이면 LocalDate.now() 사용)
     * @throws IllegalStateException 오늘 VIP 확인이 되어있지 않거나, 잔여 혜택이 0 이하인 경우
     */
    public void consumeVipFreeMessage(LocalDate now){
        ensureVip(now);
        if (this.vipDailyFreeMessagesRemaining <= 0) {
            throw new IllegalStateException("VIP 무료 메시지 혜택이 부족합니다.");
        }
        this.vipDailyFreeMessagesRemaining--;
    }
}