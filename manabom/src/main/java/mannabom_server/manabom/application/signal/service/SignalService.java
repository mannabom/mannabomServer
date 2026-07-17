package mannabom_server.manabom.application.signal.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.signal.dto.enums.MatchType;
import mannabom_server.manabom.application.signal.dto.enums.Type;
import mannabom_server.manabom.application.signal.dto.response.SignalFromMeProfileDto;
import mannabom_server.manabom.application.signal.dto.response.SignalFromMeResponseDto;
import mannabom_server.manabom.application.signal.dto.response.SignalToMeProfileDto;
import mannabom_server.manabom.application.signal.dto.response.SignalToMeResponseDto;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import mannabom_server.manabom.domain.likeRequest.entity.LikeRequest;
import mannabom_server.manabom.domain.likeRequest.enums.LikeSource;
import mannabom_server.manabom.domain.likeRequest.enums.LikeStatus;
import mannabom_server.manabom.domain.likeRequest.repository.LikeRequestRepository;
import mannabom_server.manabom.domain.matching.entity.ProfileRating;
import mannabom_server.manabom.domain.matching.repository.ProfileRatingRepository;
import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;
import mannabom_server.manabom.domain.messageRequest.repository.MessageRequestRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.ProfileImage;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileImageRepository;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignalService {
    private final LikeRequestRepository likeRequestRepository;
    private final MessageRequestRepository messageRequestRepository;
    private final ProfileRatingRepository profileRatingRepository;

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ProfileImageRepository profileImageRepository;
    private final FileStoragePort fileStoragePort;

    private static final int HIGH_SCORE_THRESHOLD = 4;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Transactional(readOnly = true)
    public SignalToMeResponseDto getSignalsToMe(Long userId){

        List<LikeRequest> likes = likeRequestRepository.findPendingSignalsToMeLast7Days(userId);
        List<MessageRequest> messages = messageRequestRepository.findPendingSignalsToMeLast7Days(userId);
        List<ProfileRating> highScores = profileRatingRepository.findHighScoresToMeLast7Days(userId, HIGH_SCORE_THRESHOLD);

        List<SignalToMeProfileDto> likeDtos = new ArrayList<>();
        for(LikeRequest like : likes){
            Long fromUserId = like.getFromUserId();

            User fromUser = userRepository.findById(fromUserId).orElse(null);
            if (fromUser == null) {
                continue;
            }

            Profile fromProfile = profileRepository.findByUser(fromUser).orElse(null);
            if (fromProfile == null) {
                continue;
            }

            if (like.getSource() == null) {
                throw new IllegalStateException("like source가 비어있습니다. likeId=" + like.getId());
            }

            MatchType matchType = (like.getSource() == LikeSource.LOVE_VIEW_MATCH)
                    ? MatchType.LOVE_VIEW : MatchType.PROFILE;

            String presignedUrl = (matchType == MatchType.LOVE_VIEW)
                    ? null : getPresignedImageUrl(fromProfile);

            likeDtos.add(
                    SignalToMeProfileDto.builder()
                            .id(like.getId())
                            .matchType(matchType)
                            .type(Type.LIKE)
                            .fromUserNickname(fromProfile.getNickName())
                            .fromUserImageUrl(presignedUrl)
                            .message(null)
                            .receivedAt(like.getCreatedAt())
                            .build()
            );
        }

        List<SignalToMeProfileDto> messageDtos = new ArrayList<>();
        for(MessageRequest message : messages){
            Long fromUserId = message.getFromUserId();

            User fromUser = userRepository.findById(fromUserId).orElse(null);
            if (fromUser == null) {
                continue;
            }

            Profile fromProfile = profileRepository.findByUser(fromUser).orElse(null);
            if (fromProfile == null) {
                continue;
            }

            if (message.getSource() == null) {
                throw new IllegalStateException("message source가 비어있습니다. messageId=" + message.getId());
            }

            MatchType matchType = (message.getSource() == MessageSource.LOVE_VIEW_MATCH)
                    ? MatchType.LOVE_VIEW : MatchType.PROFILE;

            String presignedUrl = (matchType == MatchType.LOVE_VIEW)
                    ? null : getPresignedImageUrl(fromProfile);

            messageDtos.add(
                    SignalToMeProfileDto.builder()
                            .id(message.getId())
                            .matchType(matchType)
                            .type(Type.MESSAGE)
                            .fromUserNickname(fromProfile.getNickName())
                            .fromUserImageUrl(presignedUrl)
                            .message(message.getMessage())
                            .receivedAt(message.getCreatedAt())
                            .build()
            );
        }

        List<SignalToMeProfileDto> highScoreDtos = new ArrayList<>();
        for(ProfileRating highScore : highScores){
            Long fromUserId = highScore.getFromUserId();

            User fromUser = userRepository.findById(fromUserId).orElse(null);
            if (fromUser == null) {
                continue;
            }

            Profile fromProfile = profileRepository.findByUser(fromUser).orElse(null);
            if (fromProfile == null) {
                continue;
            }

            String presignedUrl = getPresignedImageUrl(fromProfile);

            highScoreDtos.add(
                    SignalToMeProfileDto.builder()
                            .id(fromProfile.getProfileId())
                            .matchType(null)
                            .type(Type.HIGH_SCORE)
                            .fromUserNickname(fromProfile.getNickName())
                            .fromUserImageUrl(presignedUrl)
                            .message(null)
                            .receivedAt(toKstLocalDateTime(highScore.getCreatedAt()))
                            .build()
            );

        }

        likeDtos.sort((a, b) -> b.getReceivedAt().compareTo(a.getReceivedAt()));
        messageDtos.sort((a, b) -> b.getReceivedAt().compareTo(a.getReceivedAt()));
        highScoreDtos.sort((a, b) -> b.getReceivedAt().compareTo(a.getReceivedAt()));

        List<SignalToMeProfileDto> merged = new ArrayList<>(likeDtos.size() + messageDtos.size() + highScoreDtos.size());
        merged.addAll(likeDtos);
        merged.addAll(messageDtos);
        merged.addAll(highScoreDtos);

        return new SignalToMeResponseDto(merged);
    }

    @Transactional(readOnly = true)
    public SignalFromMeResponseDto getSignalsFromMe(Long userId){
        List<LikeRequest> likes = likeRequestRepository.findSignalsFromMeLast7Days(userId);
        List<MessageRequest> messages = messageRequestRepository.findSignalsFromMeLast7Days(userId);
        List<ProfileRating> highScores = profileRatingRepository.findHighScoresFromMeLast7Days(userId, HIGH_SCORE_THRESHOLD);

        List<SignalFromMeProfileDto> likeDtos = new ArrayList<>();
        for (LikeRequest like : likes) {

            Long toUserId = like.getToUserId();
            User toUser = userRepository.findById(toUserId).orElse(null);
            if (toUser == null) continue;

            Profile toProfile = profileRepository.findByUser(toUser).orElse(null);
            if (toProfile == null) continue;

            if (like.getSource() == null) {
                throw new IllegalStateException("like source가 비어있습니다. likeId=" + like.getId());
            }

            MatchType matchType = (like.getSource() == LikeSource.LOVE_VIEW_MATCH)
                    ? MatchType.LOVE_VIEW : MatchType.PROFILE;

            String presignedUrl = (matchType == MatchType.LOVE_VIEW)
                    ? null : getPresignedImageUrl(toProfile);

            likeDtos.add(
                    SignalFromMeProfileDto.builder()
                            .id(toProfile.getProfileId())
                            .requestId(like.getId())
                            .type(Type.LIKE)
                            .matchType(matchType)
                            .toUserNickname(toProfile.getNickName())
                            .toUserImageUrl(presignedUrl)
                            .message(null)
                            .status(like.getStatus().name())
                            .rejectReason((like.getStatus().equals(LikeStatus.REJECTED))? like.getRejectReason() : null)
                            .receivedAt(like.getCreatedAt())
                            .build()
            );
        }

        List<SignalFromMeProfileDto> messageDtos = new ArrayList<>();
        for (MessageRequest message : messages) {

            Long toUserId = message.getToUserId();
            User toUser = userRepository.findById(toUserId).orElse(null);
            if (toUser == null) continue;

            Profile toProfile = profileRepository.findByUser(toUser).orElse(null);
            if (toProfile == null) continue;

            if (message.getSource() == null) {
                throw new IllegalStateException("message source가 비어있습니다. messageId=" + message.getId());
            }

            MatchType matchType = (message.getSource() == MessageSource.LOVE_VIEW_MATCH)
                    ? MatchType.LOVE_VIEW : MatchType.PROFILE;

            String presignedUrl = (matchType == MatchType.LOVE_VIEW)
                    ? null : getPresignedImageUrl(toProfile);

            messageDtos.add(
                    SignalFromMeProfileDto.builder()
                            .id(toProfile.getProfileId())
                            .requestId(message.getId())
                            .type(Type.MESSAGE)
                            .matchType(matchType)
                            .toUserNickname(toProfile.getNickName())
                            .toUserImageUrl(presignedUrl)
                            .message((message.getMessage() != null)? message.getMessage() : null) // MESSAGE만 채움
                            .status(message.getStatus().name())
                            .rejectReason((message.getStatus().equals(MessageRequestStatus.REJECTED))? message.getRejectReason() : null)
                            .receivedAt(message.getCreatedAt())
                            .build()
            );
        }

        List<SignalFromMeProfileDto> highScoreDtos = new ArrayList<>();
        for (ProfileRating highScore : highScores) {

            Long targetUserId = highScore.getTargetUserId(); // 내가 준 점수의 대상(상대)
            User toUser = userRepository.findById(targetUserId).orElse(null);
            if (toUser == null) continue;

            Profile toProfile = profileRepository.findByUser(toUser).orElse(null);
            if (toProfile == null) continue;

            String presignedUrl = getPresignedImageUrl(toProfile);

            highScoreDtos.add(
                    SignalFromMeProfileDto.builder()
                            .id(toProfile.getProfileId())
                            .type(Type.HIGH_SCORE)
                            .matchType(null)
                            .toUserNickname(toProfile.getNickName())
                            .toUserImageUrl(presignedUrl)
                            .message(null)
                            .status(null)
                            .rejectReason(null)
                            .receivedAt(toKstLocalDateTime(highScore.getCreatedAt()))
                            .build()
            );
        }

        // 최신순 정렬
        likeDtos.sort((a, b) -> b.getReceivedAt().compareTo(a.getReceivedAt()));
        messageDtos.sort((a, b) -> b.getReceivedAt().compareTo(a.getReceivedAt()));
        highScoreDtos.sort((a, b) -> b.getReceivedAt().compareTo(a.getReceivedAt()));

        List<SignalFromMeProfileDto> merged = new ArrayList<>(likeDtos.size() + messageDtos.size() + highScoreDtos.size());
        merged.addAll(likeDtos);
        merged.addAll(messageDtos);
        merged.addAll(highScoreDtos);

        return new SignalFromMeResponseDto(merged);
    }

    private String getPresignedImageUrl(Profile profile){
        if (profile == null) return null;
        ProfileImage image = profileImageRepository.findByProfileAndIsMainTrue(profile)
                .orElse(null);
        if (image == null) return null;
        String imageUrl = image.getUrl();
        if (imageUrl == null || imageUrl.isBlank()) return null;
        String key = fileStoragePort.extractKeyFromUrl(imageUrl);
        if (key == null || key.isBlank()) return null;

        return fileStoragePort.presignedGetUrl(key, Duration.ofMinutes(10));
    }

    private LocalDateTime toKstLocalDateTime(Instant instant) {
        if (instant == null) return null;
        return LocalDateTime.ofInstant(instant, KST);
    }
}
