package mannabom_server.manabom.application.chat.service;

import com.google.firestore.v1.TransactionOptions;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.meeting.dto.response.MatchedChatRoomInfo;
import mannabom_server.manabom.application.meeting.service.MeetingMemberService;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.repository.ChatMemberQueryRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.user.entity.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatRoomService {
    //해당 유저가 해당 방에 있는지
    //방 입장
    //방 나가기
    //방 생성
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMemberQueryRepository chatMemberQueryRepository;
    private final MeetingMemberService meetingMemberService;

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

        List<MatchedChatRoomInfo.Participant> participants = chatMemberQueryRepository.findParticipantsByRoomId(chatRoom.getId());
        return MatchedChatRoomInfo.of(chatRoom.getId(),participants);
    }

    @Transactional
    public Long createProfileChatRoom(){
        ChatRoom chatRoom = ChatRoom.createProfileChatRoom();
        chatRoomRepository.save(chatRoom);

        return chatRoom.getId();
    }
    @Transactional
    public Long createLoveviewChatRoom(){
        ChatRoom chatRoom = ChatRoom.createLoveviewChatRoom();
        chatRoomRepository.save(chatRoom);

        return chatRoom.getId();
    }
    @Transactional
    public Long joinChatRoom(Meeting meeting, User user){
        ChatRoom room = chatRoomRepository.findByMeeting(meeting)
                .orElseThrow(()-> new IllegalArgumentException("미팅id와 연결된 채팅방: 존재하지 않은 채팅방입니다."));
        ChatMember chatMember = ChatMember.create(room,user);
        chatMemberRepository.save(chatMember);

        return room.getId();
    }

    public Long getChatRoomId(Meeting meeting){
        ChatRoom room = chatRoomRepository.findByMeeting(meeting)
                .orElseThrow(()-> new IllegalArgumentException("미팅id와 연결된 채팅방: 존재하지 않은 채팅방입니다."));
        return room.getId();
    }

}
