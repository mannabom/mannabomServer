package mannabom_server.manabom.application.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.matching.dto.request.MatchConditionRequestDto;
import mannabom_server.manabom.application.matching.dto.response.ProfileMatchConditionResponseDto;
import mannabom_server.manabom.application.matching.dto.response.RecommendedTodayProfileListResponseDto;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.matching.entity.ProfileRating;
import mannabom_server.manabom.domain.matching.entity.ProfileRecommendHistory;
import mannabom_server.manabom.domain.matching.enums.RecommendType;
import mannabom_server.manabom.domain.matching.repository.ProfileRatingRepository;
import mannabom_server.manabom.domain.matching.repository.ProfileRecommendHistoryRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.ProfileImage;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;
import mannabom_server.manabom.domain.user.repository.ProfileImageRepository;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileMatchService {
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ProfileImageRepository profileImageRepository;

    private final ProfileRecommendHistoryRepository historyRepository;
    private final RuntimePolicyService runtimePolicyService;
    private final FileStoragePort fileStoragePort;

    private final TingWalletRepository tingWalletRepository;
    private final ProfileRatingRepository profileRatingRepository;

    @Transactional
    public ProfileMatchConditionResponseDto matchFree(Long requesterUserId, MatchConditionRequestDto request) {
        RuntimePolicySnapshot p = runtimePolicyService.snapshot();
        LocalDate today = LocalDate.now();

        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Profile requesterProfile = profileRepository.findByUser(requester)
                .orElseThrow(() -> new IllegalArgumentException("프로필이 없습니다."));

        validateAgeRange(request.getMinAge(), request.getMaxAge());

        Profile picked = pickOneCandidate(requesterUserId, requesterProfile, request, p);

        TingWallet tingwallet = tingWalletRepository.findByUserIdForUpdate(requesterUserId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(requesterUserId)));


        int remains = tingwallet.checkDailyProfile(today, p.getBenefit().getBasic().getDailyProfile());

        if(remains > 0){
            tingwallet.consumeDailyProfile(today);
            log.info("프로필 매칭 무료권 사용 완료, 남은 무료권 : {}", remains-1);
        }else{
            log.info("프로필 매칭 무료권이 부족합니다.");
            throw new IllegalStateException("프로필 매칭 무료권이 부족합니다.");
        }

        historyRepository.save(new ProfileRecommendHistory(
                requesterUserId, picked.getUser().getUserId(), RecommendType.FREE, LocalDateTime.now()
        ));

        return toResponse(picked);
    }

    @Transactional
    public ProfileMatchConditionResponseDto matchExtra(Long requesterUserId, MatchConditionRequestDto req){
        RuntimePolicySnapshot p = runtimePolicyService.snapshot();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Profile requesterProfile = profileRepository.findByUser(requester)
                .orElseThrow(() -> new IllegalArgumentException("프로필이 없습니다."));

        validateAgeRange(req.getMinAge(), req.getMaxAge());

        Profile picked = pickOneCandidate(requesterUserId, requesterProfile, req, p);

        TingWallet tingWallet = tingWalletRepository.findByUserIdForUpdate(requesterUserId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(requesterUserId)));

        int freeProfileCount = tingWallet.checkDailyProfile(today, p.getBenefit().getBasic().getDailyProfile());

        if(freeProfileCount > 0) throw new IllegalStateException("무료 프로필 소개팅을 아직 모두 소비하지 않았습니다");

        int vipExtraProfileCount = 0;
        if (tingWallet.isVip(
                p.getTing().getVipThreshold(),
                p.getBenefit().getVip().getDailyExtraProfiles(),
                p.getBenefit().getVip().getDailyFreeMessages(),
                p.getBenefit().getVip().getDailyFreeLikes(),
                today
        )) {
            vipExtraProfileCount = tingWallet.checkVipExtraProfilesRemaining(today);
        }

        int membershipExtraProfileCount = 0;
        if(tingWallet.isMembershipActive(now))
            membershipExtraProfileCount = tingWallet.checkMembershipExtraProfilesRemaining(now);

        int tingExtraProfileCount = tingWallet.checkExtraProfileByTing();

        if(vipExtraProfileCount + membershipExtraProfileCount + tingExtraProfileCount <= 0){
            throw new IllegalArgumentException("남아있는 추가 프로필 혜택권이 없습니다.(팅, vip, 맴버쉽)");
        }

        // 혜택권 사용 우선순위 : 1.vip 2.맴버쉽 3.ting으로 구매한 추가 프로필
        if(vipExtraProfileCount > 0){
            tingWallet.consumeVipExtraProfile(today);
            log.info("vip 프로필 매칭권 사용 완료, 남은 혜택권 : {}", vipExtraProfileCount-1);
            historyRepository.save(
                    new ProfileRecommendHistory(requesterUserId, picked.getUser().getUserId(), RecommendType.EXTRA_VIP, now)
            );
        }else if(membershipExtraProfileCount > 0){
            tingWallet.consumeMembershipExtraProfile(now);
            log.info("맴버쉽 프로필 매칭권 사용 완료, 남은 혜택권 : {}", membershipExtraProfileCount-1);
            historyRepository.save(
                    new ProfileRecommendHistory(requesterUserId, picked.getUser().getUserId(), RecommendType.EXTRA_MEMBERSHIP, now)
            );
        }else{
            tingWallet.consumeExtraProfileByTing();
            log.info("ting 프로필 매칭권 사용 완료, 남은 혜택권 : {}", tingExtraProfileCount-1);
            historyRepository.save(
                    new ProfileRecommendHistory(requesterUserId, picked.getUser().getUserId(), RecommendType.EXTRA_TING, now)
            );
        }

        return toResponse(picked);
    }

    @Transactional(readOnly = true)
    public RecommendedTodayProfileListResponseDto getRecommendedTodayProfileList(Long userId){
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<ProfileRecommendHistory> histories =
                historyRepository.findByRequesterUserIdAndRecommendedAtGreaterThanEqualAndRecommendedAtLessThanOrderByRecommendedAtDesc(
                        userId, start, end
                );

        if(histories.isEmpty()){
            return new RecommendedTodayProfileListResponseDto(List.of());
        }

        List<Long> targetUserIds = new ArrayList<>();
        for(ProfileRecommendHistory history : histories){
            targetUserIds.add(history.getTargetUserId());
        }

        List<Profile> targetProfiles = profileRepository.findAllByUser_UserIdIn(targetUserIds);

        Map<Long, Profile> map = targetProfiles.stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.getUser().getUserId(), p -> p));

        List<ProfileMatchConditionResponseDto> result = new ArrayList<>();
        for(ProfileRecommendHistory history : histories){
            Profile profile = map.get(history.getTargetUserId());
            if(profile != null){
                result.add(toResponse(profile));
            }
        }

        return new RecommendedTodayProfileListResponseDto(result);
    }

    private ProfileMatchConditionResponseDto toResponse(Profile profile) {
        String url = profileImageRepository.findByProfileAndIsMainTrue(profile)
                .map(ProfileImage::getUrl)
                .orElse(null);

        String presignedUrl = null;
        if (url != null) {
            String fileKey = fileStoragePort.extractKeyFromUrl(url);
            if (fileKey != null && !fileKey.isBlank()) {
                presignedUrl = fileStoragePort.presignedGetUrl(fileKey, Duration.ofMinutes(10));
            }
        }

        int age = LocalDate.now().getYear() - profile.getBirthDate().getYear() + 1; // 2026 - 2002 + 1 = 25

        return new ProfileMatchConditionResponseDto(
                profile.getProfileId(),
                profile.getNickName(),
                presignedUrl,
                age,
                profile.getMbti(),
                profile.getAlcohol(),
                profile.getSmoking()
        );
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

        Page<Profile> page = profileRepository.findProfileMatchCandidates(
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

    /**
     *
     * @param today 현재 날짜
     * @param maxAge 최대 나이
     * @return 현재 날짜를 기준으로 최대 나이를 반영하여 최소 출생연도로 변환
     */
    private LocalDate minDateFromMaxAge(LocalDate today, int maxAge) {
        int yearFrom = today.getYear() - maxAge + 1; // 2026 - 25 + 1 = 2002
        return LocalDate.of(yearFrom, 1, 1);
    }

    /**
     *
     * @param today 현재 날짜
     * @param minAge 최소 나이
     * @return 현재 날짜를 기준으로 최소 나이를 반영하여 최대 출생연도로 변환
     */
    private LocalDate maxDateFromMinAge(LocalDate today, int minAge) {
        int yearTo = today.getYear() - minAge + 1; // 2026 - 20 + 1 = 2007
        return LocalDate.of(yearTo, 12, 31);
    }

    private void validateAgeRange(Integer minAge, Integer maxAge) {
        if (minAge == null || maxAge == null) throw new IllegalArgumentException("minAge/maxAge는 필수입니다.");
        if (minAge > maxAge) throw new IllegalArgumentException("minAge는 maxAge보다 클 수 없습니다.");
    }

    @Transactional
    public void rate(Long fromUserId, Long targetProfileId, int score){
        if(fromUserId == null){
            throw new IllegalArgumentException("fromUserId가 비어있습니다.");
        }
        if(targetProfileId == null){
            throw new IllegalArgumentException("toUserId가 비어있습니다.");
        }
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("score의 범위는 1~5 여야합니다.");
        }

        Profile targetProfile = profileRepository.findByIdForUpdate(targetProfileId)
                .orElseThrow(() -> new IllegalArgumentException("대상의 프로필이 존재하지 않습니다."));
        Long targetUserId = targetProfile.getUser().getUserId();

        if(fromUserId.equals(targetUserId)){
            throw new IllegalArgumentException("자기자신은 평가할 수 없습니다.");
        }

        if(profileRatingRepository.existsByFromUserIdAndTargetUserId(fromUserId, targetUserId)){
            throw new IllegalStateException("이미 평가한 상대입니다.");
        }

        int count = profileRatingRepository.countByTargetUserId(targetUserId);

        try{
            ProfileRating rating = ProfileRating.builder()
                    .fromUserId(fromUserId)
                    .targetUserId(targetUserId)
                    .score(score)
                    .build();
            profileRatingRepository.save(rating);
            targetProfile.applyNewRating(score, count);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("이미 평가한 상대입니다.");
        }
    }
}
