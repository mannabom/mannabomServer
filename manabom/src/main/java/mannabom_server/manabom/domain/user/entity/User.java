package mannabom_server.manabom.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.common.BaseTimeEntity;

/**
 * 사용자 기본 정보 엔터티
 * 카카오 로그인 시 생성되는 기본 사용자 정보
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "kakao_id", unique = true, nullable = false)     // 카카오 고유 ID (로그인 식별자)
    private String kakaoId;

    @Column(name = "user_name")                                     // 카카오에서 받은 사용자 이름 (실명)
    private String userName;

    @Column(name = "is_verified", nullable = false)                 // 이메일 인증 여부
    private Boolean isVerified = false;

    @Column(name = "phone_num")
    private String phoneNum;

    @Column(name = "is_membership", nullable = false)
    private Boolean isMembership = false;

    @Builder
    public User(String kakaoId, String userName) {
        this.kakaoId = kakaoId;
        this.userName = userName;
        this.isVerified = false;
        this.isMembership = false;
    }

    /**
     * 이메일 인증 완료 처리
     */
    public void verifyEmail() {
        this.isVerified = true;
    }

    /**
     * 멤버십 상태 변경
     */
    public void updateMembership(boolean isMembership) {
        this.isMembership = isMembership;
    }
}
