package mannabom_server.manabom.application.auth.service;

import mannabom_server.manabom.domain.user.enums.Gender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 심사·캡처용 테스트 카카오 계정 목록.
 * 목록에 있는 회원번호만 카카오 동의항목(이름·출생연도·성별) 없이 고정값으로 가입할 수 있다.
 */
@Component
public class KakaoTestAccountPolicy {

    static final String TEST_NAME = "테스트";
    static final int TEST_BIRTH_YEAR = 2000;
    static final Gender TEST_GENDER = Gender.FEMALE;

    private final Set<String> testAccountIds;

    public KakaoTestAccountPolicy(@Value("${app.kakao.test-account-ids:}") String testAccountIds) {
        this.testAccountIds = Arrays.stream(testAccountIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isTestAccount(String kakaoId) {
        return testAccountIds.contains(kakaoId);
    }
}
