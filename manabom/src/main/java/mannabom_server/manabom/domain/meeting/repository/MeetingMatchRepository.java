package mannabom_server.manabom.domain.meeting.repository;

import jakarta.persistence.LockModeType;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MeetingMatchRepository extends JpaRepository<MeetingMatch,Long> {
    /*매칭 수락, 거절할 때 getMyMeeting에서 user까지 필요*/
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MeetingMatch m " +
            "JOIN FETCH m.meeting1 mt1 " +
            "JOIN FETCH m.meeting2 mt2 " +
            "WHERE m.id = :matchId")
    Optional<MeetingMatch> findByIdWithLockAndMeeting(@Param(value = "matchId") Long matchId);



    @Query(
            "select m from MeetingMatch m "+
            "join fetch m.meeting1 m1 "+
                    "join fetch m.meeting2 m2 "+
                    "WHERE m.id = :matchId"
    )
    Optional<MeetingMatch> findByIdWithMeeting(@Param(value = "matchId") Long matchId);
}
