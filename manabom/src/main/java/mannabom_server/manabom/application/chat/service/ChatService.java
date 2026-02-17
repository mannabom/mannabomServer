package mannabom_server.manabom.application.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.dto.request.ChatSendRequest;
import mannabom_server.manabom.application.chat.dto.response.*;
import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.application.signup.service.S3FileUploadService;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.enums.SseEventName;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.ProfileImage;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ProfileRepository profileRepository;

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private final ChatMemberService chatMemberService;
    private final S3FileUploadService s3FileUploadService;
    private final NotificationService notificationService;

    private static final int PAGE_SIZE = 50;

    //채팅 보내기
    @Transactional
    public void sendMessage(ChatSendRequest request, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 입니다."));
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로필입니다."));

        List<ChatMember> members = chatMemberRepository.findAllByRoomIdAndStatus(request.getRoomId(), ChatMemberStatus.ACTIVATE);

        ChatMessage message = ChatMessage.builder()
                .room(chatRoom)
                .type(request.getMessageType())
                .content(request.getContent())
                .user(user)
                .build();
        chatMessageRepository.save(message);

        int initialUnreadCount = chatMemberRepository.countChatMemberByRoomIdAndStatus(chatRoom.getId(), ChatMemberStatus.ACTIVATE) - 1;
        stringRedisTemplate.opsForValue().set("unread:count:" + message.getId(), String.valueOf(initialUnreadCount));
        chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(chatRoom.getId(), userId,ChatMemberStatus.ACTIVATE)
                .ifPresent(member -> member.updateLastReadMessageId(message.getId()));

        ChatMessageEvent event = ChatMessageEvent.builder()
                .roomId(request.getRoomId())
                .sendAt(message.getCreatedAt())
                .senderUserId(userId)
                .content(request.getContent())
                .messageId(message.getId())
                .clientMessageId(request.getClientMessageId())
                .messageType(request.getMessageType().name())
                .unreadCount(initialUnreadCount)
                .build();
        simpMessagingTemplate.convertAndSend("/topic/rooms/" + request.getRoomId(), event);

        sendNotificationWithSSEOrPush(request, members, userId, profile.getNickName());

        log.debug("채팅 전송 완료: room={}, sender={}, msgId={}", chatRoom.getId(), userId, message.getId());

    }



    private void sendNotificationWithSSEOrPush(ChatSendRequest request, List<ChatMember> members, Long senderId, String senderNickname) {
        String displayContent = request.getMessageType().getDisplayMessage(request.getContent());

        Map<String, Object> notifyData = Map.of(
                "roomId", request.getRoomId(),
                "messageType", request.getMessageType().name()
        );

        for (ChatMember member : members) {
            Long targetUserId = member.getUser().getUserId();
            if (targetUserId.equals(senderId)) continue;

            String userLocation = stringRedisTemplate.opsForValue().get("user:location:" + targetUserId);
            if (!String.valueOf(request.getRoomId()).equals(userLocation)) {
                log.debug("유저 {} 는 방 밖에 있음. 알림 발송!", targetUserId);
                notificationService.sendNotification(targetUserId, SseEventName.NEW_CHAT_MESSAGE, senderNickname, displayContent, notifyData);
            }
        }
    }

    /**
     * 앱시작 시 동기화
     *
     */
    @Transactional(readOnly = true)
    public ChatInitialSyncResponse getInitialSync(Long userId) {
        List<ChatMember> myRooms = chatMemberRepository.findAllByUser_UserIdAndStatus(userId, ChatMemberStatus.ACTIVATE);

        int count = 0;
        boolean hasNewRoom = false;
        for (ChatMember m : myRooms) {
            if (m.getLastReadMessageId() == null)
                hasNewRoom = true;
            Long lastReadId = m.getLastReadMessageId() != null ? m.getLastReadMessageId() : 0L;
            count += chatMessageRepository.countUnreadMessages(m.getRoom().getId(), lastReadId);
        }
        return new ChatInitialSyncResponse(count, hasNewRoom);
    }

    /**
     * 채팅방 리스트 동기화
     *
     */
    @Transactional(readOnly = true)
    public List<ChatRoomListResponse> getChatRoomListSync(Long userId) {
        List<ChatMember> myRooms = chatMemberRepository.findAllByUser_UserIdAndStatus(userId, ChatMemberStatus.ACTIVATE);

        return myRooms.stream().map(m -> {
            ChatRoom room = m.getRoom();
            String roomName = generateRoomName(room, userId);
            ChatMessage lastMsg = chatMessageRepository.findTopByRoomIdOrderByIdDesc(room.getId()).orElse(null);
            int unreadCount = chatMessageRepository.countUnreadMessages(room.getId(), m.getLastReadMessageId() == null ? 0L : m.getLastReadMessageId());

            return ChatRoomListResponse.of(room, roomName, lastMsg, unreadCount);
        }).toList();
    }

    private String generateRoomName(ChatRoom room, Long userId) {
        switch (room.getType()) {
            case MEETING_GROUP -> {
                return room.getMeeting().getRoomName();
            }
            case MEETING_MATCH -> {
                MeetingMatch match = room.getMatch();
                return String.format("%s - %s", match.getMeeting1().getRoomName(), match.getMeeting2().getRoomName());
            }
            case PROFILE_MATCH, LOVEVIEW_MATCH -> {
                ChatMember opponent = chatMemberRepository.findByRoomId(room.getId()).stream()
                        .filter(chatMember -> !chatMember.getUser().getUserId().equals(userId))
                        .findFirst()
                        .orElse(null);

                if(opponent!=null){
                    return profileRepository.findByUser(opponent.getUser())
                            .map(Profile::getNickName)
                            .orElse("알 수 없는 사용자");
                }
            }

        }
        return "알 수 없는 사용자";
    }

    /**
     * 채팅메세지 동기화
     * 가장 최신 메시지 응답
     *
     */
    @Transactional(readOnly = true)
    public ChatSyncResponse getLatestChatMessageListSync(Long roomId, Long userId,Long lastMessageId) {
        ChatMember chatMember = chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(roomId, userId, ChatMemberStatus.ACTIVATE)
                .orElseThrow(()-> new IllegalArgumentException("해당 채팅방에 참여하고 있지 않습니다."));

       List<ChatMemberInfo> memberInfos = getActiveMemberInfos(roomId);

        Pageable limit = PageRequest.of(0, 100);
        List<ChatMessage> messages = chatMessageRepository.findLatestMessages(roomId,limit);

        if(messages.isEmpty()){
            return ChatSyncResponse.builder().roomId(roomId).messages(Collections.emptyList()).members(memberInfos).hasGap(false).build();
        }
        long oldestFetchedId = messages.get(messages.size()-1).getId();
        boolean hasGap = (lastMessageId != 0) && (oldestFetchedId > lastMessageId + 1);

        chatMemberService.updateReadStatus(roomId,userId, messages.get(0).getId());
        Collections.reverse(messages);
        return buildChatSyncResponse(roomId, messages, memberInfos, hasGap);
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(Long roomId, Long userId, Long firstMesssageId){
        chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(roomId, userId, ChatMemberStatus.ACTIVATE).orElseThrow(()-> new IllegalArgumentException("참여중이지 않는 채팅방 입니다."));
        Pageable limit = PageRequest.of(0, PAGE_SIZE+1);
        List<ChatMessage> messages = chatMessageRepository.findChatMessagesBefore(roomId, firstMesssageId, limit);

        if(messages.isEmpty()){
            return ChatHistoryResponse.builder()
                    .roomId(roomId)
                    .messages(Collections.emptyList())
                    .hasNext(false)
                    .build();
        }
        boolean hasNext = messages.size() > PAGE_SIZE;
        if(hasNext){
            messages = messages.subList(0, PAGE_SIZE);
        }

        Collections.reverse(messages);
        return buildChatHistoryResponse(roomId, messages, false);

    }

    private List<ChatMemberInfo> getActiveMemberInfos(Long roomId){
        List<Object[]> results = chatMemberRepository.findAllActiveMembersWithMainImage(roomId,ChatMemberStatus.ACTIVATE);
        return results.stream()
                .map(
                        result ->{
                            ChatMember cm = (ChatMember) result[0];
                            User u = (User) result[1];
                            Profile p = (Profile) result[2];
                            ProfileImage pi = (ProfileImage) result[3];

                            String imageUrl = (pi!=null)? pi.getUrl() : "default_image_url";
                            return ChatMemberInfo.of(p,imageUrl);
                        }
                ).toList();
    }
    private ChatSyncResponse buildChatSyncResponse(Long roomId, List<ChatMessage> messages, List<ChatMemberInfo> infos, boolean hasGap){
        List<ChatMessageResponse> responses = fetchUnreadCounts(messages);
        return ChatSyncResponse.builder()
                .roomId(roomId)
                .members(infos)
                .hasGap(hasGap)
                .messages(responses)
                .build();
    }
    private ChatHistoryResponse buildChatHistoryResponse(Long roomId, List<ChatMessage> messages, boolean hasNext){
        List<ChatMessageResponse> responses = fetchUnreadCounts(messages);
        return ChatHistoryResponse.builder()
                .roomId(roomId)
                .hasNext(hasNext)
                .messages(responses)
                .build();
    }
    private List<ChatMessageResponse> fetchUnreadCounts(List<ChatMessage> messages){
        List<String> keys = messages.stream().map(message-> "unread:count:" + message.getId())
                .toList();

        List<String> unreadCounts = stringRedisTemplate.opsForValue().multiGet(keys);

        List<ChatMessageResponse> responses = new ArrayList<>();
        for(int i =0; i< messages.size();i++){
            ChatMessage message = messages.get(i);
            String countStr = (unreadCounts!= null && unreadCounts.get(i)!=null) ? unreadCounts.get(i) : "0";
            int count = Integer.parseInt(countStr);
            responses.add(ChatMessageResponse.of(message,count));
        }
        return responses;
    }



}
