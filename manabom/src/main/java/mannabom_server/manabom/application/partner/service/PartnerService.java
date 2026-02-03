package mannabom_server.manabom.application.partner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.partner.dto.request.GetTargetProfileDetailRequestDto;
import mannabom_server.manabom.application.partner.dto.response.GetTargetProfileDetailResponseDto;
import mannabom_server.manabom.application.signup.service.S3FileUploadService;
import mannabom_server.manabom.domain.likeRequest.entity.LikeRequest;
import mannabom_server.manabom.domain.likeRequest.enums.LikeStatus;
import mannabom_server.manabom.domain.likeRequest.repository.LikeRequestRepository;
import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;
import mannabom_server.manabom.domain.messageRequest.repository.MessageRequestRepository;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;
import mannabom_server.manabom.domain.question.repository.QuestionAnswerRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.ProfileImage;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileImageRepository;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        List<GetTargetProfileDetailResponseDto.Photo> photos = new ArrayList<>();

        for(int i = 0; i < targetProfileImages.size(); i++){
            Long photoId = targetProfileImages.get(i).getImageId();
            String photoUrl = targetProfileImages.get(i).getUrl();
            String photoKey = s3FileUploadService.extractS3KeyFromUrl(photoUrl);
            String presignedPhotoUrl = s3FileUploadService.presignedGetUrl(photoKey, Duration.ofMinutes(10));

            if(requesterProfileImageNum <= i) {
                photos.add(new GetTargetProfileDetailResponseDto.Photo(photoId, presignedPhotoUrl, true));
            }else {
                photos.add(new GetTargetProfileDetailResponseDto.Photo(photoId, presignedPhotoUrl, false));
            }
        }

        List<QuestionAnswer> questionAnswers = questionAnswerRepository.findByProfileWithQuestion(targetProfile);

        int age = LocalDate.now().getYear() - targetProfile.getBirthDate().getYear() + 1; // 2026 - 2002 + 1 = 25
        String region = targetProfile.getRegionSido() + " " + targetProfile.getRegionSigungu();

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
                .region(region)
                .questionAnswers(questionAnswers)
                .photos(photos)
                .smoking(targetProfile.getSmoking())
                .drinking(targetProfile.getAlcohol())
                .liked(new GetTargetProfileDetailResponseDto.Liked(likeRequestExists, likeStatus))
                .messaged(new GetTargetProfileDetailResponseDto.Messaged(messageRequestExists, messageRequestStatus))
                .build();
    }
}
