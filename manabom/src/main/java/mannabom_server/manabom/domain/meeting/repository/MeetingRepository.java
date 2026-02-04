package mannabom_server.manabom.domain.meeting.repository;

import jakarta.persistence.LockModeType;
import mannabom_server.manabom.domain.meeting.entity.Meeting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long>,MeetingRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Meeting m join fetch m.region where m.code = :code")
    Optional<Meeting> findByCodeWithLock(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Meeting m join fetch m.region where m.id = :id")
    Optional<Meeting> findByIdWithLock(@Param("id") Long id);





}
