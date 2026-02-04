package mannabom_server.manabom.domain.meeting.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.meeting.dto.raw.TeamMemberDetailRaw;
import mannabom_server.manabom.application.meeting.dto.raw.TeamMemberRaw;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MeetingMemberQueryRepository {
    private final EntityManager em;

    public List<TeamMemberRaw> findActiveTeamMemberRaw(Long meetingId) {
        return em.createQuery(
                        """
                                select new mannabom_server.manabom.application.meeting.dto.raw.TeamMemberRaw(
                                    u.id,
                                    p.nickName,
                                    (
                                        select pi.url from ProfileImage pi
                                        where pi.profile = p and pi.isMain = true
                                    ),
                                    mm.meetingRole
                                ) from MeetingMember mm
                                join mm.user u 
                                join Profile p on p.user = u 
                                where mm.meeting.id = :meetingId and mm.status = :active
                                """, TeamMemberRaw.class
                ).setParameter("meetingId", meetingId)
                .setParameter("active", ChatUserStatus.ACTIVE)
                .getResultList();
    }

    public List<TeamMemberDetailRaw> findActiveTeamMemberDetailRaw(Long meetingId) {
        return em.createQuery(
                        """
                                select new mannabom_server.manabom.application.meeting.dto.raw.TeamMemberDetailRaw(
                                    u.id,
                                    p.nickName,
                                    p.birthDate,
                                    p.mbti,p.smoking,p.alcohol,
                                    (
                                        select pi.url from ProfileImage  pi
                                        where pi.profile = p and pi.isMain = true
                                    )
                                ) from MeetingMember mm
                                join mm.user u
                                join Profile p on p.user = u
                                where mm.meeting.id = :meetingId
                                and mm.status = :active
                                """, TeamMemberDetailRaw.class
                ).setParameter("meetingId", meetingId)
                .setParameter("active",ChatUserStatus.ACTIVE)
                .getResultList();
    }
}
