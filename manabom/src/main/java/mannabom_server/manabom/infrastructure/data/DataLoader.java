package mannabom_server.manabom.infrastructure.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.domain.question.entity.Question;
import mannabom_server.manabom.domain.question.enums.QuestionType;
import mannabom_server.manabom.domain.question.repository.QuestionRepository;
import mannabom_server.manabom.domain.university.entity.University;
import mannabom_server.manabom.domain.university.repository.UniversityRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 애플리케이션 시작 시 초기 데이터를 로드하는 컴포넌트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements ApplicationRunner {

    private final QuestionRepository questionRepository;
    private final UniversityRepository universityRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("초기 데이터 로드 시작");

        loadQuestions();
        loadUniversities();

        log.info("초기 데이터 로드 완료");
    }

    /**
     * 회원가입 질문 데이터 로드
     */
    private void loadQuestions() {
        if (questionRepository.count() > 0) {
            log.info("질문 데이터가 이미 존재합니다. 스킵합니다.");
            return;
        }

        log.info("질문 데이터 로드 시작");

        List<Question> questions = List.of(
                // 필수 주관식 질문 1~3
                Question.builder()
                        .question("자기소개 (필수)")
                        .questionType(QuestionType.REQUIRED_TEXT)
                        .build(),

                Question.builder()
                        .question("나를 설레게 하는 이성의 매력? (필수)")
                        .questionType(QuestionType.REQUIRED_TEXT)
                        .build(),

                Question.builder()
                        .question("연인에게 꼭 바라는 한 가지는? (필수)")
                        .questionType(QuestionType.REQUIRED_TEXT)
                        .build(),

                // 선택 주관식 질문 4~7
                Question.builder()
                        .question("나에게 연애란? (선택)")
                        .questionType(QuestionType.OPTIONAL_TEXT)
                        .build(),

                Question.builder()
                        .question("나의 소울 푸드는? (선택)")
                        .questionType(QuestionType.OPTIONAL_TEXT)
                        .build(),

                Question.builder()
                        .question("나의 하루, 그리고 나의 휴일은? (선탞)")
                        .questionType(QuestionType.OPTIONAL_TEXT)
                        .build(),

                Question.builder()
                        .question("하고 싶은 데이트는? (선택) ")
                        .questionType(QuestionType.OPTIONAL_TEXT)
                        .build(),

                // 필수 객관식 질문 (연애관 이지선다) 8~15
                Question.builder()
                        .question("애인과 싸웠을 때 (선택, 5포인트 팅)")
                        .questionType(QuestionType.REQUIRED_CHOICE)
                        .build(),

                Question.builder()
                        .question("연인과 함께한 사진 (선택, 5포인트 팅)")
                        .questionType(QuestionType.REQUIRED_CHOICE)
                        .build(),

                Question.builder()
                        .question("연애에서 더 중요한 것은 (선택, 5포인트 팅)")
                        .questionType(QuestionType.REQUIRED_CHOICE)
                        .build(),

                Question.builder()
                        .question("연인과의 데이트에서 (선택, 5포인트 팅)")
                        .questionType(QuestionType.REQUIRED_CHOICE)
                        .build(),

                Question.builder()
                        .question("연애에서 적당한 질투가 (선택, 5포인트 팅)")
                        .questionType(QuestionType.REQUIRED_CHOICE)
                        .build(),

                Question.builder()
                        .question("연인과의 이상적인 하루는 (선택, 5포인트 팅)")
                        .questionType(QuestionType.REQUIRED_CHOICE)
                        .build(),

                Question.builder()
                        .question("연인에게 주로 끌리는 모습은 (선택, 5포인트 팅)")
                        .questionType(QuestionType.REQUIRED_CHOICE)
                        .build(),

                Question.builder()
                        .question("연인이 내 친구들과 (선택, 5포인트 팅)")
                        .questionType(QuestionType.REQUIRED_CHOICE)
                        .build()
        );

        questionRepository.saveAll(questions);
        log.info("질문 데이터 로드 완료 - 총 {}개", questions.size());
    }

    /**
     * 대학 도메인 데이터 로드
     */
    private void loadUniversities() {
        if (universityRepository.count() > 0) {
            log.info("대학 데이터가 이미 존재합니다. 스킵합니다.");
            return;
        }

        log.info("대학 데이터 로드 시작");

        List<University> universities = List.of(
                // 서울 지역 주요 대학
                University.builder().name("서울대학교").domain("snu.ac.kr").build(),
                University.builder().name("연세대학교").domain("yonsei.ac.kr").build(),
                University.builder().name("고려대학교").domain("korea.ac.kr").build(),
                University.builder().name("서강대학교").domain("sogang.ac.kr").build(),
                University.builder().name("성균관대학교").domain("skku.edu").build(),
                University.builder().name("한양대학교").domain("hanyang.ac.kr").build(),
                University.builder().name("중앙대학교").domain("cau.ac.kr").build(),
                University.builder().name("경희대학교").domain("khu.ac.kr").build(),
                University.builder().name("서울시립대학교").domain("uos.ac.kr").build(),
                University.builder().name("이화여자대학교").domain("ewha.ac.kr").build(),
                University.builder().name("건국대학교").domain("konkuk.ac.kr").build(),
                University.builder().name("동국대학교").domain("dongguk.edu").build(),
                University.builder().name("홍익대학교").domain("hongik.ac.kr").build(),
                University.builder().name("숙명여자대학교").domain("sookmyung.ac.kr").build(),
                University.builder().name("국민대학교").domain("kookmin.ac.kr").build(),
                University.builder().name("세종대학교").domain("sju.ac.kr").build()
        );

        universityRepository.saveAll(universities);
        log.info("대학 데이터 로드 완료 - 총 {}개", universities.size());
    }
}