package mannabom_server.manabom.application.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.enums.ChatRoomType;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.matching.entity.LoveViewPhotoRequest;
import mannabom_server.manabom.domain.matching.entity.LoveViewRecommendHistory;
import mannabom_server.manabom.domain.matching.enums.LoveViewPhotoStatus;
import mannabom_server.manabom.domain.matching.enums.PhotoRequestStatus;
import mannabom_server.manabom.domain.matching.repository.LoveViewPhotoRequestRepository;
import mannabom_server.manabom.domain.meeting.enums.SseEventName;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhotoRequestService {
    private final ChatRoomRepository chatRoomRepository;
    private final LoveViewPhotoRequestRepository photoRequestRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatMemberRepository chatMemberRepository;

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationService notificationService;
    private final StringRedisTemplate stringRedisTemplate;

    //조회- 채팅방 입장
    public LoveViewPhotoStatus getPhotoRequestStatus(Long roomId, Long userId) {
        ChatRoom room = findRoomById(roomId);
        validateLoveViewChatRoom(room);
        validateActiveMember(roomId, userId);
        Long loveViewId = room.getLoveView().getId();

        Optional<LoveViewPhotoRequest> latestOpt = photoRequestRepository.findTopByHistoryIdOrderByIdDesc(loveViewId);
        if (latestOpt.isPresent()) {
            LoveViewPhotoRequest request = latestOpt.get();

            if (request.getStatus() == PhotoRequestStatus.ACCEPTED)
                return LoveViewPhotoStatus.ACCEPTED;

            if (request.getStatus() == PhotoRequestStatus.PENDING)
                return request.getSender().getUserId().equals(userId) ? LoveViewPhotoStatus.PENDING : LoveViewPhotoStatus.RECEIVED;

            if (request.getStatus() == PhotoRequestStatus.REJECTED) {
                int messageCount = chatMessageRepository.countChatMessagesByRoom_IdAndCreatedAtAfter(room.getId(), request.getUpdatedAt());
                return messageCount >= 10 ? LoveViewPhotoStatus.READY : LoveViewPhotoStatus.REJECTED;

            }
        }

        int messageCount = chatMessageRepository.countChatMessagesByRoom_Id(room.getId());
        return messageCount >= 10 ? LoveViewPhotoStatus.READY : LoveViewPhotoStatus.WAITING;
    }

    //요청
    @Transactional
    public void createPhotoRequest(Long roomId, Long userId) {
        User sender = findUserById(userId);
        ChatRoom room = findRoomById(roomId);
        validateLoveViewChatRoom(room);
        validateActiveMember(roomId, userId);

        LoveViewPhotoStatus currentStatus = getPhotoRequestStatus(roomId, userId);
        if (currentStatus != LoveViewPhotoStatus.READY) {
            throw new IllegalStateException("아직 프로필 사진을 요청할 수 있는 상태가 아닙니다.");
        }

        LoveViewRecommendHistory loveView = room.getLoveView();
        Long loveViewId = loveView.getId();

        Optional<LoveViewPhotoRequest> latestOpt = photoRequestRepository.findTopByHistoryIdOrderByIdDesc(loveViewId);
        if(latestOpt.isPresent()){
            if(latestOpt.get().getStatus()!=PhotoRequestStatus.REJECTED){
                throw new IllegalStateException("프로필 요청이 중복되었거나 이미 수락된 상태입니다.");
            }
        }
        User receiver = getOpponent(room.getId(), userId);

        LoveViewPhotoRequest request = LoveViewPhotoRequest.builder()
                .history(loveView)
                .sender(sender)
                .status(PhotoRequestStatus.PENDING)
                .receiver(receiver)
                .build();

        photoRequestRepository.save(request);
        sendNotification(roomId, receiver.getUserId(), "프로필 공개 요청",
                "상대방이 프로필을 몹시 궁금해하고 있어요!", SseEventName.PHOTO_REQUEST_RECEIVED, LoveViewPhotoStatus.RECEIVED);

    }

    //수락
    @Transactional
    public void acceptPhotoRequest(Long roomId, Long userId) {
        ChatRoom room = findRoomById(roomId);
        validateLoveViewChatRoom(room);
        validateActiveMember(roomId, userId);

        LoveViewPhotoRequest request = findPendingRequest(room.getLoveView().getId(), userId);
        request.accept();

        User opponent = getOpponent(roomId, userId);
        sendNotification(roomId, opponent.getUserId(),
                "프로필 열람 성공!", "상대방이 요청을 수락했어요! 지금 바로 확인해 보세요.",
                SseEventName.PHOTO_REQUEST_ACCEPTED, LoveViewPhotoStatus.ACCEPTED);    }

    //거절
    @Transactional
    public void rejectPhotoRequest(Long roomId, Long userId) {
        ChatRoom room = findRoomById(roomId);
        validateLoveViewChatRoom(room);
        validateActiveMember(roomId, userId);


        LoveViewPhotoRequest request = findPendingRequest(room.getLoveView().getId(), userId);
        request.reject();

        User opponent = getOpponent(roomId, userId);
        sendNotification(roomId, opponent.getUserId(),
                "다음에 다시 시도해 봐요!", "상대방이 아직은 사진 공개가 조금 부끄러운가 봐요.",
                SseEventName.PHOTO_REQUEST_REJECTED, LoveViewPhotoStatus.REJECTED);
    }
    private LoveViewPhotoRequest findPendingRequest(Long historyId, Long userId){
        LoveViewPhotoRequest request = photoRequestRepository.findTopByHistoryIdOrderByIdDesc(historyId)
                .orElseThrow(()-> new IllegalStateException("프로필 요청이 존재 하지 않습니다."));

        if(request.getStatus()!= PhotoRequestStatus.PENDING)
            throw new IllegalStateException("현재 대기 중인 요청이 없습니다.");
        if(!request.getReceiver().getUserId().equals(userId))
            throw new IllegalStateException("본인에게 온 요청만 수락/거절 할 수 있습니다.");

        return request;
    }
    private User findUserById(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 유저 입니다."));
    }


    private ChatRoom findRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId).orElseThrow(
                () -> new IllegalArgumentException("존재 하지 않는 채팅방 아이디 입니다.")
        );
    }

    private void validateLoveViewChatRoom(ChatRoom room) {
        if (room.getType() != ChatRoomType.LOVEVIEW_MATCH || room.getLoveView() == null)
            throw new IllegalStateException("유효한 연애코드 매칭 방이 아닙니다.");
    }

    private User getOpponent(Long chatRoomId, Long userId){
        return chatMemberRepository.findByRoomId(chatRoomId)
                .stream()
                .filter(m-> !m.getUser().getUserId().equals(userId))
                .filter(m-> m.getStatus()== ChatMemberStatus.ACTIVATE)
                .map(ChatMember::getUser)
                .findFirst()
                .orElseThrow(()-> new IllegalStateException("상대방이 채팅방을 나갔습니다."));
    }
    private void sendNotification(Long roomId, Long targetUserId, String title, String msg, SseEventName sseEventName, LoveViewPhotoStatus status){
        simpMessagingTemplate.convertAndSend("/topic/rooms/"+roomId, Map.of("status", status, "roomId", roomId));

        String location = stringRedisTemplate.opsForValue().get("user:location:"+targetUserId);
        if(!String.valueOf(roomId).equals(location)){

            notificationService.sendNotification(targetUserId,sseEventName,title, msg,Map.of("roomId",roomId));
        }
    }
    private void validateActiveMember(Long roomId, Long userId) {
        chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(
                roomId, userId, ChatMemberStatus.ACTIVATE
        ).orElseThrow(() -> new IllegalStateException("해당 채팅방의 활성 멤버만 접근할 수 있습니다."));
    }
}
