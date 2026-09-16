package mannabom_server.manabom.application.messageRequest.service;

import mannabom_server.manabom.application.chat.service.ChatRoomService;
import mannabom_server.manabom.application.currency.dto.response.CheckTingWalletResponseDto;
import mannabom_server.manabom.application.currency.service.TingTransactionRecorder;
import mannabom_server.manabom.application.messageRequest.dto.response.SendMessageResponseDto;
import mannabom_server.manabom.application.currency.service.TingWalletService;
import mannabom_server.manabom.application.pushService.service.pushSender.PushService;
import mannabom_server.manabom.application.signal.dto.response.RespondSignalResponseDto;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.matching.entity.LoveViewRecommendHistory;
import mannabom_server.manabom.domain.matching.enums.RecommendType;
import mannabom_server.manabom.domain.matching.repository.LoveViewRecommendHistoryRepository;
import mannabom_server.manabom.domain.matching.repository.ProfileRecommendHistoryRepository;
import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;
import mannabom_server.manabom.domain.messageRequest.repository.MessageRequestRepository;
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
class MessageRequestServiceTest {

    @Mock private MessageRequestRepository messageRequestRepository;
    @Mock private PushService pushService;
    @Mock private ProfileRepository profileRepository;
    @Mock private TingWalletRepository tingWalletRepository;
    @Mock private RuntimePolicyService runtimePolicyService;
    @Mock private TingWalletService tingWalletService;
    @Mock private ProfileRecommendHistoryRepository profileRecommendHistoryRepository;
    @Mock private LoveViewRecommendHistoryRepository loveViewRecommendHistoryRepository;
    @Mock private ChatRoomService chatRoomService;
    @Mock private TingTransactionRecorder tingTransactionRecorder;

    @InjectMocks
    private MessageRequestService messageRequestService;

    @Test
    void createsLoveViewChatRoomWhenMessageIsAccepted() {
        MessageRequest messageRequest = new MessageRequest(
                1L,
                2L,
                "안녕하세요",
                MessageSource.LOVE_VIEW_MATCH
        );
        LoveViewRecommendHistory history = new LoveViewRecommendHistory(
                1L,
                2L,
                RecommendType.FREE,
                LocalDateTime.now()
        );
        when(messageRequestRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(messageRequest));
        when(loveViewRecommendHistoryRepository
                .findTopByRequesterUserIdAndTargetUserIdOrderByRecommendedAtDesc(1L, 2L))
                .thenReturn(Optional.of(history));
        when(chatRoomService.createLoveViewChatRoom(history)).thenReturn(200L);

        RespondSignalResponseDto response = messageRequestService.respondMessageRequest(
                2L,
                20L,
                true,
                null
        );

        assertThat(messageRequest.getStatus()).isEqualTo(MessageRequestStatus.ACCEPTED);
        assertThat(response.isAccepted()).isTrue();
        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getChatRoomId()).isEqualTo(200L);
        verify(chatRoomService).createLoveViewChatRoom(history);
    }

    @Test
    void skipsTingChargeWhenMessageCostIsZero() {
        Profile toProfile = mock(Profile.class);
        User toUser = mock(User.class);
        when(profileRepository.findById(20L)).thenReturn(Optional.of(toProfile));
        when(toProfile.getUser()).thenReturn(toUser);
        when(toUser.getUserId()).thenReturn(2L);
        when(messageRequestRepository.findByFromUserIdAndToUserId(1L, 2L)).thenReturn(Optional.empty());
        when(runtimePolicyService.snapshot()).thenReturn(zeroMessageCostPolicy());
        TingWallet wallet = new TingWallet(1L);
        when(tingWalletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        when(messageRequestRepository.save(any(MessageRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tingWalletService.checkTingWallet(1L))
                .thenReturn(new CheckTingWalletResponseDto(0, 0, 0, 0, 0, 0, 0));

        SendMessageResponseDto response = messageRequestService.sendMessageRequest(
                1L,
                20L,
                "안녕하세요",
                MessageSource.LOVE_VIEW_MATCH
        );

        assertThat(response).isNotNull();
        assertThat(wallet.getTing()).isZero();
        assertThat(wallet.getEventTing()).isZero();
        verify(messageRequestRepository).save(any(MessageRequest.class));
        verifyNoInteractions(tingTransactionRecorder);
    }

    private RuntimePolicySnapshot zeroMessageCostPolicy() {
        return RuntimePolicySnapshot.builder()
                .ting(RuntimePolicySnapshot.Ting.builder()
                        .vipThreshold(100)
                        .cost(RuntimePolicySnapshot.Ting.Cost.builder()
                                .message(0)
                                .like(10)
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
