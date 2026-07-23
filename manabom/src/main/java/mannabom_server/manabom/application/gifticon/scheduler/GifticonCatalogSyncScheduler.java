package mannabom_server.manabom.application.gifticon.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.gifticon.port.GifticonTemplateProvider;
import mannabom_server.manabom.application.gifticon.service.GifticonCatalogService;
import mannabom_server.manabom.domain.gifticon.vo.GifticonTemplateSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.kakao.giftbiz.sync",
        name = "enabled",
        havingValue = "true"
)
public class GifticonCatalogSyncScheduler {

    private final GifticonTemplateProvider gifticonTemplateProvider;
    private final GifticonCatalogService gifticonCatalogService;

    @Scheduled(
            initialDelayString = "${app.kakao.giftbiz.sync.initial-delay:10000}",
            fixedDelayString = "${app.kakao.giftbiz.sync.fixed-delay:1800000}"
    )
    public void synchronize() {
        try {
            List<GifticonTemplateSnapshot> templates =
                    gifticonTemplateProvider.findAliveTemplates();
            int synchronizedCount =
                    gifticonCatalogService.synchronizeAliveTemplates(templates);
            log.info("[Gift Biz] 템플릿 DB 동기화 완료: {}개", synchronizedCount);
        } catch (Exception e) {
            log.error("[Gift Biz] 템플릿 동기화 실패. 기존 상품 DB를 유지합니다.", e);
        }
    }
}
