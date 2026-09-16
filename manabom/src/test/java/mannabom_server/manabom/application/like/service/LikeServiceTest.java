package mannabom_server.manabom.application.like.service;

import mannabom_server.manabom.application.chat.service.ChatRoomService;
import mannabom_server.manabom.application.currency.dto.response.CheckTingWalletResponseDto;
import mannabom_server.manabom.application.currency.service.TingTransactionRecorder;
import mannabom_server.manabom.application.like.dto.response.SendLikeResponseDto;
import mannabom_server.manabom.application.currency.service.TingWalletService;
import mannabom_server.manabom.application.pushService.service.pushSender.PushService;
import mannabom_server.manabom.application.signal.dto.response.RespondSignalResponseDto;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.likeRequest.entity.LikeRequest;
import mannabom_server.manabom.domain.likeRequest.enums.LikeSource;
import mannabom_server.manabom.domain.likeRequest.enums.LikeStatus;
import mannabom_server.manabom.domain.likeRequest.repository.LikeRequestRepository;
import mannabom_server.manabom.domain.matching.entity.ProfileRecommendHistory;
import mannabom_server.manabom.domain.matching.enums.RecommendType;
import mannabom_server.manabom.domain.matching.repository.LoveViewRecommendHistoryRepository;
import mannabom_server.manabom.domain.matching.repository.ProfileRecommendHistoryRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock private TingTransactionRecorder tingTransactionRecorder;

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

    @Test
    void skipsTingChargeWhenLikeCostIsZero() {
        Profile toProfile = mock(Profile.class);
        User toUser = mock(User.class);
        when(profileRepository.findById(20L)).thenReturn(Optional.of(toProfile));
        when(toProfile.getUser()).thenReturn(toUser);
        when(toUser.getUserId()).thenReturn(2L);
        when(runtimePolicyService.snapshot()).thenReturn(zeroLikeCostPolicy());
        TingWallet wallet = new TingWallet(1L);
        when(tingWalletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        when(likeRequestRepository.findByFromUserIdAndToUserId(1L, 2L)).thenReturn(Optional.empty());
        when(likeRequestRepository.save(any(LikeRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tingWalletService.checkTingWallet(1L))
                .thenReturn(new CheckTingWalletResponseDto(0, 0, 0, 0, 0, 0, 0));

        SendLikeResponseDto response = likeService.sendLike(1L, 20L, LikeSource.PROFILE_MATCH);

        assertThat(response).isNotNull();
        assertThat(wallet.getTing()).isZero();
        assertThat(wallet.getEventTing()).isZero();
        verify(likeRequestRepository).save(any(LikeRequest.class));
        verifyNoInteractions(tingTransactionRecorder);
    }

    private RuntimePolicySnapshot zeroLikeCostPolicy() {
        return RuntimePolicySnapshot.builder()
                .ting(RuntimePolicySnapshot.Ting.builder()
                        .vipThreshold(100)
                        .cost(RuntimePolicySnapshot.Ting.Cost.builder()
                                .like(0)
                                .message(10)
                                .build())
                        .build())
                .benefit(RuntimePolicySnapshot.Benefit.builder()
                        .vip(RuntimePolicySnapshot.Benefit.Vip.builder()
                                .dailyExtraProfiles(0)
                                .dailyFreeMessages(0)
                                .dailyFreeLikes(0)
                                .build())
                        .build())
                .build();
    }
}
