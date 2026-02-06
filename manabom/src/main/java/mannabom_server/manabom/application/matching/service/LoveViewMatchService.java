package mannabom_server.manabom.application.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.matching.dto.request.MatchConditionRequestDto;
import mannabom_server.manabom.application.matching.dto.response.LoveViewMatchConditionResponseDto;
import mannabom_server.manabom.application.matching.dto.response.RecommendedTodayLoveViewListResponseDto;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.matching.entity.LoveViewRecommendHistory;
import mannabom_server.manabom.domain.matching.repository.LoveViewRecommendHistoryRepository;
import mannabom_server.manabom.domain.matching.enums.RecommendType;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;
import mannabom_server.manabom.domain.question.repository.QuestionAnswerRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoveViewMatchService {
    private final RuntimePolicyService runtimePolicyService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final TingWalletRepository tingWalletRepository;
    private final LoveViewRecommendHistoryRepository loveViewRecommendHistoryRepository;
    private final QuestionAnswerRepository questionAnswerRepository;

    @Transactional
    public LoveViewMatchConditionResponseDto matchFree(Long requesterUserId, MatchConditionRequestDto request){
        RuntimePolicySnapshot p = runtimePolicyService.snapshot();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(()->new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        Profile requesterProfile = profileRepository.findByUser(requester)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 프로필을 찾을 수 없습니다."));

        validateAgeRange(request.getMinAge(), request.getMaxAge());

        Profile pickedProfile = pickOneCandidate(requesterUserId, requesterProfile, request, p);
        User pickedUser = pickedProfile.getUser();

        List<QuestionAnswer> questionAnswers = questionAnswerRepository.findByProfileWithQuestion(pickedProfile);

        questionAnswers.sort(
                Comparator.comparingLong(qa -> qa.getQuestion().getQuestionId())
        );

        TingWallet tingWallet = tingWalletRepository.findByUserIdForUpdate(requesterUserId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(requesterUserId)));

        int freeLoveViewRemains = tingWallet.checkDailyLoveView(today, p.getBenefit().getBasic().getDailyLoveView());

        if(freeLoveViewRemains <= 0)
            throw new IllegalStateException("무료 연애관 매칭권이 부족합니다.");
        else {
            tingWallet.consumeDailyLoveView(today);
            log.info("연애관 매칭 무료권 사용 완료, 남은 무료권 : {}", freeLoveViewRemains-1);
        }

        loveViewRecommendHistoryRepository.save(new LoveViewRecommendHistory(requesterUserId, pickedUser.getUserId(), RecommendType.FREE, now));

        return toResponse(today, pickedProfile, questionAnswers);
    }

    @Transactional
    public LoveViewMatchConditionResponseDto matchExtra(Long requesterId, MatchConditionRequestDto request){
        RuntimePolicySnapshot p = runtimePolicyService.snapshot();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        Profile requesterProfile = profileRepository.findByUser(requester)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 프로필을 찾을 수 없습니다."));

        validateAgeRange(request.getMinAge(), request.getMaxAge());

        Profile pickedProfile = pickOneCandidate(requester.getUserId(), requesterProfile, request, p);
        User pickedUser = pickedProfile.getUser();

        List<QuestionAnswer> qnas = questionAnswerRepository.findByProfileWithQuestion(pickedProfile);
        qnas.sort(
                Comparator.comparingLong(qa -> qa.getQuestion().getQuestionId())
        );

        TingWallet tingWallet = tingWalletRepository.findByUserIdForUpdate(requester.getUserId())
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(requester.getUserId())));

        if(tingWallet.checkDailyLoveView(today, p.getBenefit().getBasic().getDailyLoveView()) > 0)
            throw new IllegalStateException("아직 연애관 매칭 무료권을 모두 사용하지 않았습니다.");

        int extraLoveViewByTing = tingWallet.checkExtraProfileByTing();
        int extraLoveViewByVip = 0;
        if(tingWallet.isVip(
                p.getTing().getVipThreshold(),
                p.getBenefit().getVip().getDailyExtraProfiles(),
                p.getBenefit().getVip().getDailyFreeMessages(),
                p.getBenefit().getVip().getDailyFreeLikes(),
                today
                )){
            extraLoveViewByVip = tingWallet.checkVipExtraProfilesRemaining(today);
        }
        int extraLoveViewByMembership = 0;
        if(tingWallet.isMembershipActive(now)){
            extraLoveViewByMembership = tingWallet.checkMembershipExtraProfilesRemaining(now);
        }

        if((extraLoveViewByTing + extraLoveViewByVip + extraLoveViewByMembership) <= 0)
            throw new IllegalStateException("남아있는 추가 연애관 혜택권이 없습니다");

        if(extraLoveViewByVip > 0){
            tingWallet.consumeVipExtraProfile(today);
            log.info("Vip 연애관 소개팅 추가 혜택권 사용, 남은 혜택권 : {}", tingWallet.checkVipExtraProfilesRemaining(today));
            loveViewRecommendHistoryRepository.save(new LoveViewRecommendHistory(requester.getUserId(), pickedUser.getUserId(), RecommendType.EXTRA_VIP, now));
        }else if(extraLoveViewByMembership > 0){
            tingWallet.consumeMembershipExtraProfile(now);
            log.info("맴버쉽 연애관 소개팅 추가 혜택권 사용, 남은 혜택권 : {}", tingWallet.checkMembershipExtraProfilesRemaining(now));
            loveViewRecommendHistoryRepository.save(new LoveViewRecommendHistory(requester.getUserId(), pickedUser.getUserId(), RecommendType.EXTRA_MEMBERSHIP, now));
        }else{
            tingWallet.consumeExtraProfileByTing();
            log.info("팅으로 결제한 연애관 소개팅 추가 혜택권 사용, 남은 혜택권 : {}", tingWallet.checkExtraProfileByTing());
            loveViewRecommendHistoryRepository.save(new LoveViewRecommendHistory(requester.getUserId(), pickedUser.getUserId(), RecommendType.EXTRA_TING, now));
        }

        return toResponse(today, pickedProfile, qnas);
    }

    @Transactional(readOnly = true)
    public RecommendedTodayLoveViewListResponseDto getRecommendedTodayLoveViewList(Long userId){
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<LoveViewRecommendHistory> histories =
                loveViewRecommendHistoryRepository.findByRequesterUserIdAndRecommendedAtGreaterThanEqualAndRecommendedAtLessThanOrderByRecommendedAtDesc(
                        userId, start, end
                );

        if(histories.isEmpty()){
            return new RecommendedTodayLoveViewListResponseDto(List.of());
        }

        List<Long> targetUserIds = histories.stream()
                .map(LoveViewRecommendHistory::getTargetUserId)
                .toList();

        List<Profile> targetProfiles = profileRepository.findAllByUser_UserIdIn(targetUserIds);

        Map<Long, Profile> map = targetProfiles.stream()
                .collect(java.util.stream.Collectors.toMap(
                        p -> p.getUser().getUserId(),
                        p -> p,
                        (a, b) -> a
                ));

        List<LoveViewMatchConditionResponseDto> result = new ArrayList<>();
        for(LoveViewRecommendHistory h : histories){
            Profile profile = map.get(h.getTargetUserId());
            if(profile == null) continue;

            List<QuestionAnswer> qnas = questionAnswerRepository.findByProfileWithQuestion(profile);
            qnas.sort(Comparator.comparingLong(qa -> qa.getQuestion().getQuestionId()));

            result.add(toResponse(today, profile, qnas));
        }

        return new RecommendedTodayLoveViewListResponseDto(result);
    }

    private LoveViewMatchConditionResponseDto toResponse(
            LocalDate today, Profile pickedProfile,
            List<QuestionAnswer> questionAnswers
    ){
        int age = today.getYear() - pickedProfile.getBirthDate().getYear() + 1; // 2026 - 2002 + 1 = 25

        return new LoveViewMatchConditionResponseDto(
                pickedProfile.getProfileId(),
                age,
                pickedProfile.getMbti(),
                pickedProfile.getAlcohol(),
                pickedProfile.getSmoking(),
                questionAnswers
                );
    }

    private void validateAgeRange(Integer minAge, Integer maxAge){
        if(minAge ==  null || maxAge == null)
            throw new IllegalArgumentException("최소 나이 또는 최대 나이가 비어있습니다.");
        if(minAge > maxAge)
            throw new IllegalArgumentException("최소 나이는 최대 나이보다 클 수 없습니다.");

    }

    private LocalDate minDateFromMaxAge(LocalDate today, int maxAge){
        int yearFrom = today.getYear() - maxAge + 1;
        return LocalDate.of(yearFrom, 1, 1);
    }

    private LocalDate maxDateFromMinAge(LocalDate today, int minAge) {
        int yearTo = today.getYear() - minAge + 1;
        return LocalDate.of(yearTo, 12, 31);
    }

    private Profile pickOneCandidate(Long requesterUserId, Profile requesterProfile, MatchConditionRequestDto request, RuntimePolicySnapshot p) {
        LocalDate today = LocalDate.now();
        LocalDate minDate = minDateFromMaxAge(today, request.getMaxAge());
        LocalDate maxDate = maxDateFromMinAge(today, request.getMinAge());

        LocalDateTime cooldownFrom = LocalDateTime.now().minusHours(p.getMatch().getCooldownHours());

        List<SmokingHabit> smoking = request.getSmoking() == null ? List.of() : request.getSmoking();
        List<DrinkingHabit> drinking = request.getDrinking() == null ? List.of() : request.getDrinking();

        boolean smokingEmpty = smoking.isEmpty();
        boolean drinkingEmpty = drinking.isEmpty();

        Page<Profile> page = profileRepository.findLoveViewMatchCandidates(
                requesterUserId,
                requesterProfile.getGender(),
                minDate, maxDate,
                smokingEmpty,
                smoking,
                drinkingEmpty,
                drinking,
                requesterProfile.getRegion().getSidoName(),
                requesterProfile.getRegion().getSigunguName(),
                cooldownFrom,
                PageRequest.of(0, p.getMatch().getCandidatePoolSize())
        );

        List<Profile> candidates = page.getContent();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("조건에 맞는 상대가 없습니다.");
        }

        int bestPriority = computePriority(candidates.get(0), requesterProfile);

        List<Profile> bestGroup = new ArrayList<>();
        for (Profile profile : candidates) {
            int pr = computePriority(profile, requesterProfile);
            if (pr != bestPriority) break;
            bestGroup.add(profile);
            if (bestGroup.size() >= p.getMatch().getPickPoolSize()) break;
        }

        return bestGroup.get(ThreadLocalRandom.current().nextInt(bestGroup.size()));
    }

    /**
     *
     * @param target 대상 프로필
     * @param requesterProfile 요청자 프로필
     * @return 요청자의 주소와 대상의 주소를 비교하여 우선순위를 계산하여 리턴해줌
     */
    private int computePriority(Profile target, Profile requesterProfile) {
        boolean sameSido = Objects.equals(target.getRegion().getSidoName(), requesterProfile.getRegion().getSidoName());
        boolean sameSigungu = Objects.equals(target.getRegion().getSigunguName(), requesterProfile.getRegion().getSigunguName());
        if (sameSido && sameSigungu) return 0;
        if (sameSido) return 1;
        return 2;
    }
}
