package mannabom_server.manabom.infrastructure.external.apple;

import com.apple.itunes.storekit.client.APIError;
import com.apple.itunes.storekit.client.APIException;
import com.apple.itunes.storekit.client.AppStoreServerAPIClient;
import com.apple.itunes.storekit.model.Data;
import com.apple.itunes.storekit.model.Environment;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.ResponseBodyV2DecodedPayload;
import com.apple.itunes.storekit.model.TransactionInfoResponse;
import com.apple.itunes.storekit.verification.SignedDataVerifier;
import com.apple.itunes.storekit.verification.VerificationException;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.tingPurchase.port.AppleServerNotificationDecoder;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway;
import mannabom_server.manabom.application.tingPurchase.port.StoreGatewayException;
import mannabom_server.manabom.application.tingPurchase.port.StorePurchaseInvalidException;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.enums.StoreEnvironment;
import mannabom_server.manabom.infrastructure.external.apple.config.AppleAppStoreProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Apple App Store Server API 로 거래를 조회하고 JWS 서명을 검증한다.
 * 클라이언트는 StoreKit 2 의 Transaction.id 만 보내며, 서버가 Apple 에 직접 조회하므로 영수증 위조가 불가능하다.
 * 설정이 없어도 애플리케이션은 기동되어야 하므로 클라이언트는 첫 사용 시 지연 생성한다.
 */
@Slf4j
@Component
public class AppleAppStoreClient implements InAppPurchaseGateway, AppleServerNotificationDecoder {

    private final AppleAppStoreProperties properties;
    private final Map<Environment, AppStoreServerAPIClient> apiClients = new EnumMap<>(Environment.class);
    private final Map<Environment, SignedDataVerifier> verifiers = new EnumMap<>(Environment.class);
    private volatile byte[][] rootCertificates;

    public AppleAppStoreClient(AppleAppStoreProperties properties) {
        this.properties = properties;
    }

    @Override
    public PurchaseStore store() {
        return PurchaseStore.APPLE;
    }

    @Override
    public StorePurchase verify(VerifyPurchaseCommand command) {
        Environment primary = primaryEnvironment();
        String signed;
        Environment resolved = primary;
        try {
            signed = fetchSignedTransaction(primary, command.purchaseKey());
        } catch (TransactionNotFound e) {
            if (primary == Environment.PRODUCTION && properties.isSandboxFallbackEnabled()) {
                log.info("Apple PRODUCTION 에서 거래를 찾지 못해 SANDBOX 로 재조회. transactionId={}", command.purchaseKey());
                resolved = Environment.SANDBOX;
                try {
                    signed = fetchSignedTransaction(Environment.SANDBOX, command.purchaseKey());
                } catch (TransactionNotFound sandboxMiss) {
                    throw new StorePurchaseInvalidException("Apple 에 존재하지 않는 거래입니다: " + command.purchaseKey());
                }
            } else {
                throw new StorePurchaseInvalidException("Apple 에 존재하지 않는 거래입니다: " + command.purchaseKey());
            }
        }

        JWSTransactionDecodedPayload payload = decodeTransaction(resolved, signed);
        if (payload.getTransactionId() == null || !payload.getTransactionId().equals(command.purchaseKey())) {
            throw new StorePurchaseInvalidException("Apple 거래 번호가 요청과 일치하지 않습니다.");
        }
        return toStorePurchase(payload, resolved, signed);
    }

    @Override
    public void consume(ConsumePurchaseCommand command) {
        // Apple 소모성 상품은 클라이언트가 Transaction.finish() 로 마감한다. 서버 처리 없음.
    }

    @Override
    public AppleNotification decode(String signedPayload) {
        // 알림은 어느 환경에서든 올 수 있으므로 기본 환경으로 먼저, 실패하면 다른 환경으로 검증한다.
        Environment primary = primaryEnvironment();
        Environment secondary = primary == Environment.PRODUCTION ? Environment.SANDBOX : Environment.PRODUCTION;
        ResponseBodyV2DecodedPayload body;
        Environment resolved = primary;
        try {
            body = verifier(primary).verifyAndDecodeNotification(signedPayload);
        } catch (VerificationException primaryFailure) {
            try {
                body = verifier(secondary).verifyAndDecodeNotification(signedPayload);
                resolved = secondary;
            } catch (VerificationException e) {
                throw new StorePurchaseInvalidException("Apple 서버 알림 서명 검증 실패: " + primaryFailure.getStatus(), primaryFailure);
            }
        }

        Data data = body.getData();
        JWSTransactionDecodedPayload transaction = null;
        if (data != null && StringUtils.hasText(data.getSignedTransactionInfo())) {
            Environment txEnv = data.getEnvironment() != null ? data.getEnvironment() : resolved;
            transaction = decodeTransaction(txEnv, data.getSignedTransactionInfo());
        }
        return new AppleNotification(
                body.getRawNotificationType(),
                body.getRawSubtype(),
                body.getNotificationUUID(),
                data == null ? resolved.getValue() : data.getRawEnvironment(),
                transaction == null ? null : transaction.getTransactionId(),
                transaction == null ? null : transaction.getOriginalTransactionId(),
                transaction == null ? null : transaction.getProductId(),
                transaction == null || transaction.getRevocationDate() == null
                        ? null : Instant.ofEpochMilli(transaction.getRevocationDate()),
                transaction == null ? null : transaction.getRawRevocationReason() == null
                        ? null : String.valueOf(transaction.getRawRevocationReason())
        );
    }

    private String fetchSignedTransaction(Environment environment, String transactionId) {
        try {
            TransactionInfoResponse response = apiClient(environment).getTransactionInfo(transactionId);
            if (response == null || !StringUtils.hasText(response.getSignedTransactionInfo())) {
                throw new StoreGatewayException("Apple 이 빈 거래 응답을 반환했습니다.");
            }
            return response.getSignedTransactionInfo();
        } catch (APIException e) {
            APIError error = e.getApiError();
            if (error == APIError.TRANSACTION_ID_NOT_FOUND
                    || error == APIError.ORIGINAL_TRANSACTION_ID_NOT_FOUND
                    || error == APIError.INVALID_TRANSACTION_ID) {
                throw new TransactionNotFound();
            }
            if (e.getHttpStatusCode() >= 400 && e.getHttpStatusCode() < 500
                    && error != APIError.RATE_LIMIT_EXCEEDED
                    && error != APIError.ACCOUNT_NOT_FOUND_RETRYABLE
                    && error != APIError.APP_NOT_FOUND_RETRYABLE
                    && error != APIError.ORIGINAL_TRANSACTION_ID_NOT_FOUND_RETRYABLE) {
                throw new StorePurchaseInvalidException(
                        "Apple 거래 조회 거절(" + e.getHttpStatusCode() + "): " + e.getApiErrorMessage(), e);
            }
            throw new StoreGatewayException(
                    "Apple 거래 조회 실패(" + e.getHttpStatusCode() + "): " + e.getApiErrorMessage(), e);
        } catch (IOException e) {
            throw new StoreGatewayException("Apple App Store Server API 통신 실패", e);
        }
    }

    private JWSTransactionDecodedPayload decodeTransaction(Environment environment, String signed) {
        try {
            return verifier(environment).verifyAndDecodeTransaction(signed);
        } catch (VerificationException e) {
            throw new StorePurchaseInvalidException("Apple 거래 서명 검증 실패: " + e.getStatus(), e);
        }
    }

    private StorePurchase toStorePurchase(JWSTransactionDecodedPayload payload, Environment environment, String signed) {
        StorePurchaseState state = payload.getRevocationDate() != null
                ? StorePurchaseState.CANCELED
                : StorePurchaseState.PURCHASED;
        StoreEnvironment storeEnvironment = payload.getEnvironment() == Environment.PRODUCTION
                || (payload.getEnvironment() == null && environment == Environment.PRODUCTION)
                ? StoreEnvironment.PRODUCTION
                : StoreEnvironment.SANDBOX;
        // Apple price 는 통화 단위 * 1000 (milliunits)
        BigDecimal amount = payload.getPrice() == null
                ? null
                : BigDecimal.valueOf(payload.getPrice()).movePointLeft(3);
        return new StorePurchase(
                PurchaseStore.APPLE,
                payload.getProductId(),
                payload.getTransactionId(),
                payload.getOriginalTransactionId(),
                state,
                storeEnvironment,
                payload.getQuantity() == null ? 1 : payload.getQuantity(),
                payload.getPurchaseDate() == null ? null : Instant.ofEpochMilli(payload.getPurchaseDate()),
                payload.getAppAccountToken() == null ? null : payload.getAppAccountToken().toString(),
                amount,
                payload.getCurrency(),
                false,
                signed
        );
    }

    private Environment primaryEnvironment() {
        String value = properties.getEnvironment();
        if (!StringUtils.hasText(value)) {
            return Environment.SANDBOX;
        }
        return "PRODUCTION".equals(value.trim().toUpperCase(Locale.ROOT))
                ? Environment.PRODUCTION
                : Environment.SANDBOX;
    }

    private AppStoreServerAPIClient apiClient(Environment environment) {
        return apiClients.computeIfAbsent(environment, env -> {
            requireText(properties.getIssuerId(), "APPLE_APP_STORE_ISSUER_ID");
            requireText(properties.getKeyId(), "APPLE_APP_STORE_KEY_ID");
            requireText(properties.getBundleId(), "APPLE_APP_STORE_BUNDLE_ID");
            return new AppStoreServerAPIClient(
                    loadPrivateKey(),
                    properties.getKeyId().trim(),
                    properties.getIssuerId().trim(),
                    properties.getBundleId().trim(),
                    env
            );
        });
    }

    private SignedDataVerifier verifier(Environment environment) {
        return verifiers.computeIfAbsent(environment, env -> {
            requireText(properties.getBundleId(), "APPLE_APP_STORE_BUNDLE_ID");
            if (env == Environment.PRODUCTION && properties.getAppAppleId() == null) {
                throw new IllegalStateException("APPLE_APP_STORE_APP_APPLE_ID 설정이 필요합니다.");
            }
            Set<InputStream> roots = new HashSet<>();
            for (byte[] cert : rootCertificates()) {
                roots.add(new ByteArrayInputStream(cert));
            }
            return new SignedDataVerifier(
                    roots,
                    properties.getBundleId().trim(),
                    properties.getAppAppleId(),
                    env,
                    properties.isEnableOnlineChecks()
            );
        });
    }

    private byte[][] rootCertificates() {
        byte[][] cached = rootCertificates;
        if (cached != null) {
            return cached;
        }
        requireText(properties.getRootCertificatesPath(), "APPLE_ROOT_CERTIFICATES_PATH");
        Path dir = Path.of(properties.getRootCertificatesPath().trim());
        try (Stream<Path> files = Files.list(dir)) {
            byte[][] loaded = files
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".cer") || name.endsWith(".der") || name.endsWith(".crt") || name.endsWith(".pem");
                    })
                    .sorted()
                    .map(path -> {
                        try {
                            return Files.readAllBytes(path);
                        } catch (IOException e) {
                            throw new IllegalStateException("Apple Root CA 파일을 읽을 수 없습니다: " + path, e);
                        }
                    })
                    .toArray(byte[][]::new);
            if (loaded.length == 0) {
                throw new IllegalStateException("Apple Root CA 인증서가 없습니다: " + dir
                        + " (https://www.apple.com/certificateauthority/ 에서 Apple Root CA .cer 파일들을 내려받아 두세요)");
            }
            rootCertificates = loaded;
            return loaded;
        } catch (IOException e) {
            throw new IllegalStateException("Apple Root CA 디렉터리를 읽을 수 없습니다: " + dir, e);
        }
    }

    private String loadPrivateKey() {
        if (StringUtils.hasText(properties.getPrivateKey())) {
            return properties.getPrivateKey().trim().replace("\\n", "\n");
        }
        if (StringUtils.hasText(properties.getPrivateKeyPath())) {
            try {
                return Files.readString(Path.of(properties.getPrivateKeyPath().trim()));
            } catch (IOException e) {
                throw new IllegalStateException("Apple 인앱 구입 키(.p8)를 읽을 수 없습니다: " + properties.getPrivateKeyPath(), e);
            }
        }
        throw new IllegalStateException("APPLE_APP_STORE_PRIVATE_KEY 또는 APPLE_APP_STORE_PRIVATE_KEY_PATH 설정이 필요합니다.");
    }

    private static void requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(name + " 설정이 필요합니다.");
        }
    }

    private static final class TransactionNotFound extends RuntimeException {
        private TransactionNotFound() {
            super(null, null, false, false);
        }
    }
}
