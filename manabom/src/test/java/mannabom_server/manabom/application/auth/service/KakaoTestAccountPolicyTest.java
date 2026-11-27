package mannabom_server.manabom.application.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoTestAccountPolicyTest {

    @Test
    void matchesOnlyListedKakaoIds() {
        KakaoTestAccountPolicy policy = new KakaoTestAccountPolicy(" 111, 222 ,,");

        assertThat(policy.isTestAccount("111")).isTrue();
        assertThat(policy.isTestAccount("222")).isTrue();
        assertThat(policy.isTestAccount("333")).isFalse();
    }

    @Test
    void allowsNoAccountWhenEmpty() {
        KakaoTestAccountPolicy policy = new KakaoTestAccountPolicy("");

        assertThat(policy.isTestAccount("")).isFalse();
        assertThat(policy.isTestAccount("111")).isFalse();
    }
}
