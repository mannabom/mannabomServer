package mannabom_server.manabom.presentation.signup.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.signup.dto.response.QuestionListResponseDto;
import mannabom_server.manabom.application.signup.service.QuestionService;
import mannabom_server.manabom.domain.question.enums.QuestionType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 질문 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/signup/questions")
@RequiredArgsConstructor
@Slf4j
public class QuestionController {

    private final QuestionService questionService;

    /**
     * 모든 질문 목록 조회
     */
    @GetMapping
    public ResponseEntity<QuestionListResponseDto> getAllQuestions() {
        log.info("질문 목록 조회 API 호출");

        QuestionListResponseDto response = questionService.getAllQuestions();

        log.info("질문 목록 조회 API 완료 - 총 {}개",
                response.getData().getQuestions().size());

        return ResponseEntity.ok(response);
    }

    /**
     * 질문 유형별 조회
     */
    @GetMapping("/type/{questionType}")
    public ResponseEntity<QuestionListResponseDto> getQuestionsByType(
            @PathVariable QuestionType questionType) {

        log.info("질문 유형별 조회 API 호출 - 유형: {}", questionType);

        QuestionListResponseDto response = questionService.getQuestionsByType(questionType);

        log.info("질문 유형별 조회 API 완료 - 개수: {}",
                response.getData().getQuestions().size());

        return ResponseEntity.ok(response);
    }
}