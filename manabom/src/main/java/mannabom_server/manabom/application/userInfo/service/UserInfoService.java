package mannabom_server.manabom.application.userInfo.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import mannabom_server.manabom.application.region.service.RegionService;

import mannabom_server.manabom.application.signup.service.S3FileUploadService;

import mannabom_server.manabom.application.userInfo.dto.common.ProfileDto;
import mannabom_server.manabom.application.userInfo.dto.request.PutUserInfoRequest;
import mannabom_server.manabom.application.userInfo.dto.response.CheckEntitlementsResponseDto;
import mannabom_server.manabom.application.userInfo.dto.common.UserAllPhotosDto;
import mannabom_server.manabom.application.userInfo.dto.response.GetUserInfoResponse;
import mannabom_server.manabom.application.userInfo.dto.response.GetUserMainPhotoResponseDto;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.question.entity.Question;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;
import mannabom_server.manabom.domain.question.repository.QuestionAnswerRepository;
import mannabom_server.manabom.domain.question.repository.QuestionRepository;
import mannabom_server.manabom.domain.region.entity.Region;
import mannabom_server.manabom.domain.university.entity.University;
import mannabom_server.manabom.domain.university.repository.UniversityRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.ProfileImage;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileImageRepository;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserInfoService {

    private final ProfileRepository profileRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final RegionService regionService;
    private final ProfileImageRepository profileImageRepository;
    private final S3FileUploadService s3FileUploadService;
    private final TingWalletRepository tingWalletRepository;
    private final RuntimePolicyService runtimePolicyService;


    public GetUserInfoResponse getUserInfo(Long userId){
        log.info("회원 정보 조회 서비스 계층 동작 시작");

        User user = userRepository.findById(userId).orElseThrow(()-> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Profile profile = profileRepository.findByUserWithRegionAndUniversity(user)
                .orElseThrow(()->new IllegalArgumentException("사용자의 프로필을 찾을 수 없습니다."));
        List<QuestionAnswer> questionAnswerList = questionAnswerRepository.findByProfileWithQuestion(profile);

        log.info("회원 정보 조회 서비스 계층 동작 완료");

        return new GetUserInfoResponse(ProfileDto.of(profile), questionAnswerList);
    }

    @Transactional
    public void putUserInfo(Long userId, PutUserInfoRequest request){
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Profile profile = profileRepository.findByUser(user).orElseThrow(() -> new IllegalArgumentException("해당 사용자의 프로필을 찾을 수 없습니다."));

        log.info("회원 정보 입력(프로필 수정) 서비스 계층 동작 시작");

        Region region = regionService.resolveRegion(request.getProfile().getRegion().getSido(),request.getProfile().getRegion().getSigungu());
        University university = universityRepository.findByName(request.getProfile().getUniversity())
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 대학교로 수정하실 수 없습니다."));

        if(request.getProfile() != null) {
            profile.setGender(request.getProfile().getGender());
            profile.setHeight(request.getProfile().getHeight());
            profile.setBodyType(request.getProfile().getBodyType());
            profile.setRegion(region);
            profile.setNickName(request.getProfile().getNickName());
            profile.setBirthDate(request.getProfile().getBirthDate());
            profile.setMbti(request.getProfile().getMbti());
            profile.setSmoking(request.getProfile().getSmoking());
            profile.setAlcohol(request.getProfile().getAlcohol());
            profile.setUniversity(university);
            profile.setEmail(request.getProfile().getEmail());

            profileRepository.save(profile);
        }

        log.info("회원 정보 입력(프로필 수정) 서비스 계층 동작 완료, 답변 업데이트 시작");

        // --- 기존 답변을 질문과 함께 한 번에 조회
        List<QuestionAnswer> existingList = questionAnswerRepository.findByProfileWithQuestion(profile);
        Map<Long, QuestionAnswer> existingByQid = existingList.stream()
                .collect(Collectors.toMap(qa -> qa.getQuestion().getQuestionId(), qa -> qa));

        // --- 요청 리스트(없으면 빈 리스트)
        List<QuestionAnswer> requested = Optional.ofNullable(request.getAnswers())
                .orElseGet(Collections::emptyList);

        // 요청된 questionId 집합(“요청에 없는 기존 답변은 유지” 정책이면 안 씀)
        Set<Long> handled = new HashSet<>();

        for (QuestionAnswer dto : requested) {
            Long questionId = dto.getQuestion().getQuestionId();
            String answer = dto.getAnswer();
            handled.add(questionId);

            QuestionAnswer existing = existingByQid.get(questionId);

            // 1) 비어있으면 삭제
            if (!StringUtils.hasText(answer)) {
                if (existing != null) {
                    questionAnswerRepository.delete(existing);
                }
                continue;
            }

            // 2) 존재하면 내용 변경
            if (existing != null) {
                if (!Objects.equals(existing.getAnswer(), answer)) {
                    existing.updateAnswer(answer);
                }
                continue;
            }

            // 3) 없으면 새로 생성
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 questionId: " + questionId));

            QuestionAnswer created = QuestionAnswer.builder()
                    .profile(profile)
                    .question(question)
                    .answer(answer)
                    .build();

            questionAnswerRepository.save(created);

            log.info("회원 정보 입력(답변 입력) 서비스 계층 동작 완료");

        }

    }

    @Transactional(readOnly = true)
    public GetUserMainPhotoResponseDto getUserMainPhoto(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 프로필을 찾을 수 없습니다."));

        ProfileImage mainImage = profileImageRepository.findByProfileAndIsMainTrue(profile)
                .orElseThrow(() -> new IllegalStateException("해당 사용자의 메인 프로필 사진을 찾을 수 없습니다."));

        String key = s3FileUploadService.extractS3KeyFromUrl(mainImage.getUrl());
        log.info("메인 프로필 사진 제공 완료, url : {}", mainImage.getUrl());

        return new GetUserMainPhotoResponseDto(s3FileUploadService.presignedGetUrl(key, Duration.ofMinutes(10)));
    }

    @Transactional(readOnly = true)
    public UserAllPhotosDto getUserAllPhotos(Long userId){
        Profile profile = profileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 프로필을 찾을 수 없습니다."));

        List<UserAllPhotosDto.Photo> photos = getUserAllPhotos(profile);

        return new UserAllPhotosDto(photos);
    }

    @Transactional
    public UserAllPhotosDto putUserPhoto(Long userId, MultipartFile photo){
        Profile profile = profileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 프로필을 찾을 수 없습니다."));
        List<ProfileImage> images = profileImageRepository.findAllByProfileForUpdate(profile);

        String url = s3FileUploadService.uploadFile(photo, "profiles");
        try {
            String fileName = extractFileNameFromS3Url(url);

            int nextIdx = images.stream()
                    .mapToInt(ProfileImage::getImageIndex)
                    .max()
                    .orElse(-1) + 1;

            boolean isMain = images.isEmpty();

            String originalName = "profile_"+ nextIdx;

            log.debug("image url : {}", url);

            profileImageRepository.save(new ProfileImage(profile, url, fileName, originalName, nextIdx, isMain));
            profileImageRepository.flush();

            List<UserAllPhotosDto.Photo> photos = getUserAllPhotos(profile);
            return new UserAllPhotosDto(photos);
        } catch (DataIntegrityViolationException e) {
            if(url != null)
                s3FileUploadService.deleteFile(url);
            throw new IllegalStateException("프로필 사진 저장 중 충돌이 발생했습니다.");
        } catch (RuntimeException e) {
            if(url != null)
                s3FileUploadService.deleteFile(url);
            throw e;
        }
    }

    @Transactional
    public UserAllPhotosDto deleteUserPhoto(Long userId, Long photoId){
        Profile profile = profileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 프로필을 찾을 수 없습니다."));

        List<ProfileImage> images = profileImageRepository.findAllByProfileForUpdate(profile);
        if(images == null || images.size() < 2){
            throw new IllegalStateException("사진은 2개 이상일 때만 삭제할 수 있습니다.");
        }

        images.sort(Comparator.comparingInt(ProfileImage::getImageIndex));

        ProfileImage target = null;
        for(ProfileImage image : images){
            if(image.getImageId().equals(photoId)){
                target = image;
                break;
            }
        }
        if(target == null)
            throw new IllegalStateException("해당 사용자 프로필 사진이 아닙니다.");

        if(!s3FileUploadService.deleteFile(target.getUrl())) {
            throw new IllegalStateException("프로필 사진 삭제에 실패했습니다");
        }
        profileImageRepository.delete(target);
        profileImageRepository.flush();

        profileImageRepository.decrementIndexesAfter(profile, target.getImageIndex());

        List<ProfileImage> remaining = profileImageRepository.findAllByProfile(profile);
        remaining.sort(Comparator.comparingInt(ProfileImage::getImageIndex));
        for (int i = 0; i < remaining.size(); i++) {
            if(i == 0) remaining.get(i).setAsMain();
            else remaining.get(i).unsetAsMain();
        }
        profileImageRepository.saveAll(remaining);
        profileImageRepository.flush();

        List<UserAllPhotosDto.Photo> photos = getUserAllPhotos(profile);

        return new UserAllPhotosDto(photos);
    }

    @Transactional
    public CheckEntitlementsResponseDto checkEntitlements(Long userId) {
        RuntimePolicySnapshot p = runtimePolicyService.snapshot();
        TingWallet tingWallet = tingWalletRepository.findByUserId(userId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(userId)));
        boolean isMembership = tingWallet.isMembershipActive(LocalDateTime.now());
        LocalDateTime membershipActiveUntil = tingWallet.getMembershipActiveUntil();
        boolean isVip = tingWallet.isVip(
                p.getTing().getVipThreshold(),
                p.getBenefit().getVip().getDailyExtraProfiles(),
                p.getBenefit().getVip().getDailyFreeMessages(),
                p.getBenefit().getVip().getDailyFreeLikes(),
                LocalDate.now()
        );

        return new CheckEntitlementsResponseDto(isMembership, membershipActiveUntil, isVip);
    }

    private List<UserAllPhotosDto.Photo> getUserAllPhotos(Profile profile){
        List<ProfileImage> images = profileImageRepository.findAllByProfile(profile);
        images.sort(Comparator.comparingInt(ProfileImage::getImageIndex));
        List<UserAllPhotosDto.Photo> photos = new ArrayList<>();
        for(ProfileImage image : images){
            Long photoId = image.getImageId();
            Integer photoIndex = image.getImageIndex();
            String key = s3FileUploadService.extractS3KeyFromUrl(image.getUrl());
            String presignedUrl = s3FileUploadService.presignedGetUrl(key, Duration.ofMinutes(10));

            photos.add(new UserAllPhotosDto.Photo(photoId, photoIndex, presignedUrl));
        }

        return photos;
    }

    /**
     * S3 URL에서 파일명 추출
     */
    private String extractFileNameFromS3Url(String s3Url) {
        if (s3Url == null) return null;
        int lastSlashIndex = s3Url.lastIndexOf('/');
        return lastSlashIndex != -1 ? s3Url.substring(lastSlashIndex + 1) : s3Url;
    }

    /**
     * 임시용, 출시 전 삭제해야함, 확인 필요, 삭제 예정, 지우기, 삭제삭제삭제
     * 혹시라도 이 매소드 쓰면 이거 지우고 사용중이라고 써두기!
     */
    @Transactional
    public void activeMembership(Long profileId){
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow();
        Long userId = profile.getUser().getUserId();
        RuntimePolicySnapshot p = runtimePolicyService.snapshot();
        TingWallet tingWallet = tingWalletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(userId)));
        tingWallet.activateMembership(
                LocalDateTime.now(),
                p.getBenefit().getMembership().getCycleExtraProfiles(),
                p.getBenefit().getMembership().getCycleFreeMessages(),
                p.getBenefit().getMembership().getCycleFreeLikes()
        );
    }

    /**
     * 임시용, 출시 전 삭제해야함, 확인 필요, 삭제 예정, 지우기, 삭제삭제삭제
     * 혹시라도 이 매소드 쓰면 이거 지우고 사용중이라고 써두기!
     */
    @Transactional
    public void addTing(int amount, Long targetProfileId){
        Profile targetProfile = profileRepository.findById(targetProfileId)
                .orElseThrow();
        Long targetUserId = targetProfile.getUser().getUserId();
        TingWallet tingWallet = tingWalletRepository.findByUserIdForUpdate(targetUserId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(targetUserId)));
        tingWallet.addTing(amount);
        log.info("[관리자 기능] 팅 지급 완료, 현재 팅 보유량(팅 : {}, 이벤트 팅 : {})", tingWallet.getTing(), tingWallet.getEventTing());
    }

}
