package mannabom_server.manabom.infrastructure.external.google.play.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Google Play Developer API 설정.
 * Play Console > 설정 > API 액세스에서 연결한 GCP 프로젝트의 서비스 계정 키(JSON)를 사용한다.
 * 서비스 계정에는 Play Console 사용자 권한 "주문 및 구독 관리"(재무 데이터 보기 포함)가 필요하다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.google-play")
public class GooglePlayProperties {

    /** 안드로이드 앱 패키지명 */
    private String packageName;
    /** 서비스 계정 키 JSON 내용. 비어 있으면 serviceAccountPath 에서 읽는다 */
    private String serviceAccountJson;
    /** 서비스 계정 키 JSON 파일 경로 */
    private String serviceAccountPath;
    /** API 호출 시 User-Agent 로 보내는 이름 */
    private String applicationName = "mannabom-server";
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 10;
}
