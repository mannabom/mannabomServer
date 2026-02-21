package mannabom_server.manabom.application.partner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.partner.dto.common.LikedDto;
import mannabom_server.manabom.application.partner.dto.common.MessagedDto;
import mannabom_server.manabom.application.partner.dto.request.GetReceivedScoreRequestDto;
import mannabom_server.manabom.application.partner.dto.request.GetTargetProfileDetailRequestDto;
import mannabom_server.manabom.application.partner.dto.request.PurchaseAdditionalProfileByTingRequestDto;
import mannabom_server.manabom.application.partner.dto.request.UnlockTargetPhotoRequestDto;
import mannabom_server.manabom.application.partner.dto.response.*;
import mannabom_server.manabom.application.signup.service.S3FileUploadService;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.likeRequest.entity.LikeRequest;
import mannabom_server.manabom.domain.likeRequest.enums.LikeStatus;
import mannabom_server.manabom.domain.likeRequest.repository.LikeRequestRepository;
import mannabom_server.manabom.domain.matching.entity.ProfileRating;
import mannabom_server.manabom.domain.matching.repository.ProfileRatingRepository;
import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;
import mannabom_server.manabom.domain.messageRequest.repository.MessageRequestRepository;
import mannabom_server.manabom.domain.partner.entity.ProfileExtraPhotoUnlock;
import mannabom_server.manabom.domain.partner.repository.ProfileExtraPhotoUnlockRepository;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;
import mannabom_server.manabom.domain.question.repository.QuestionAnswerRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.ProfileImage;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileImageRepository;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {
    private final ProfileRepository profileRepository;
    private final ProfileImageRepository profileImageRepository;
    private final UserRepository userRepository;
    private final S3FileUploadService s3FileUploadService;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final LikeRequestRepository likeRequestRepository;
    private final MessageRequestRepository messageRequestRepository;
    private final ProfileExtraPhotoUnlockRepository profileExtraPhotoUnlockRepository;
    private final RuntimePolicyService runtimePolicyService;
    private final TingWalletRepository tingWalletRepository;
    private final ProfileRatingRepository profileRatingRepository;

    @Transactional(readOnly = true)
    public GetTargetProfileDetailResponseDto getTargetProfileDetail(Long requesterUserId, GetTargetProfileDetailRequestDto request){
        User requesterUser = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("요청자의 유저 정보를 찾을 수 없습니다."));
        Profile requesterProfile = profileRepository.findByUser(requesterUser)
                .orElseThrow(() -> new IllegalArgumentException("요청자의 프로필을 찾을 수 없습니다."));
        int requesterProfileImageNum = profileImageRepository.countByProfile(requesterProfile);

        Long targetProfileId = request.getTargetProfileId();
        Profile targetProfile = profileRepository.findById(targetProfileId)
                .orElseThrow(() -> new IllegalArgumentException("상대방의 프로필을 찾을 수 없습니다."));
        Long targetUserId = targetProfile.getUser().getUserId();

        List<ProfileImage> targetProfileImages = profileImageRepository.findAllByProfile(targetProfile);
        List<Long> unlockedPhotoIds = profileExtraPhotoUnlockRepository.findUnlockedExtraPhotoIds(requesterUserId, targetUserId);
        Set<Long> unlockedSet = new HashSet<>(unlockedPhotoIds);

        List<GetTargetProfileDetailResponseDto.Photo> photos = new ArrayList<>();

        for(int i = 0; i < targetProfileImages.size(); i++){
            Long photoId = targetProfileImages.get(i).getImageId();
            String photoUrl = targetProfileImages.get(i).getUrl();
            String photoKey = s3FileUploadService.extractS3KeyFromUrl(photoUrl);
            String presignedPhotoUrl = s3FileUploadService.presignedGetUrl(photoKey, Duration.ofMinutes(10));

            if(i >= requesterProfileImageNum && !unlockedSet.contains(photoId)) {
                photos.add(new GetTargetProfileDetailResponseDto.Photo(photoId, presignedPhotoUrl, true));
            }else {
                photos.add(new GetTargetProfileDetailResponseDto.Photo(photoId, presignedPhotoUrl, false));
            }
        }

        List<QuestionAnswer> questionAnswers = questionAnswerRepository.findByProfileWithQuestion(targetProfile);

        int age = LocalDate.now().getYear() - targetProfile.getBirthDate().getYear() + 1; // 2026 - 2002 + 1 = 25
        String region = targetProfile.getRegion().getSidoName() + " " + targetProfile.getRegion().getSigunguName();

        Optional<LikeRequest> likeRequestOpt = likeRequestRepository.findByFromUserIdAndToUserId(requesterUserId, targetUserId);
        boolean likeRequestExists = likeRequestOpt.isPresent();
        LikeStatus likeStatus = null;
        if(likeRequestExists){
            likeStatus = likeRequestOpt.get().getStatus();
        }

        Optional<MessageRequest> messageRequestOpt = messageRequestRepository.findByFromUserIdAndToUserId(requesterUserId, targetUserId);
        boolean messageRequestExists = messageRequestOpt.isPresent();
        MessageRequestStatus messageRequestStatus = null;
        if(messageRequestExists){
            messageRequestStatus = messageRequestOpt.get().getStatus();
        }

        return GetTargetProfileDetailResponseDto.builder()
                .nickname(targetProfile.getNickName())
                .age(age)
                .height(targetProfile.getHeight())
                .bodyType(targetProfile.getBodyType())
                .region(region)
                .questionAnswers(questionAnswers)
                .photos(photos)
                .smoking(targetProfile.getSmoking())
                .drinking(targetProfile.getAlcohol())
                .liked(new LikedDto(likeRequestExists, likeStatus))
                .messaged(new MessagedDto(messageRequestExists, messageRequestStatus))
                .build();
    }

    @Transactional(readOnly = true)
    public GetTargetLoveViewDetailResponseDto getTargetLoveViewDetail(Long requesterUserId, Long targetProfileId){
        userRepository.findById(requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("요청자의 유저 정보를 찾을 수 없습니다."));

        Profile targetProfile = profileRepository.findById(targetProfileId)
                .orElseThrow(() -> new IllegalArgumentException("상대방의 프로필을 찾을 수 없습니다."));
        Long targetUserId = targetProfile.getUser().getUserId();

        List<QuestionAnswer> questionAnswers = questionAnswerRepository.findByProfileWithQuestion(targetProfile);

        int age = LocalDate.now().getYear() - targetProfile.getBirthDate().getYear() + 1; // 2026 - 2002 + 1 = 25
        String region = targetProfile.getRegion().getSidoName() + " " + targetProfile.getRegion().getSigunguName();

        Optional<LikeRequest> likeRequestOpt = likeRequestRepository.findByFromUserIdAndToUserId(requesterUserId, targetUserId);
        boolean likeRequestExists = likeRequestOpt.isPresent();
        LikeStatus likeStatus = null;
        if(likeRequestExists){
            likeStatus = likeRequestOpt.get().getStatus();
        }

        Optional<MessageRequest> messageRequestOpt = messageRequestRepository.findByFromUserIdAndToUserId(requesterUserId, targetUserId);
        boolean messageRequestExists = messageRequestOpt.isPresent();
        MessageRequestStatus messageRequestStatus = null;
        if(messageRequestExists){
            messageRequestStatus = messageRequestOpt.get().getStatus();
        }

        return GetTargetLoveViewDetailResponseDto.builder()
                .nickname(targetProfile.getNickName())
                .age(age)
                .region(region)
                .questionAnswers(questionAnswers)
                .smokingHabit(targetProfile.getSmoking())
                .drinkingHabit(targetProfile.getAlcohol())
                .liked(new LikedDto(likeRequestExists, likeStatus))
                .messaged(new MessagedDto(messageRequestExists, messageRequestStatus))
                .build();
    }

    @Transactional
    public UnlockTargetPhotoResponseDto unlockTargetPhoto(Long requesterUserId, UnlockTargetPhotoRequestDto request) {
        RuntimePolicySnapshot p = runtimePolicyService.snapshot();

        userRepository.findById(requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("요청자의 유저 정보를 찾을 수 없습니다."));

        Profile targetProfile = profileRepository.findById(request.getTargetProfileId())
                .orElseThrow(() -> new IllegalArgumentException("상대방의 프로필을 찾을 수 없습니다."));
        Long targetUserId = targetProfile.getUser().getUserId();
        Long photoId = request.getPhotoId();

        boolean isTargetPhoto = profileImageRepository.existsByImageIdAndProfile(photoId, targetProfile);
        if(!isTargetPhoto){
            throw new IllegalArgumentException("해당 photoId는 대상자의 프로필 사진이 아닙니다.");
        }

        boolean alreadyUnlocked = profileExtraPhotoUnlockRepository
                .existsByRequesterUserIdAndTargetUserIdAndPhotoId(requesterUserId, targetUserId, photoId);
        if(alreadyUnlocked){
            throw new IllegalStateException("이미 잠금이 풀려있는 사진입니다.");
        }

        TingWallet tingWallet = tingWalletRepository.findByUserIdForUpdate(requesterUserId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(requesterUserId)));

        int cost = p.getTing().getCost().getViewExtraPhoto();
        if(tingWallet.getEventTing() >= cost){
            tingWallet.spendEventTing(cost);
        } else if (tingWallet.getTing() >= cost) {
            tingWallet.spendTing(cost);
        } else {
            throw new IllegalStateException("이벤트 팅과 팅이 부족합니다.");
        }

        profileExtraPhotoUnlockRepository.save(
                new ProfileExtraPhotoUnlock(requesterUserId, targetUserId, photoId)
        );

        int tingRemains = tingWallet.getTing();
        int eventTingRemains = tingWallet.getEventTing();

        return new UnlockTargetPhotoResponseDto(tingRemains, eventTingRemains);
    }

    @Transactional
    public void purchaseAdditionalProfileByTing(Long userId, PurchaseAdditionalProfileByTingRequestDto request){
        RuntimePolicySnapshot p = runtimePolicyService.snapshot();

        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저 정보를 찾을 수 없습니다."));
        TingWallet tingWallet = tingWalletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(userId)));

        int cost;
        int num = request.getAdditionalProfileNumByTing();
        if(num == 1){
            cost = p.getTing().getCost().getExtraProfile();
        } else if (num == 5) {
            cost = p.getTing().getCost().getExtraProfileBundle5();
        }else {
            throw new IllegalArgumentException("추가로 구매하는 프로필의 갯수가 1 또는 5가 아닙니다.");
        }

        if(tingWallet.getEventTing() >= cost){
            tingWallet.spendEventTing(cost);
        }else if(tingWallet.getTing() >= cost){
            tingWallet.spendTing(cost);
        }else {
            throw new IllegalStateException("팅이나 이벤트 팅이 부족합니다.");
        }
        tingWallet.addExtraProfileByTing(num);
    }

    @Transactional
    public GetReceivedScoreResponseDto getReceivedScore(Long userId, GetReceivedScoreRequestDto request) {
        RuntimePolicySnapshot p = runtimePolicyService.snapshot();

        Long targetProfileId = request.getTargetProfileId();
        Profile targetProfile = profileRepository.findById(targetProfileId)
                .orElseThrow(() -> new IllegalArgumentException("상대방 프로필을 찾을 수 없습니다."));
        Long targetUserId = targetProfile.getUser().getUserId();

        ProfileRating rating = profileRatingRepository.findByFromUserIdAndTargetUserId(targetUserId, userId)
                .orElse(null);

        if(rating == null){
            return new GetReceivedScoreResponseDto(-1);
        }

        int cost = p.getTing().getCost().getViewScore();
        TingWallet tingWallet = tingWalletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(userId)));
        if(tingWallet.getEventTing() >= cost){
            tingWallet.spendEventTing(cost);
        } else if (tingWallet.getTing() >= cost){
            tingWallet.spendTing(cost);
        } else {
            throw new IllegalStateException("팅 또는 이벤트 팅이 부족합니다.");
        }

        return new GetReceivedScoreResponseDto(rating.getScore());
    }

    @Transactional(readOnly = true)
    public CheckReceivedScoreResponseDto checkReceivedScore(Long userId, Long targetProfileId){
        Profile targetProfile = profileRepository.findById(targetProfileId)
                .orElseThrow(() -> new IllegalArgumentException("상대방 프로필을 찾을 수 없습니다."));
        Long targetUserId = targetProfile.getUser().getUserId();

        boolean received = profileRatingRepository.existsByFromUserIdAndTargetUserId(targetUserId, userId);

        if(received){
            return new CheckReceivedScoreResponseDto(true);
        } else {
            return new CheckReceivedScoreResponseDto(false);
        }
    }
}
