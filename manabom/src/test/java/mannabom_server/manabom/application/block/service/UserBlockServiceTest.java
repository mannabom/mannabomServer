package mannabom_server.manabom.application.block.service;

import mannabom_server.manabom.application.messageRequest.service.MessageRequestService;
import mannabom_server.manabom.domain.block.entity.UserBlock;
import mannabom_server.manabom.domain.block.enums.UserBlockSource;
import mannabom_server.manabom.domain.block.repository.UserBlockRepository;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatStatus;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.likeRequest.entity.LikeRequest;
import mannabom_server.manabom.domain.likeRequest.enums.LikeSource;
import mannabom_server.manabom.domain.likeRequest.enums.LikeStatus;
import mannabom_server.manabom.domain.likeRequest.repository.LikeRequestRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceTest {

    @Mock private UserBlockRepository userBlockRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private LikeRequestRepository likeRequestRepository;
    @Mock private MessageRequestService messageRequestService;
    @InjectMocks private UserBlockService userBlockService;

    @Test
    void blockClosesOneToOneRoomsAndRejectsPendingRequestsInBothDirections() {
        ChatRoom room = mock(ChatRoom.class);
        LikeRequest pendingLikeToMe = new LikeRequest(2L, 1L, LikeSource.PROFILE_MATCH);
        when(userBlockRepository.findByBlockerUserIdAndBlockedUserId(1L, 2L)).thenReturn(Optional.empty());
        when(chatRoomRepository.findRoomsBetween(eq(1L), eq(2L), anyCollection(), eq(ChatStatus.ENABLED)))
                .thenReturn(List.of(room));
        when(likeRequestRepository.findByFromUserIdAndToUserId(1L, 2L)).thenReturn(Optional.empty());
        when(likeRequestRepository.findByFromUserIdAndToUserId(2L, 1L)).thenReturn(Optional.of(pendingLikeToMe));

        userBlockService.block(1L, 2L, UserBlockSource.DIRECT);

        ArgumentCaptor<UserBlock> saved = ArgumentCaptor.forClass(UserBlock.class);
        verify(userBlockRepository).save(saved.capture());
        assertThat(saved.getValue().getBlockerUserId()).isEqualTo(1L);
        assertThat(saved.getValue().getBlockedUserId()).isEqualTo(2L);
        verify(room).deactivate();
        assertThat(pendingLikeToMe.getStatus()).isEqualTo(LikeStatus.REJECTED);
        verify(messageRequestService).rejectPendingForBlock(1L, 2L);
        verify(messageRequestService).rejectPendingForBlock(2L, 1L);
    }

    @Test
    void blockingTwiceDoesNothing() {
        when(userBlockRepository.findByBlockerUserIdAndBlockedUserId(1L, 2L))
                .thenReturn(Optional.of(new UserBlock(1L, 2L, UserBlockSource.DIRECT)));

        userBlockService.block(1L, 2L, UserBlockSource.REPORT);

        verify(userBlockRepository, never()).save(any());
        verifyNoInteractions(chatRoomRepository, likeRequestRepository, messageRequestService);
    }

    @Test
    void cannotBlockSelf() {
        when(userBlockRepository.findByBlockerUserIdAndBlockedUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userBlockService.block(1L, 1L, UserBlockSource.DIRECT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unblockDeletesOnlyMyBlock() {
        Profile profile = mock(Profile.class);
        User user = mock(User.class);
        when(profileRepository.findById(20L)).thenReturn(Optional.of(profile));
        when(profile.getUser()).thenReturn(user);
        when(user.getUserId()).thenReturn(2L);
        UserBlock block = new UserBlock(1L, 2L, UserBlockSource.DIRECT);
        when(userBlockRepository.findByBlockerUserIdAndBlockedUserId(1L, 2L)).thenReturn(Optional.of(block));

        userBlockService.unblock(1L, 20L);

        verify(userBlockRepository).delete(block);
    }

    @Test
    void blockedListShowsProfilesOfMembersIBlocked() {
        Profile profile = mock(Profile.class);
        User user = mock(User.class);
        when(profile.getUser()).thenReturn(user);
        when(user.getUserId()).thenReturn(2L);
        when(profile.getProfileId()).thenReturn(20L);
        when(profile.getNickName()).thenReturn("봄이");
        when(userBlockRepository.findAllByBlockerUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(new UserBlock(1L, 2L, UserBlockSource.DIRECT)));
        when(profileRepository.findAllByUser_UserIdIn(List.of(2L))).thenReturn(List.of(profile));

        var result = userBlockService.getBlockedUsers(1L);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.profileId()).isEqualTo(20L);
            assertThat(item.nickName()).isEqualTo("봄이");
        });
    }
}
