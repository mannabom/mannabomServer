package mannabom_server.manabom.application.like.service;

import mannabom_server.manabom.application.chat.service.ChatRoomService;
import mannabom_server.manabom.application.currency.service.TingWalletService;
import mannabom_server.manabom.application.pushService.service.pushSender.PushService;
import mannabom_server.manabom.application.signal.dto.response.RespondSignalResponseDto;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.likeRequest.entity.LikeRequest;
import mannabom_server.manabom.domain.likeRequest.enums.LikeSource;
import mannabom_server.manabom.domain.likeRequest.enums.LikeStatus;
import mannabom_server.manabom.domain.likeRequest.repository.LikeRequestRepository;
import mannabom_server.manabom.domain.matching.entity.ProfileRecommendHistory;
import mannabom_server.manabom.domain.matching.enums.RecommendType;
import mannabom_server.manabom.domain.matching.repository.LoveViewRecommendHistoryRepository;
import mannabom_server.manabom.domain.matching.repository.ProfileRecommendHistoryRepository;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock private LikeRequestRepository likeRequestRepository;
    @Mock private TingWalletRepository tingWalletRepository;
    @Mock private PushService pushService;
    @Mock private RuntimePolicyService runtimePolicyService;
    @Mock private TingWalletService tingWalletService;
    @Mock private ProfileRepository profileRepository;
    @Mock private ProfileRecommendHistoryRepository profileRecommendHistoryRepository;
    @Mock private LoveViewRecommendHistoryRepository loveViewRecommendHistoryRepository;
    @Mock private ChatRoomService chatRoomService;

    @InjectMocks
    private LikeService likeService;

    @Test
    void createsProfileChatRoomWhenLikeIsAccepted() {
        LikeRequest likeRequest = new LikeRequest(1L, 2L, LikeSource.PROFILE_MATCH);
        ProfileRecommendHistory history = new ProfileRecommendHistory(
                1L,
                2L,
                RecommendType.FREE,
                LocalDateTime.now()
        );
        when(likeRequestRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(likeRequest));
        when(profileRecommendHistoryRepository
                .findTopByRequesterUserIdAndTargetUserIdOrderByRecommendedAtDesc(1L, 2L))
                .thenReturn(Optional.of(history));
        when(chatRoomService.createProfileChatRoom(history)).thenReturn(100L);

        RespondSignalResponseDto response = likeService.respondLike(2L, 10L, true, null);

        assertThat(likeRequest.getStatus()).isEqualTo(LikeStatus.ACCEPTED);
        assertThat(response.isAccepted()).isTrue();
        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getChatRoomId()).isEqualTo(100L);
        verify(chatRoomService).createProfileChatRoom(history);
    }
}
