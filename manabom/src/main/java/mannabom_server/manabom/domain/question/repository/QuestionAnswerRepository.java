package mannabom_server.manabom.domain.question.repository;

import mannabom_server.manabom.domain.question.entity.Question;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;
import mannabom_server.manabom.domain.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {

    /**
     * 프로필의 특정 질문 답변 조회
     */
    Optional<QuestionAnswer> findByProfileAndQuestion(Profile profile, Question question);

    /**
     * 프로필의 모든 답변 조회
     */
    List<QuestionAnswer> findByProfile(Profile profile);

    /**
     * 프로필의 특정 질문 답변 존재 여부 확인
     */
    boolean existsByProfileAndQuestion(Profile profile, Question question);

    /**
     * 해당 프로필 기준 답변 삭제
     */
    void deleteByProfile(Profile profile);
}
