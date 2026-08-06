package mannabom_server.manabom.infrastructure.security.crypto;

import mannabom_server.manabom.application.gifticon.port.GifticonTokenCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmGifticonTokenCipher implements GifticonTokenCipher {

    private static final String VERSION_PREFIX = "v1:";
    private static final int AES_256_KEY_BYTES = 32;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private static final String KEY_SETTING_NAME =
            "APP_KAKAO_GIFTBIZ_TOKEN_ENCRYPTION_KEY";

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmGifticonTokenCipher(
            @Value("${app.kakao.giftbiz.token-encryption-key:}") String encodedKey
    ) {
        this.secretKey = decodeOptionalSecretKey(encodedKey);
    }

    @Override
    public String encrypt(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new IllegalArgumentException("암호화할 템플릿 토큰은 비어있을 수 없습니다.");
        }
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, requiredSecretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("기프티콘 템플릿 토큰 암호화에 실패했습니다.", e);
        }
    }

    @Override
    public String decrypt(String encryptedToken) {
        if (encryptedToken == null || !encryptedToken.startsWith(VERSION_PREFIX)) {
            throw new IllegalStateException("지원하지 않는 기프티콘 템플릿 토큰 암호문입니다.");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(
                    encryptedToken.substring(VERSION_PREFIX.length())
            );
            if (payload.length <= GCM_IV_BYTES) {
                throw new IllegalStateException("기프티콘 템플릿 토큰 암호문이 손상되었습니다.");
            }

            byte[] iv = new byte[GCM_IV_BYTES];
            byte[] ciphertext = new byte[payload.length - GCM_IV_BYTES];
            ByteBuffer.wrap(payload)
                    .get(iv)
                    .get(ciphertext);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, requiredSecretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IllegalStateException("기프티콘 템플릿 토큰 복호화에 실패했습니다.", e);
        }
    }

    private SecretKey decodeOptionalSecretKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            return null;
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encodedKey.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    KEY_SETTING_NAME + "는 Base64 형식이어야 합니다.",
                    e
            );
        }
        if (decoded.length != AES_256_KEY_BYTES) {
            throw new IllegalStateException(
                    KEY_SETTING_NAME + "는 Base64로 인코딩한 32바이트 키여야 합니다."
            );
        }
        return new SecretKeySpec(decoded, "AES");
    }

    private SecretKey requiredSecretKey() {
        if (secretKey == null) {
            throw new IllegalStateException(KEY_SETTING_NAME + " 설정이 필요합니다.");
        }
        return secretKey;
    }
}
