package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.meeting.dto.raw.TeamMemberDetailRaw;
import mannabom_server.manabom.application.meeting.dto.raw.TeamMemberRaw;
import mannabom_server.manabom.application.meeting.dto.response.MeetingChatRoomInfo;
import mannabom_server.manabom.application.meeting.dto.response.TeamMemberProfilesDto;
import mannabom_server.manabom.application.meeting.dto.response.TeamMemberPreviewDto;
import mannabom_server.manabom.domain.meeting.enums.MeetingRole;
import mannabom_server.manabom.domain.meeting.repository.MeetingMemberQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingMemberReadService {
    private final MeetingMemberQueryRepository meetingMemberQueryRepository;


    public List<MeetingChatRoomInfo.TeamMember> getActiveTeamMembers(Long meetingId) {
        List<TeamMemberRaw> raws = meetingMemberQueryRepository.findActiveTeamMemberRaw(meetingId);

        return raws.stream().map(
                r ->
                        MeetingChatRoomInfo.TeamMember.builder()
                                .userId(r.userId())
                                .nickname(r.nickName())
                                .profileImage(r.mainImageUrl())
                                .isLeader(r.role() == MeetingRole.LEADER)
                                .build()

        ).toList();
    }


    public List<TeamMemberPreviewDto> getActiveTeamMemberPreivews(Long meetingId){
        List<TeamMemberRaw> raws = meetingMemberQueryRepository.findActiveTeamMemberRaw(meetingId);

        return raws.stream().map(
                r ->
                        TeamMemberPreviewDto.builder()
                                .userId(r.userId())
                                .profileImage(r.mainImageUrl())
                                .build()

        ).toList();
    }

    public List<TeamMemberProfilesDto.TeamMemberDetailDto> getActiveTeammMemberDetails(Long meetingId){
        List<TeamMemberDetailRaw> raws = meetingMemberQueryRepository.findActiveTeamMemberDetailRaw(meetingId);

        return raws.stream().map(
                r -> TeamMemberProfilesDto.TeamMemberDetailDto.builder()
                        .mbti(r.mbti())
                        .userId(r.userId())
                        .profileImage(r.mainImageUrl())
                        .nickname(r.nickname())
                        .age(computeAge(r.birthDate()))
                        .drinkingHabit(r.drinkingHabit())
                        .smokingHabit(r.smokingHabit())
                        .build()
        ).toList();
    }

    private Integer computeAge(LocalDate birthDate){
        if(birthDate==null) return null;
        return  LocalDate.now().getYear()-birthDate.getYear()+1;
    }
}
