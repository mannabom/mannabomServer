package mannabom_server.manabom.application.userInfo.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.userInfo.dto.GetUserInfoResponse;
import mannabom_server.manabom.application.userInfo.dto.PutUserInfoRequest;
import mannabom_server.manabom.domain.question.entity.Question;
import mannabom_server.manabom.domain.question.entity.QuestionAnswer;
import mannabom_server.manabom.domain.question.repository.QuestionAnswerRepository;
import mannabom_server.manabom.domain.question.repository.QuestionRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserInfoService {

    private final ProfileRepository profileRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final UserRepository userRepository;

    public GetUserInfoResponse getUserInfo(Long userId){
        log.info("회원 정보 조회 서비스 계층 동작 시작");

        User user = userRepository.findById(userId).orElseThrow(()-> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Profile profile = profileRepository.findByUser(user).orElseThrow(()-> new IllegalArgumentException("해당 사용자의 프로필을 찾을 수 없습니다."));
        List<QuestionAnswer> questionAnswerList = questionAnswerRepository.findByProfileWithQuestion(profile);

        log.info("회원 정보 조회 서비스 계층 동작 완료");

        return new GetUserInfoResponse(profile, questionAnswerList);
    }

    @Transactional
    public void putUserInfo(Long userId, PutUserInfoRequest request){
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Profile profile = profileRepository.findByUser(user).orElseThrow(() -> new IllegalArgumentException("해당 사용자의 프로필을 찾을 수 없습니다."));

        log.info("회원 정보 입력(프로필 수정) 서비스 계층 동작 시작");

        if(request.getProfile() != null) {
            profile.setGender(request.getProfile().getGender());
            profile.setHeight(request.getProfile().getHeight());
            profile.setBodyType(request.getProfile().getBodyType());
            profile.setRegionSido(request.getProfile().getRegionSido());
            profile.setRegionSigungu(request.getProfile().getRegionSigungu());
            profile.setNickName(request.getProfile().getNickName());
            profile.setBirthDate(request.getProfile().getBirthDate());
            profile.setMbti(request.getProfile().getMbti());
            profile.setSmoking(request.getProfile().getSmoking());
            profile.setAlcohol(request.getProfile().getAlcohol());
            profile.setUniversity(request.getProfile().getUniversity());
            profile.setEmail(request.getProfile().getEmail());

            profileRepository.save(profile);
        }

        log.info("회원 정보 입력(프로필 수정) 서비스 계층 동작 완료, 답변 업데이트 시작");

        // --- 기존 답변을 질문과 함께 한 번에 조회
        List<QuestionAnswer> existingList = questionAnswerRepository.findByProfileWithQuestion(profile);
        Map<Long, QuestionAnswer> existingByQid = existingList.stream()
                .collect(Collectors.toMap(qa -> qa.getQuestion().getQuestionId(), qa -> qa));

        // --- 요청 리스트(없으면 빈 리스트)
        List<QuestionAnswer> requested = Optional.ofNullable(request.getAnswers())
                .orElseGet(Collections::emptyList);

        // 요청된 questionId 집합(“요청에 없는 기존 답변은 유지” 정책이면 안 씀)
        Set<Long> handled = new HashSet<>();

        for (QuestionAnswer dto : requested) {
            Long questionId = dto.getQuestion().getQuestionId();
            String answer = dto.getAnswer();
            handled.add(questionId);

            QuestionAnswer existing = existingByQid.get(questionId);

            // 1) 비어있으면 삭제
            if (!StringUtils.hasText(answer)) {
                if (existing != null) {
                    questionAnswerRepository.delete(existing);
                }
                continue;
            }

            // 2) 존재하면 내용 변경
            if (existing != null) {
                if (!Objects.equals(existing.getAnswer(), answer)) {
                    existing.updateAnswer(answer);
                }
                continue;
            }

            // 3) 없으면 새로 생성
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 questionId: " + questionId));

            QuestionAnswer created = QuestionAnswer.builder()
                    .profile(profile)
                    .question(question)
                    .answer(answer)
                    .build();

            questionAnswerRepository.save(created);

            log.info("회원 정보 입력(답변 입력) 서비스 계층 동작 완료");

        }

    }

}
