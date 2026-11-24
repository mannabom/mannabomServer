package mannabom_server.manabom.infrastructure.external.google.play;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.api.services.androidpublisher.model.ProductPurchase;
import com.google.api.services.androidpublisher.model.VoidedPurchase;
import com.google.api.services.androidpublisher.model.VoidedPurchasesListResponse;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.tingPurchase.port.GooglePlayVoidedPurchaseSource;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway;
import mannabom_server.manabom.application.tingPurchase.port.StoreGatewayException;
import mannabom_server.manabom.application.tingPurchase.port.StorePurchaseInvalidException;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.enums.StoreEnvironment;
import mannabom_server.manabom.infrastructure.external.google.play.config.GooglePlayProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Google Play Developer API(androidpublisher v3) 로 소모성 상품 구매를 조회·소모하고 환불 목록을 가져온다.
 * 설정이 없어도 애플리케이션은 기동되어야 하므로 클라이언트는 첫 사용 시 지연 생성한다.
 */
@Slf4j
@Component
public class GooglePlayClient implements InAppPurchaseGateway, GooglePlayVoidedPurchaseSource {

    private static final int PURCHASE_STATE_PURCHASED = 0;
    private static final int PURCHASE_STATE_CANCELED = 1;
    private static final int PURCHASE_STATE_PENDING = 2;
    private static final int CONSUMPTION_STATE_CONSUMED = 1;
    private static final int VOIDED_TYPE_ONE_TIME = 0;

    private final GooglePlayProperties properties;
    private volatile AndroidPublisher publisher;

    public GooglePlayClient(GooglePlayProperties properties) {
        this.properties = properties;
    }

    @Override
    public PurchaseStore store() {
        return PurchaseStore.GOOGLE;
    }

    @Override
    public StorePurchase verify(VerifyPurchaseCommand command) {
        ProductPurchase purchase;
        try {
            purchase = publisher().purchases().products()
                    .get(packageName(), command.storeProductId(), command.purchaseKey())
                    .execute();
        } catch (GoogleJsonResponseException e) {
            throw translate(e, "Google Play 구매 조회");
        } catch (IOException e) {
            throw new StoreGatewayException("Google Play Developer API 통신 실패", e);
        }
        if (purchase == null) {
            throw new StoreGatewayException("Google Play 가 빈 구매 응답을 반환했습니다.");
        }

        Integer state = purchase.getPurchaseState();
        StorePurchaseState purchaseState;
        if (state == null || state == PURCHASE_STATE_PURCHASED) {
            purchaseState = StorePurchaseState.PURCHASED;
        } else if (state == PURCHASE_STATE_PENDING) {
            purchaseState = StorePurchaseState.PENDING;
        } else if (state == PURCHASE_STATE_CANCELED) {
            purchaseState = StorePurchaseState.CANCELED;
        } else {
            throw new StorePurchaseInvalidException("알 수 없는 Google Play 구매 상태입니다: " + state);
        }

        // purchaseType: null=일반, 0=라이선스 테스터 테스트 구매, 1=프로모션, 2=리워드
        StoreEnvironment environment = purchase.getPurchaseType() != null && purchase.getPurchaseType() == 0
                ? StoreEnvironment.SANDBOX
                : StoreEnvironment.PRODUCTION;
        String productId = StringUtils.hasText(purchase.getProductId())
                ? purchase.getProductId()
                : command.storeProductId();

        return new StorePurchase(
                PurchaseStore.GOOGLE,
                productId,
                purchase.getOrderId(),
                null,
                purchaseState,
                environment,
                purchase.getQuantity() == null ? 1 : Math.max(1, purchase.getQuantity()),
                purchase.getPurchaseTimeMillis() == null ? null : Instant.ofEpochMilli(purchase.getPurchaseTimeMillis()),
                purchase.getObfuscatedExternalAccountId(),
                null,
                null,
                purchase.getConsumptionState() != null && purchase.getConsumptionState() == CONSUMPTION_STATE_CONSUMED,
                safeJson(purchase)
        );
    }

    @Override
    public void consume(ConsumePurchaseCommand command) {
        try {
            publisher().purchases().products()
                    .consume(packageName(), command.storeProductId(), command.purchaseKey())
                    .execute();
        } catch (GoogleJsonResponseException e) {
            throw translate(e, "Google Play 구매 consume");
        } catch (IOException e) {
            throw new StoreGatewayException("Google Play Developer API 통신 실패", e);
        }
    }

    @Override
    public List<VoidedPurchase> listVoidedPurchases(Instant since) {
        List<VoidedPurchase> result = new ArrayList<>();
        String pageToken = null;
        try {
            do {
                AndroidPublisher.Purchases.Voidedpurchases.List request = publisher().purchases()
                        .voidedpurchases()
                        .list(packageName())
                        .setType(VOIDED_TYPE_ONE_TIME)
                        .setStartTime(since.toEpochMilli());
                if (pageToken != null) {
                    request.setToken(pageToken);
                }
                VoidedPurchasesListResponse response = request.execute();
                if (response != null && response.getVoidedPurchases() != null) {
                    for (com.google.api.services.androidpublisher.model.VoidedPurchase voided : response.getVoidedPurchases()) {
                        result.add(new VoidedPurchase(
                                voided.getPurchaseToken(),
                                voided.getOrderId(),
                                voided.getVoidedTimeMillis() == null ? null : Instant.ofEpochMilli(voided.getVoidedTimeMillis()),
                                describeVoidedReason(voided.getVoidedReason(), voided.getVoidedSource())
                        ));
                    }
                }
                pageToken = response == null || response.getTokenPagination() == null
                        ? null
                        : response.getTokenPagination().getNextPageToken();
            } while (StringUtils.hasText(pageToken));
        } catch (GoogleJsonResponseException e) {
            throw translate(e, "Google Play voided purchases 조회");
        } catch (IOException e) {
            throw new StoreGatewayException("Google Play Developer API 통신 실패", e);
        }
        return result;
    }

    private RuntimeException translate(GoogleJsonResponseException e, String action) {
        int status = e.getStatusCode();
        String message = e.getDetails() == null ? e.getStatusMessage() : e.getDetails().getMessage();
        if (status == 400 || status == 404 || status == 410) {
            return new StorePurchaseInvalidException(action + " 거절(" + status + "): " + message, e);
        }
        if (status == 401 || status == 403) {
            // 서비스 계정 권한 문제는 운영 설정 오류이므로 구매 자체를 실패로 마감하지 않는다.
            return new StoreGatewayException(action + " 권한 오류(" + status + "): " + message, e);
        }
        return new StoreGatewayException(action + " 실패(" + status + "): " + message, e);
    }

    private static String describeVoidedReason(Integer reason, Integer source) {
        String reasonText = reason == null ? "UNKNOWN" : switch (reason) {
            case 0 -> "OTHER";
            case 1 -> "REMORSE";
            case 2 -> "NOT_RECEIVED";
            case 3 -> "DEFECTIVE";
            case 4 -> "ACCIDENTAL_PURCHASE";
            case 5 -> "FRAUD";
            case 6 -> "FRIENDLY_FRAUD";
            case 7 -> "CHARGEBACK";
            case 8 -> "UNACKNOWLEDGED_PURCHASE";
            default -> "REASON_" + reason;
        };
        String sourceText = source == null ? "" : switch (source) {
            case 0 -> "/USER";
            case 1 -> "/DEVELOPER";
            case 2 -> "/GOOGLE";
            default -> "/SOURCE_" + source;
        };
        return "GOOGLE_VOIDED:" + reasonText + sourceText;
    }

    private static String safeJson(ProductPurchase purchase) {
        try {
            return purchase.toString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String packageName() {
        if (!StringUtils.hasText(properties.getPackageName())) {
            throw new IllegalStateException("GOOGLE_PLAY_PACKAGE_NAME 설정이 필요합니다.");
        }
        return properties.getPackageName().trim();
    }

    private AndroidPublisher publisher() {
        AndroidPublisher current = publisher;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (publisher == null) {
                publisher = buildPublisher();
            }
            return publisher;
        }
    }

    private AndroidPublisher buildPublisher() {
        try (InputStream credentialStream = openServiceAccount()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialStream)
                    .createScoped(AndroidPublisherScopes.ANDROIDPUBLISHER);
            HttpRequestInitializer authorizer = new HttpCredentialsAdapter(credentials);
            HttpRequestInitializer initializer = request -> {
                authorizer.initialize(request);
                request.setConnectTimeout(Math.max(1, properties.getConnectTimeoutSeconds()) * 1000);
                request.setReadTimeout(Math.max(1, properties.getReadTimeoutSeconds()) * 1000);
            };
            return new AndroidPublisher.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    initializer
            )
                    .setApplicationName(properties.getApplicationName())
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Google Play Developer API 클라이언트를 초기화할 수 없습니다.", e);
        }
    }

    private InputStream openServiceAccount() throws IOException {
        if (StringUtils.hasText(properties.getServiceAccountJson())) {
            return new ByteArrayInputStream(properties.getServiceAccountJson().getBytes(StandardCharsets.UTF_8));
        }
        if (StringUtils.hasText(properties.getServiceAccountPath())) {
            return Files.newInputStream(Path.of(properties.getServiceAccountPath().trim()));
        }
        throw new IllegalStateException(
                "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON 또는 GOOGLE_PLAY_SERVICE_ACCOUNT_PATH 설정이 필요합니다.");
    }
}
