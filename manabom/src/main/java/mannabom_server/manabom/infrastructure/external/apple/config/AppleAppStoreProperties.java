package mannabom_server.manabom.infrastructure.external.apple.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * App Store Server API / Server Notifications 설정.
 * App Store Connect > 사용자 및 액세스 > 통합 > 인앱 구입 키에서 발급한 .p8 키를 사용한다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.apple.app-store")
public class AppleAppStoreProperties {

    /** App Store Connect 인앱 구입 키의 Issuer ID */
    private String issuerId;
    /** 인앱 구입 키의 Key ID */
    private String keyId;
    /** .p8 파일 내용(PEM). 환경변수에 넣을 때는 줄바꿈을 \n 으로 이스케이프해도 된다 */
    private String privateKey;
    /** .p8 파일 경로. privateKey 가 비어 있으면 이 경로에서 읽는다 */
    private String privateKeyPath;
    /** iOS 앱 Bundle ID */
    private String bundleId;
    /** App Store Connect 의 Apple ID(숫자). PRODUCTION 검증에 필수 */
    private Long appAppleId;
    /** 기본 환경: PRODUCTION | SANDBOX */
    private String environment = "SANDBOX";
    /** PRODUCTION 에서 거래를 못 찾으면 SANDBOX 로 재조회(심사/TestFlight 대응) */
    private boolean sandboxFallbackEnabled = true;
    /** Apple Root CA(.cer) 파일들이 있는 디렉터리. https://www.apple.com/certificateauthority/ */
    private String rootCertificatesPath;
    /** OCSP 온라인 인증서 폐기 확인. 지연이 생기므로 기본 false */
    private boolean enableOnlineChecks = false;
}
