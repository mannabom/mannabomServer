package mannabom_server.manabom.application.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.user.entity.User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {
    //해당 유저가 해당 방에 있는지
    //방 입장
    //방 나가기
    //방 생성
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;

    public Long createMeetingChatRoom(Meeting meeting){
        ChatRoom chatRoom = ChatRoom.createMeetingChatRoom(meeting);
        chatRoomRepository.save(chatRoom);

        return chatRoom.getId();
    }

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
