package mannabom_server.manabom.application.signal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import mannabom_server.manabom.application.signal.dto.enums.Type;
import mannabom_server.manabom.application.signal.dto.response.SignalFromMeProfileDto;
import mannabom_server.manabom.application.signal.dto.response.SignalFromMeResponseDto;
import mannabom_server.manabom.application.signal.dto.response.SignalToMeProfileDto;
import mannabom_server.manabom.application.signal.dto.response.SignalToMeResponseDto;
import mannabom_server.manabom.domain.likeRequest.entity.LikeRequest;
import mannabom_server.manabom.domain.likeRequest.enums.LikeSource;
import mannabom_server.manabom.domain.likeRequest.repository.LikeRequestRepository;
import mannabom_server.manabom.domain.matching.repository.ProfileRatingRepository;
import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;
import mannabom_server.manabom.domain.messageRequest.repository.MessageRequestRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileImageRepository;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignalServiceTest {

    @Mock private LikeRequestRepository likeRequestRepository;
    @Mock private MessageRequestRepository messageRequestRepository;
    @Mock private ProfileRatingRepository profileRatingRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private ProfileImageRepository profileImageRepository;
    @Mock private FileStoragePort fileStoragePort;

    @InjectMocks
    private SignalService signalService;

    @Test
    void returnsTargetProfileIdAndRequestIdForSentLikeAndMessage() {
        long requesterUserId = 1L;
        User likedUser = User.builder().userId(2L).userName("호감 상대").build();
        User messagedUser = User.builder().userId(3L).userName("메시지 상대").build();
        Profile likedProfile = profile(201L, likedUser, "호감상대");
        Profile messagedProfile = profile(301L, messagedUser, "메시지상대");

        LikeRequest likeRequest = new LikeRequest(requesterUserId, 2L, LikeSource.LOVE_VIEW_MATCH);
        ReflectionTestUtils.setField(likeRequest, "id", 101L);
        MessageRequest messageRequest = new MessageRequest(
                requesterUserId,
                3L,
                "안녕하세요",
                MessageSource.LOVE_VIEW_MATCH
        );
        ReflectionTestUtils.setField(messageRequest, "id", 102L);

        when(likeRequestRepository.findSignalsFromMeLast7Days(requesterUserId))
                .thenReturn(List.of(likeRequest));
        when(messageRequestRepository.findSignalsFromMeLast7Days(requesterUserId))
                .thenReturn(List.of(messageRequest));
        when(profileRatingRepository.findHighScoresFromMeLast7Days(requesterUserId, 4))
                .thenReturn(List.of());
        when(userRepository.findById(2L)).thenReturn(Optional.of(likedUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(messagedUser));
        when(profileRepository.findByUser(likedUser)).thenReturn(Optional.of(likedProfile));
        when(profileRepository.findByUser(messagedUser)).thenReturn(Optional.of(messagedProfile));

        SignalFromMeResponseDto response = signalService.getSignalsFromMe(requesterUserId);

        assertThat(response.getProfiles())
                .extracting(
                        SignalFromMeProfileDto::getType,
                        SignalFromMeProfileDto::getTargetProfileId,
                        SignalFromMeProfileDto::getRequestId
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(Type.LIKE, 201L, 101L),
                        org.assertj.core.groups.Tuple.tuple(Type.MESSAGE, 301L, 102L)
                );
    }

    @Test
    void returnsTargetProfileIdAndRequestIdForReceivedLikeAndMessage() {
        long receiverUserId = 1L;
        User likedUser = User.builder().userId(2L).userName("호감 보낸 상대").build();
        User messagedUser = User.builder().userId(3L).userName("메시지 보낸 상대").build();
        Profile likedProfile = profile(201L, likedUser, "호감보낸상대");
        Profile messagedProfile = profile(301L, messagedUser, "메시지보낸상대");

        LikeRequest likeRequest = new LikeRequest(2L, receiverUserId, LikeSource.LOVE_VIEW_MATCH);
        ReflectionTestUtils.setField(likeRequest, "id", 103L);
        MessageRequest messageRequest = new MessageRequest(
                3L,
                receiverUserId,
                "안녕하세요",
                MessageSource.LOVE_VIEW_MATCH
        );
        ReflectionTestUtils.setField(messageRequest, "id", 104L);

        when(likeRequestRepository.findPendingSignalsToMeLast7Days(receiverUserId))
                .thenReturn(List.of(likeRequest));
        when(messageRequestRepository.findPendingSignalsToMeLast7Days(receiverUserId))
                .thenReturn(List.of(messageRequest));
        when(profileRatingRepository.findHighScoresToMeLast7Days(receiverUserId, 4))
                .thenReturn(List.of());
        when(userRepository.findById(2L)).thenReturn(Optional.of(likedUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(messagedUser));
        when(profileRepository.findByUser(likedUser)).thenReturn(Optional.of(likedProfile));
        when(profileRepository.findByUser(messagedUser)).thenReturn(Optional.of(messagedProfile));

        SignalToMeResponseDto response = signalService.getSignalsToMe(receiverUserId);

        assertThat(response.getProfiles())
                .extracting(
                        SignalToMeProfileDto::getType,
                        SignalToMeProfileDto::getTargetProfileId,
                        SignalToMeProfileDto::getRequestId
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(Type.LIKE, 201L, 103L),
                        org.assertj.core.groups.Tuple.tuple(Type.MESSAGE, 301L, 104L)
                );
    }

    @Test
    void omitsRequestIdFromSentAndReceivedHighScoreResponse() throws JsonProcessingException {
        SignalFromMeProfileDto sentHighScore = SignalFromMeProfileDto.builder()
                .targetProfileId(401L)
                .type(Type.HIGH_SCORE)
                .build();
        SignalToMeProfileDto receivedHighScore = SignalToMeProfileDto.builder()
                .targetProfileId(402L)
                .type(Type.HIGH_SCORE)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        String sentJson = objectMapper.writeValueAsString(sentHighScore);
        String receivedJson = objectMapper.writeValueAsString(receivedHighScore);

        assertThat(sentJson).doesNotContain("requestId");
        assertThat(receivedJson).doesNotContain("requestId");
    }

    private Profile profile(Long profileId, User user, String nickname) {
        Profile profile = Profile.builder()
                .user(user)
                .nickName(nickname)
                .build();
        profile.setProfileId(profileId);
        return profile;
    }
}
