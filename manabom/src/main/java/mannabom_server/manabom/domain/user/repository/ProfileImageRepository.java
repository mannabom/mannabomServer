package mannabom_server.manabom.domain.user.repository;

import jakarta.persistence.LockModeType;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.ProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileImageRepository extends JpaRepository<ProfileImage, Long> {

    /**
     * 프로필의 이미지들을 순서대로 조회
     */
    List<ProfileImage> findByProfileOrderByImageIndex(Profile profile);

    /**
     * 프로필의 모든 이미지 조회
     */
    @Query("""
            select pi
            from ProfileImage pi
            where pi.profile = :profile
            order by pi.isMain desc, pi.imageIndex asc, pi.imageId asc
            """)
    List<ProfileImage> findAllByProfile(@Param("profile") Profile profile);

    /**
     * 프로필의 모든 이미지 조회(비관적 락)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select pi
            from ProfileImage pi
            where pi.profile = :profile
            order by pi.isMain desc, pi.imageIndex asc, pi.imageId asc
            """)
    List<ProfileImage> findAllByProfileForUpdate(@Param("profile") Profile profile);

    /**
     * 프로필의 대표 이미지 조회
     */
    Optional<ProfileImage> findByProfileAndIsMainTrue(Profile profile);

    /**
     * 프로필의 이미지 개수 확인
     */
    int countByProfile(Profile profile);

    /**
     * 프로필의 모든 이미지 삭제
     */
    void deleteByProfile(Profile profile);

    /**
     * 프로필의 모든 이미지를 대표사진 해제 (대표사진 변경 시 사용)
     */
    @Modifying
    @Query("UPDATE ProfileImage p SET p.isMain = false WHERE p.profile = :profile")
    void unsetAllMainPhotos(@Param("profile") Profile profile);

    /**
     * 해당 photoId가 해당 유저의 것인지 확인
     */
    boolean existsByImageIdAndProfile(Long imageId, Profile profile);
}