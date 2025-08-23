package mannabom_server.manabom.application.signup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.signup.dto.response.QuestionListResponseDto;
import mannabom_server.manabom.domain.question.entity.Question;
import mannabom_server.manabom.domain.question.enums.QuestionType;
import mannabom_server.manabom.domain.question.repository.QuestionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 질문 관련 서비스 - (캐시 + 폴백 활용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;

    /**
     * 모든 질문 목록 조회 (캐시 + 폴백)
     */
    public QuestionListResponseDto getAllQuestions() {
        log.info("전체 질문 목록 조회 요청");

        try {
            List<QuestionListResponseDto.QuestionDto> questionDtos = getAllQuestionsFromCache();

            if (questionDtos.isEmpty()) {
                log.warn("질문 목록이 비어있습니다. 초기 데이터를 확인해주세요.");
            }

            log.info("질문 목록 조회 완료 - 총 {}개", questionDtos.size());

            return buildSuccessResponse(questionDtos, "질문 목록 조회 성공");

        } catch (Exception e) {
            log.error("캐시를 통한 질문 목록 조회 실패", e);
            return getAllQuestionsDirectly();
        }
    }

    /**
     * 질문 유형별 조회 (캐시 + 폴백)
     */
    public QuestionListResponseDto getQuestionsByType(QuestionType questionType) {
        log.info("질문 유형별 조회 - 유형: {}", questionType);

        try {
            List<QuestionListResponseDto.QuestionDto> questionDtos = getQuestionsByTypeFromCache(questionType);

            log.info("질문 유형별 조회 완료 - 유형: {}, 개수: {}", questionType, questionDtos.size());

            return buildSuccessResponse(questionDtos, "질문 목록 조회 성공");

        } catch (Exception e) {
            log.error("캐시를 통한 질문 유형별 조회 실패 - 유형: {}", questionType, e);
            return getQuestionsByTypeDirectly(questionType);
        }
    }

    /**
     * 캐시를 통한 모든 질문 조회
     */
    @Cacheable(
            value = "questions",
            key = "'all'",
            condition = "true",  // 항상 캐시 시도
            unless = "#result == null or #result.isEmpty()"  // 결과가 null이거나 비어있으면 캐시하지 않음
    )
    public List<QuestionListResponseDto.QuestionDto> getAllQuestionsFromCache() {
        log.debug("DB에서 전체 질문 목록 조회 중...");
        return fetchAllQuestionsFromDB();
    }

    /**
     * 캐시를 통한 유형별 질문 조회
     */
    @Cacheable(
            value = "questions",
            key = "'type_' + #questionType.name()",
            condition = "#questionType != null",
            unless = "#result == null or #result.isEmpty()"
    )
    public List<QuestionListResponseDto.QuestionDto> getQuestionsByTypeFromCache(QuestionType questionType) {
        log.debug("DB에서 질문 유형별 조회 중 - 유형: {}", questionType);
        return fetchQuestionsByTypeFromDB(questionType);
    }

    /**
     * 폴백: 캐시 우회 전체 질문 직접 조회
     */
    private QuestionListResponseDto getAllQuestionsDirectly() {
        log.warn("폴백 실행: 캐시 우회하여 DB 직접 조회");

        try {
            List<QuestionListResponseDto.QuestionDto> questionDtos = fetchAllQuestionsFromDB();
            return buildSuccessResponse(questionDtos, "질문 목록 조회 성공 (캐시 우회)");

        } catch (Exception e) {
            log.error("DB 직접 조회도 실패", e);
            throw new RuntimeException("질문 목록을 불러올 수 없습니다. 관리자에게 문의해주세요.", e);
        }
    }

    /**
     * 폴백: 캐시 우회 유형별 질문 직접 조회
     */
    private QuestionListResponseDto getQuestionsByTypeDirectly(QuestionType questionType) {
        log.warn("폴백 실행: 캐시 우회하여 DB 직접 조회 - 유형: {}", questionType);

        try {
            List<QuestionListResponseDto.QuestionDto> questionDtos = fetchQuestionsByTypeFromDB(questionType);
            return buildSuccessResponse(questionDtos, "질문 목록 조회 성공 (캐시 우회)");

        } catch (Exception e) {
            log.error("DB 직접 조회도 실패 - 유형: {}", questionType, e);
            throw new RuntimeException("질문 목록을 불러올 수 없습니다. 관리자에게 문의해주세요.", e);
        }
    }

    /**
     * 실제 DB 조회 - 전체 질문
     */
    private List<QuestionListResponseDto.QuestionDto> fetchAllQuestionsFromDB() {
        List<Question> questions = questionRepository.findAllByOrderByQuestionId();
        return questions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 실제 DB 조회 - 유형별 질문
     */
    private List<QuestionListResponseDto.QuestionDto> fetchQuestionsByTypeFromDB(QuestionType questionType) {
        List<Question> questions = questionRepository.findByQuestionType(questionType);
        return questions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 성공 응답 생성 (중복 제거)
     */
    private QuestionListResponseDto buildSuccessResponse(
            List<QuestionListResponseDto.QuestionDto> questionDtos,
            String message) {

        return QuestionListResponseDto.builder()
                .success(true)
                .data(QuestionListResponseDto.QuestionListDataDto.builder()
                        .questions(questionDtos)
                        .build())
                .message(message)
                .build();
    }

    /**
     * Question 엔터티를 DTO로 변환
     */
    private QuestionListResponseDto.QuestionDto convertToDto(Question question) {
        return QuestionListResponseDto.QuestionDto.builder()
                .questionId(question.getQuestionId())
                .question(question.getQuestion())
                .questionType(question.getQuestionType())
                .build();
    }
}