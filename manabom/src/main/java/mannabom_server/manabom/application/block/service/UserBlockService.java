package mannabom_server.manabom.application.block.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.block.dto.BlockedUserResponse;
import mannabom_server.manabom.application.messageRequest.service.MessageRequestService;
import mannabom_server.manabom.domain.block.entity.UserBlock;
import mannabom_server.manabom.domain.block.enums.UserBlockSource;
import mannabom_server.manabom.domain.block.repository.UserBlockRepository;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatRoomType;
import mannabom_server.manabom.domain.chat.enums.ChatStatus;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.likeRequest.enums.LikeStatus;
import mannabom_server.manabom.domain.likeRequest.repository.LikeRequestRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 회원 차단(1단계: 소개팅·1:1 대화). 미팅 단체방은 대상이 아니다.
 */
@Service
@RequiredArgsConstructor
public class UserBlockService {

    private static final List<ChatRoomType> ONE_TO_ONE_ROOM_TYPES =
            List.of(ChatRoomType.PROFILE_MATCH, ChatRoomType.LOVEVIEW_MATCH);

    private final UserBlockRepository userBlockRepository;
    private final ProfileRepository profileRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final LikeRequestRepository likeRequestRepository;
    private final MessageRequestService messageRequestService;

    @Transactional
    public void blockByProfile(Long blockerUserId, Long targetProfileId, UserBlockSource source) {
        block(blockerUserId, findUserIdByProfile(targetProfileId), source);
    }

    /**
     * 차단하고, 두 회원 사이의 1:1 대화방을 닫고, 대기 중인 호감·메시지 요청을 조용히 거절한다.
     * 이미 차단한 회원이면 아무것도 하지 않는다.
     */
    @Transactional
    public void block(Long blockerUserId, Long targetUserId, UserBlockSource source) {
        if (blockerUserId == null || targetUserId == null) {
            throw new IllegalArgumentException("차단할 회원 정보가 없습니다.");
        }
        if (userBlockRepository.findByBlockerUserIdAndBlockedUserId(blockerUserId, targetUserId).isPresent()) {
            return;
        }
        userBlockRepository.save(new UserBlock(blockerUserId, targetUserId, source));

        closeOneToOneRooms(blockerUserId, targetUserId);
        rejectPendingLike(blockerUserId, targetUserId);
        rejectPendingLike(targetUserId, blockerUserId);
        messageRequestService.rejectPendingForBlock(blockerUserId, targetUserId);
        messageRequestService.rejectPendingForBlock(targetUserId, blockerUserId);
    }

    /** 차단을 풀어도 이미 닫힌 대화방은 다시 열지 않는다. */
    @Transactional
    public void unblock(Long blockerUserId, Long targetProfileId) {
        Long targetUserId = findUserIdByProfile(targetProfileId);
        userBlockRepository.findByBlockerUserIdAndBlockedUserId(blockerUserId, targetUserId)
                .ifPresent(userBlockRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<BlockedUserResponse> getBlockedUsers(Long blockerUserId) {
        List<UserBlock> blocks = userBlockRepository.findAllByBlockerUserIdOrderByCreatedAtDesc(blockerUserId);
        if (blocks.isEmpty()) {
            return List.of();
        }
        Map<Long, Profile> profiles = profileRepository.findAllByUser_UserIdIn(
                        blocks.stream().map(UserBlock::getBlockedUserId).toList())
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getUserId(), Function.identity(), (a, b) -> a));

        return blocks.stream()
                .filter(block -> profiles.containsKey(block.getBlockedUserId()))
                .map(block -> {
                    Profile profile = profiles.get(block.getBlockedUserId());
                    return new BlockedUserResponse(profile.getProfileId(), profile.getNickName(), block.getCreatedAt());
                })
                .toList();
    }

    private Long findUserIdByProfile(Long profileId) {
        if (profileId == null) {
            throw new IllegalArgumentException("회원의 프로필 ID가 없습니다.");
        }
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("회원의 프로필을 찾을 수 없습니다."))
                .getUser()
                .getUserId();
    }

    private void closeOneToOneRooms(Long userId, Long otherUserId) {
        chatRoomRepository.findRoomsBetween(userId, otherUserId, ONE_TO_ONE_ROOM_TYPES, ChatStatus.ENABLED)
                .forEach(ChatRoom::deactivate);
    }

    private void rejectPendingLike(Long fromUserId, Long toUserId) {
        likeRequestRepository.findByFromUserIdAndToUserId(fromUserId, toUserId)
                .filter(like -> like.getStatus() == LikeStatus.PENDING)
                .ifPresent(like -> like.reject(null));
    }
}
