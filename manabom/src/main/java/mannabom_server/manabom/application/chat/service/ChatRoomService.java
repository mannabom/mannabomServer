package mannabom_server.manabom.application.chat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.dto.event.ChatRoomLeaveEvent;
import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageType;
import mannabom_server.manabom.application.meeting.dto.response.MatchedChatRoomInfo;
import mannabom_server.manabom.application.meeting.service.MeetingMatchingService;
import mannabom_server.manabom.application.meeting.service.MeetingMemberService;
import mannabom_server.manabom.application.meeting.service.MeetingService;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.enums.ChatStatus;
import mannabom_server.manabom.domain.chat.repository.ChatMemberQueryRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.matching.entity.LoveViewRecommendHistory;
import mannabom_server.manabom.domain.matching.entity.ProfileRecommendHistory;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMemberQueryRepository chatMemberQueryRepository;
    private final UserRepository userRepository;
    private final MeetingMemberService meetingMemberService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long createMeetingChatRoom(Meeting meeting,User user){
        ChatRoom chatRoom = ChatRoom.createMeetingChatRoom(meeting);
        chatRoomRepository.save(chatRoom);
        ChatMember chatMember = ChatMember.create(chatRoom,user);
        chatMemberRepository.save(chatMember);

        return chatRoom.getId();
    }

    @Transactional
    public MatchedChatRoomInfo createMatchingChatRoom(MeetingMatch match){
        return createMatchingChatRoom(match, null);
    }

    @Transactional
    public MatchedChatRoomInfo createMatchingChatRoom(MeetingMatch match, Long actorUserId){
        ChatRoom chatRoom = ChatRoom.createMatchingChatRoom(match);
        chatRoomRepository.save(chatRoom);

        Meeting meeting1 = match.getMeeting1();
        Meeting meeting2 = match.getMeeting2();

        List<ChatMember> newChatMembers = new ArrayList<>();
        for (MeetingMember m: meetingMemberService.getActiveMembers(meeting1.getId()))
            newChatMembers.add(ChatMember.create(chatRoom,m.getUser()));

        for (MeetingMember m:  meetingMemberService.getActiveMembers(meeting2.getId()))
            newChatMembers.add(ChatMember.create(chatRoom,m.getUser()));

        chatMemberRepository.saveAll(newChatMembers);
        publishRoomCreated(chatRoom, actorUserId);

        List<MatchedChatRoomInfo.Participant> participants = chatMemberQueryRepository.findParticipantsByRoomId(chatRoom.getId());
        return MatchedChatRoomInfo.of(chatRoom.getId(),participants);
    }

    /**
    *프로필 매칭 채팅방 생성
     * */
    @Transactional
    public Long createProfileChatRoom(ProfileRecommendHistory profileHistory){
        return createProfileChatRoom(profileHistory, null);
    }

    @Transactional
    public Long createProfileChatRoom(ProfileRecommendHistory profileHistory, Long actorUserId){
        ChatRoom chatRoom = ChatRoom.createProfileChatRoom(profileHistory);
        chatRoomRepository.save(chatRoom);

        setOneToOneChatMember(chatRoom, profileHistory.getRequesterUserId(),profileHistory.getTargetUserId());
        publishRoomCreated(chatRoom, actorUserId);

        return chatRoom.getId();
    }
    /**
     *연애코드 매칭 채팅방 생성
     * */
    @Transactional
    public Long createLoveViewChatRoom(LoveViewRecommendHistory loveViewHistory){
        return createLoveViewChatRoom(loveViewHistory, null);
    }

    @Transactional
    public Long createLoveViewChatRoom(LoveViewRecommendHistory loveViewHistory, Long actorUserId){
        ChatRoom chatRoom = ChatRoom.createLoveviewChatRoom(loveViewHistory);
        chatRoomRepository.save(chatRoom);

        setOneToOneChatMember(chatRoom, loveViewHistory.getRequesterUserId(),loveViewHistory.getTargetUserId());
        publishRoomCreated(chatRoom, actorUserId);

        return chatRoom.getId();
    }
    private void publishRoomCreated(ChatRoom room, Long actorUserId){
        eventPublisher.publishEvent(ChatSystemMessageEvent.of(
                room.getId(),
                SystemMessageType.CHAT_ROOM_CREATED,
                actorUserId,
                null,
                Map.of("roomType", room.getType().name())
        ));
    }

    private void setOneToOneChatMember(ChatRoom chatRoom, Long user1Id, Long user2Id){
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(()->new IllegalArgumentException("일대일 매칭 채팅방 생성: 존재하지 않는 requesterId 입니다. "+ user1Id));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(()->new IllegalArgumentException("일대일 매칭 채팅방 생성: 존재하지 않는 targetUserId 입니다. "+ user2Id));
        List<ChatMember> chatMembers = List.of(
                ChatMember.create(chatRoom, user1),
                ChatMember.create(chatRoom, user2)
        );

        chatMemberRepository.saveAll(chatMembers);
    }
    @Transactional
    public Long joinChatRoom(Meeting meeting, User user){
        ChatRoom room = chatRoomRepository.findByMeeting(meeting)
                .orElseThrow(()-> new IllegalArgumentException("미팅id와 연결된 채팅방: 존재하지 않은 채팅방입니다."));
        ChatMember chatMember = ChatMember.create(room,user);
        chatMemberRepository.save(chatMember);

        return room.getId();
    }

    @Transactional
    public Long joinMatchingChatRoom(MeetingMatch match, User user) {
        ChatRoom room = chatRoomRepository.findByMatch(match)
                .orElseThrow(() -> new IllegalArgumentException(
                        "매칭 정보와 연결된 남녀 채팅방이 존재하지 않습니다."
                ));

        if (chatMemberRepository.existsByRoomIdAndUser_UserIdAndStatus(
                room.getId(),
                user.getUserId(),
                ChatMemberStatus.ACTIVATE
        )) {
            throw new IllegalStateException("이미 해당 매칭 채팅방에 참여한 사용자입니다.");
        }

        chatMemberRepository.save(ChatMember.create(room, user));
        return room.getId();
    }

    @Transactional
    public void disableMeetingGroupChatRooms(MeetingMatch match) {
        disableMeetingGroupChatRoom(match.getMeeting1());
        disableMeetingGroupChatRoom(match.getMeeting2());
    }

    @Transactional
    public void deactivateMeetingGroupMember(Meeting meeting, Long userId) {
        ChatRoom room = chatRoomRepository.findByMeeting(meeting)
                .orElseThrow(() -> new IllegalArgumentException(
                        "미팅과 연결된 동성 채팅방이 존재하지 않습니다."
                ));

        chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(
                room.getId(),
                userId,
                ChatMemberStatus.ACTIVATE
        ).ifPresent(ChatMember::deactivate);
    }

    private void disableMeetingGroupChatRoom(Meeting meeting) {
        ChatRoom room = chatRoomRepository.findByMeeting(meeting)
                .orElseThrow(() -> new IllegalArgumentException(
                        "미팅과 연결된 동성 채팅방이 존재하지 않습니다."
                ));
        room.deactivate();
    }

    public Long getChatRoomId(Meeting meeting){
        ChatRoom room = chatRoomRepository.findByMeeting(meeting)
                .orElseThrow(()-> new IllegalArgumentException("미팅id와 연결된 채팅방: 존재하지 않은 채팅방입니다."));
        return room.getId();
    }

    public Long getMatchingChatRoomId(MeetingMatch match) {
        return chatRoomRepository.findByMatch(match)
                .map(ChatRoom::getId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "매칭 정보와 연결된 남녀 채팅방이 존재하지 않습니다."
                ));
    }

    public void leaveChatRoom(Long roomId, Long userId){
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(()-> new IllegalArgumentException("채팅방 나가기: 존재하지 않는 채팅방아이디 입니다."));

        if (room.getChatStatus() == ChatStatus.DISABLED) {
            throw new IllegalStateException("비활성화된 채팅방에서는 나갈 수 없습니다.");
        }

        ChatMember chatMember = chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(roomId, userId, ChatMemberStatus.ACTIVATE)
                .orElseThrow(()-> new IllegalArgumentException("방에 참여중인 유저가 아닙니다."));

        chatMember.deactivate();
        Long referenceId=null;
        switch (room.getType()){
            case MEETING_GROUP -> {
                referenceId = room.getMeeting().getId();
                List<MeetingMember> activeMembers = meetingMemberService.getActiveMembers(referenceId);
                if (activeMembers.size() == 1 && activeMembers.get(0).getUser().getUserId().equals(userId)) {
                    room.delete();
                }
            }
            case MEETING_MATCH -> {
                referenceId = room.getMatch().getId();
            }
            case LOVEVIEW_MATCH,PROFILE_MATCH -> {
                room.deactivate();
            }
        }
        if(referenceId!=null)
            eventPublisher.publishEvent(new ChatRoomLeaveEvent(userId, room.getType(), referenceId));
        log.info("채팅방 퇴장 처리 완료: userId={}, roomId={}", userId, roomId);
    }

}
