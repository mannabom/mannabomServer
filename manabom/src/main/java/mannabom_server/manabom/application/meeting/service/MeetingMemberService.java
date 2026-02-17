package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingRole;
import mannabom_server.manabom.domain.meeting.repository.MeetingMemberRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingRepository;
import mannabom_server.manabom.domain.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MeetingMemberService {
    private final MeetingMemberRepository meetingMemberRepository;

    public void addLeader(Meeting meeting, User user){
        MeetingMember meetingMember = MeetingMember.addLeader(meeting,user);

        meetingMemberRepository.save(meetingMember);
    }
    public void addMember(Meeting meeting, User user){
        MeetingMember meetingMember = MeetingMember.addMember(meeting,user);

        meetingMemberRepository.save(meetingMember);
    }

    public Optional<MeetingMember> isActivate(Long userId){
        return meetingMemberRepository.findByUser_UserIdAndStatus(userId, ChatUserStatus.ACTIVE);
    }
    public Optional<Meeting> findMyLeadingMeeting(Long userId){
        return meetingMemberRepository.findMeetingByRole(userId, MeetingRole.LEADER, ChatUserStatus.ACTIVE);
    }

    public  boolean isKicked(Long meetingId, Long userId){
        return meetingMemberRepository.existsByMeeting_IdAndUser_UserIdAndStatus(meetingId, userId, ChatUserStatus.KICKED);
    }

    public List<MeetingMember> getActiveMembers(Long meetingId){
        return meetingMemberRepository.findByMeetingIdAndStatus(meetingId,ChatUserStatus.ACTIVE);
    }

    public boolean isLeader(Long meetingId, Long userId){
        return meetingMemberRepository.existsByMeeting_IdAndUser_UserIdAndStatusAndMeetingRole(meetingId, userId, ChatUserStatus.ACTIVE,MeetingRole.LEADER);
    }
    /*리더 멤버 포함*/
    public boolean isMember(Long meetingId, Long userId){
        return meetingMemberRepository.existsByMeeting_IdAndUser_UserIdAndStatus(meetingId, userId, ChatUserStatus.ACTIVE);
    }

    public Long getLeader(Long meetingId){
        return meetingMemberRepository.findUserIdByMeetingIdAndRole(meetingId, MeetingRole.LEADER,ChatUserStatus.ACTIVE)
                .orElseThrow(()->new IllegalArgumentException("해당 미팅에 리더가 존재하지 않습니다."));
    }


    public void deactivateStatus(Long meetingId, Long userId){
        MeetingMember mm = isActivate(userId).orElseThrow(()-> new IllegalArgumentException("채팅방나가기: 미팅방에 입장하지 않은 유저입니다."));
        if(!mm.getMeeting().getId().equals(meetingId)){
            throw new IllegalArgumentException("채팅방 나가기: 유효하지 않은 채팅방아이디입니다.");
        }
        mm.deactivate();
    }

    public void appointNextLeader(Long meetingId){
        List<MeetingMember> list = getActiveMembers(meetingId);
        if (!list.isEmpty()) {
            MeetingMember nextLeader = list.get(0);
            nextLeader.appointLeader();

            log.info("미팅 [{}]의 새로운 리더로 유저 [{}]가 선출되었습니다.", meetingId, nextLeader.getUser().getUserId());
        }
    }



}
