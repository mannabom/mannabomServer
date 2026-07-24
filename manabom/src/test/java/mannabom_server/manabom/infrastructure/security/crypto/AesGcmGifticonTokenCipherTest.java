package mannabom_server.manabom.infrastructure.security.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmGifticonTokenCipherTest {

    private static final String ENCODED_KEY = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)
    );

    @Test
    void encryptsAndDecryptsTemplateToken() {
        AesGcmGifticonTokenCipher cipher = new AesGcmGifticonTokenCipher(ENCODED_KEY);

        String encrypted = cipher.encrypt("secret-template-token");

        assertThat(encrypted)
                .startsWith("v1:")
                .doesNotContain("secret-template-token");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("secret-template-token");
    }

    @Test
    void usesDifferentIvForEveryEncryption() {
        AesGcmGifticonTokenCipher cipher = new AesGcmGifticonTokenCipher(ENCODED_KEY);

        assertThat(cipher.encrypt("same-token"))
                .isNotEqualTo(cipher.encrypt("same-token"));
    }

    @Test
    void rejectsCiphertextEncryptedWithAnotherKey() {
        AesGcmGifticonTokenCipher cipher = new AesGcmGifticonTokenCipher(ENCODED_KEY);
        AesGcmGifticonTokenCipher anotherCipher = new AesGcmGifticonTokenCipher(
                Base64.getEncoder().encodeToString(
                        "abcdef0123456789abcdef0123456789".getBytes(StandardCharsets.UTF_8)
                )
        );

        String encrypted = cipher.encrypt("secret-template-token");

        assertThatThrownBy(() -> anotherCipher.decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class);
    }
}
