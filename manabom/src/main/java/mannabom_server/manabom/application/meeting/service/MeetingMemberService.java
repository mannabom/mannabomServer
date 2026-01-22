package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import mannabom_server.manabom.domain.meeting.repository.MeetingMemberRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingRepository;
import mannabom_server.manabom.domain.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MeetingMemberService {
    private final MeetingMemberRepository meetingMemberRepository;
    private MeetingRepository meetingRepository;

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
    public  boolean isKicked(Long meetingId, Long userId){
        return meetingMemberRepository.existsByMeeting_IdAndUser_UserIdAndStatus(meetingId, userId, ChatUserStatus.KICKED);
    }




}
