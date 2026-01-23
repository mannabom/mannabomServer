package mannabom_server.manabom.application.matching.profileMatching.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.matching.profileMatching.dto.request.ProfileMatchConditionRequestDto;
import mannabom_server.manabom.application.matching.profileMatching.dto.response.ProfileMatchConditionResponseDto;
import mannabom_server.manabom.application.signup.service.S3FileUploadService;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.matching.profileMatching.entity.ProfileRecommendHistory;
import mannabom_server.manabom.domain.matching.profileMatching.enums.RecommendType;
import mannabom_server.manabom.domain.matching.profileMatching.repository.ProfileRecommendHistoryRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.ProfileImage;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileImageRepository;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.policy.config.MatchPolicyProperties;
import mannabom_server.manabom.policy.config.TingPolicyProperties;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static org.aspectj.util.LangUtil.safeList;

//        ** 이 매소드 확인하고 정책값들 스냅샷으로 반영되는지 확인하고 고치기

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileMatchService {
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ProfileImageRepository profileImageRepository;

    private final ProfileRecommendHistoryRepository historyRepository;
    private final RuntimePolicyService runtimePolicyService;
    private final S3FileUploadService s3FileUploadService;

    private final TingWalletRepository tingWalletRepository;

    @Transactional
    public ProfileMatchConditionResponseDto matchFree(Long requesterUserId, ProfileMatchConditionRequestDto request) {
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
    public ProfileMatchConditionResponseDto matchExtra(Long requesterUserId, ProfileMatchConditionRequestDto req){
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
            vipExtraProfileCount = tingWallet.getVipDailyExtraProfileRemaining();
        }

        int membershipExtraProfileCount = 0;
        if(tingWallet.isMembershipActive(now))
            membershipExtraProfileCount = tingWallet.getMembershipMonthlyExtraProfileRemaining();

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

    private ProfileMatchConditionResponseDto toResponse(Profile profile) {
        String url = profileImageRepository.findByProfileAndIsMainTrue(profile)
                .map(ProfileImage::getUrl)
                .orElse(null);

        String presignedUrl = null;
        if (url != null) {
            String s3Key = s3FileUploadService.extractS3KeyFromUrl(url);
            if (s3Key != null && !s3Key.isBlank()) {
                presignedUrl = s3FileUploadService.presignedGetUrl(s3Key, Duration.ofMinutes(10));
            }
        }

        int age = LocalDate.now().getYear() - profile.getBirthDate().getYear() + 1; // 2026 - 2002 + 1 = 25

        return new ProfileMatchConditionResponseDto(
                profile.getProfileId(),
                presignedUrl,
                age,
                profile.getMbti(),
                profile.getAlcohol(),
                profile.getSmoking()
        );
    }

    private Profile pickOneCandidate(Long requesterUserId, Profile requesterProfile, ProfileMatchConditionRequestDto request, RuntimePolicySnapshot p) {
        LocalDate today = LocalDate.now();
        LocalDate minDate = minDateFromMaxAge(today, request.getMaxAge());
        LocalDate maxDate = maxDateFromMinAge(today, request.getMinAge());

        LocalDateTime cooldownFrom = LocalDateTime.now().minusHours(p.getMatch().getCooldownHours());

        Page<Profile> page = profileRepository.findMatchCandidates(
                requesterUserId,
                requesterProfile.getGender(),
                minDate, maxDate,
                request.getSmoking() == null || request.getSmoking().isEmpty(), safeList(request.getSmoking()),
                request.getDrinking() == null || request.getDrinking().isEmpty(), safeList(request.getDrinking()),
                requesterProfile.getRegionSido(),
                requesterProfile.getRegionSigungu(),
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
        boolean sameSido = Objects.equals(target.getRegionSido(), requesterProfile.getRegionSido());
        boolean sameSigungu = Objects.equals(target.getRegionSigungu(), requesterProfile.getRegionSigungu());
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
}
