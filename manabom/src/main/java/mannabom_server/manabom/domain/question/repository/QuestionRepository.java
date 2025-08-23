package mannabom_server.manabom.domain.question.repository;

import mannabom_server.manabom.domain.question.entity.Question;
import mannabom_server.manabom.domain.question.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    /**
     * 질문 유형별 조회
     */
    List<Question> findByQuestionType(QuestionType questionType);

    /**
     * 모든 질문을 ID 순으로 조회 (API용)
     */
    List<Question> findAllByOrderByQuestionId();
}
