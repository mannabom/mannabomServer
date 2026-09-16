package mannabom_server.manabom.domain.chat.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.meeting.dto.response.MatchedChatRoomInfo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatMemberQueryRepository {
    private final EntityManager em;

    public List<MatchedChatRoomInfo.Participant> findParticipantsByRoomId(Long roomId) {
        return em.createQuery("""
            select new mannabom_server.manabom.application.meeting.dto.response.MatchedChatRoomInfo$Participant(
                u.userId,
                p.profileId,
                p.gender,
                p.nickName,
                (
                    select pi.url
                    from ProfileImage pi
                    where pi.profile = p
                      and pi.isMain = true
                )
            )
            from ChatMember cm
            join cm.user u
            join Profile p on p.user = u
            where cm.room.id = :roomId
        """, MatchedChatRoomInfo.Participant.class)
                .setParameter("roomId", roomId)
                .getResultList();
    }
}
