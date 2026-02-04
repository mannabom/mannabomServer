package mannabom_server.manabom.domain.user.repository;

import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.Gender;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    /**
     * 사용자로 프로필 조회
     */
    Optional<Profile> findByUser(User user);

    /**
     * 사용자로 프로필 조회 (지역이랑 대학까지 join fetch)
     */
    @Query("SELECT p FROM Profile p " +
            "LEFT JOIN FETCH p.region " +
            "LEFT JOIN FETCH p.university " +
            "WHERE p.user = :user")
    Optional<Profile> findByUserWithRegionAndUniversity(User user);
    /**
     * 닉네임으로 프로필 조회
     */
    Optional<Profile> findByNickName(String nickName);

    /**
     * 닉네임 존재 여부 확인
     */
    boolean existsByNickName(String nickName);

    /**
     * 이메일로 프로필 조회
     */
    Optional<Profile> findByEmail(String email);

    /**
     * 유저 ID 기준으로 프로필 삭제
     */
    void deleteByUser(User user);

    Optional<Profile> findByUser_UserId(Long userId);

    @Query("""
        select p
        from Profile p
        where p.user.userId <> :requesterId
          and p.gender <> :requesterGender
          and p.birthDate between :birthFrom and :birthTo
          and (:smokingEmpty = true or p.smoking in :smoking)
          and (:alcoholEmpty = true or p.alcohol in :alcohol)
          and not exists (
              select 1
              from ProfileRecommendHistory h
              where h.requesterUserId = :requesterId
                and h.targetUserId = p.user.userId
                and h.recommendedAt >= :cooldownFrom
          )
        order by
          case
            when p.region.sidoName = :reqSido and p.region.sigunguName = :reqSigungu then 0
            when p.region.sidoName = :reqSido then 1
            else 2
          end,
          p.profileId desc
        """)
    Page<Profile> findProfileMatchCandidates(
            @Param("requesterId") Long requesterId,
            @Param("requesterGender") Gender requesterGender,
            @Param("birthFrom") LocalDate birthFrom,
            @Param("birthTo") LocalDate birthTo,
            @Param("smokingEmpty") boolean smokingEmpty,
            @Param("smoking") List<SmokingHabit> smoking,
            @Param("alcoholEmpty") boolean alcoholEmpty,
            @Param("alcohol") List<DrinkingHabit> alcohol,
            @Param("reqSido") String reqSido,
            @Param("reqSigungu") String reqSigungu,
            @Param("cooldownFrom") LocalDateTime cooldownFrom,
            Pageable pageable
    );

    @Query("""
        select p
        from Profile p
        where p.user.userId <> :requesterId
          and p.gender <> :requesterGender
          and p.birthDate between :birthFrom and :birthTo
          and (:smokingEmpty = true or p.smoking in :smoking)
          and (:alcoholEmpty = true or p.alcohol in :alcohol)
          and not exists (
              select 1
              from LoveViewRecommendHistory h
              where h.requesterUserId = :requesterId
                and h.targetUserId = p.user.userId
                and h.recommendedAt >= :cooldownFrom
          )
        order by
          case
            when p.region.sidoName = :reqSido and p.region.sigunguName = :reqSigungu then 0
            when p.region.sidoName = :reqSido then 1
            else 2
          end,
          p.profileId desc
        """)
    Page<Profile> findLoveViewMatchCandidates(
            @Param("requesterId") Long requesterId,
            @Param("requesterGender") Gender requesterGender,
            @Param("birthFrom") LocalDate birthFrom,
            @Param("birthTo") LocalDate birthTo,
            @Param("smokingEmpty") boolean smokingEmpty,
            @Param("smoking") List<SmokingHabit> smoking,
            @Param("alcoholEmpty") boolean alcoholEmpty,
            @Param("alcohol") List<DrinkingHabit> alcohol,
            @Param("reqSido") String reqSido,
            @Param("reqSigungu") String reqSigungu,
            @Param("cooldownFrom") LocalDateTime cooldownFrom,
            Pageable pageable
    );
}
