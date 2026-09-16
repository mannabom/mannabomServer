package mannabom_server.manabom.infrastructure.external.kakao.giftbiz;

import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.gifticon.port.GifticonTemplateProvider;
import mannabom_server.manabom.domain.gifticon.vo.GifticonTemplateSnapshot;
import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.config.GiftbizProperties;
import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.dto.KakaoGiftbizTemplatePage;
import mannabom_server.manabom.infrastructure.external.kakao.giftbiz.dto.KakaoGiftbizTemplatePage.KakaoGiftbizTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KakaoGiftbizClient implements GifticonTemplateProvider {

    private static final String TEMPLATE_PATH = "/openapi/giftbiz/v1/template";
    private static final String ALIVE_STATUS = "ALIVE";

    private final GiftbizProperties properties;
    private final KakaoGiftbizTemplateMapper templateMapper;
    private final WebClient webClient;

    public KakaoGiftbizClient(
            GiftbizProperties properties,
            KakaoGiftbizTemplateMapper templateMapper
    ) {
        this.properties = properties;
        this.templateMapper = templateMapper;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public List<GifticonTemplateSnapshot> findAliveTemplates() {
        if (!StringUtils.hasText(properties.getAuthorization())) {
            throw new IllegalStateException("KAKAO_GIFTBIZ_AUTHORIZATION 설정이 필요합니다.");
        }

        int maxPages = properties.getSync().getMaxPages();
        if (maxPages <= 0) {
            throw new IllegalStateException("Gift Biz 최대 조회 페이지 수는 1 이상이어야 합니다.");
        }

        List<KakaoGiftbizTemplate> templates = new ArrayList<>();
        for (int page = 0; page < maxPages; page++) {
            KakaoGiftbizTemplatePage response = fetchPage(page);
            List<KakaoGiftbizTemplate> contents =
                    response.contents() == null ? List.of() : response.contents();
            templates.addAll(contents);

            if (Boolean.TRUE.equals(response.last())) {
                log.info("[Gift Biz] 활성 템플릿 조회 완료: {}개", templates.size());
                return mapValidTemplates(templates);
            }
            if (contents.isEmpty()) {
                throw new IllegalStateException(
                        "Gift Biz 템플릿 API가 last=false 상태에서 빈 페이지를 반환했습니다: " + page
                );
            }
        }

        throw new IllegalStateException(
                "Gift Biz 템플릿 조회가 최대 페이지 수를 초과했습니다: " + maxPages
        );
    }

    private List<GifticonTemplateSnapshot> mapValidTemplates(
            List<KakaoGiftbizTemplate> templates
    ) {
        List<GifticonTemplateSnapshot> snapshots = new ArrayList<>();
        for (KakaoGiftbizTemplate template : templates) {
            try {
                snapshots.add(templateMapper.toSnapshot(template));
            } catch (IllegalArgumentException e) {
                log.warn(
                        "[Gift Biz] 필수 상품 정보가 누락된 템플릿을 건너뜁니다. templateTraceId={}",
                        template == null ? null : template.templateTraceId(),
                        e
                );
            }
        }
        return snapshots;
    }

    private KakaoGiftbizTemplatePage fetchPage(int page) {
        KakaoGiftbizTemplatePage response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(TEMPLATE_PATH)
                        .queryParam("status", ALIVE_STATUS)
                        .queryParam("page", page)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, properties.getAuthorization())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        clientResponse -> clientResponse.createException()
                )
                .bodyToMono(KakaoGiftbizTemplatePage.class)
                .block(Duration.ofSeconds(properties.getRequestTimeoutSeconds()));

        if (response == null) {
            throw new IllegalStateException("Gift Biz 템플릿 API가 빈 응답을 반환했습니다.");
        }
        return response;
    }
}
