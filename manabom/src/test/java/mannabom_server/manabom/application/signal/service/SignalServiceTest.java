package mannabom_server.manabom.application.signal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import mannabom_server.manabom.application.signal.dto.enums.Type;
import mannabom_server.manabom.application.signal.dto.response.SignalFromMeProfileDto;
import mannabom_server.manabom.application.signal.dto.response.SignalFromMeResponseDto;
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
                        SignalFromMeProfileDto::getId,
                        SignalFromMeProfileDto::getRequestId
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(Type.LIKE, 201L, 101L),
                        org.assertj.core.groups.Tuple.tuple(Type.MESSAGE, 301L, 102L)
                );
    }

    @Test
    void omitsRequestIdFromHighScoreResponse() throws JsonProcessingException {
        SignalFromMeProfileDto highScore = SignalFromMeProfileDto.builder()
                .id(401L)
                .type(Type.HIGH_SCORE)
                .build();

        String json = new ObjectMapper().writeValueAsString(highScore);

        assertThat(json).doesNotContain("requestId");
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
