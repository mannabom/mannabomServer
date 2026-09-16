package mannabom_server.manabom.application.gifticon.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.gifticon.port.GifticonTemplateProvider;
import mannabom_server.manabom.domain.gifticon.vo.GifticonTemplateSnapshot;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class GifticonCatalogSynchronizer {

    private final GifticonTemplateProvider gifticonTemplateProvider;
    private final GifticonCatalogService gifticonCatalogService;
    private final AtomicBoolean synchronizing = new AtomicBoolean(false);

    public SynchronizationResult synchronize() {
        if (!synchronizing.compareAndSet(false, true)) {
            throw new IllegalStateException("기프티콘 상품 동기화가 이미 진행 중입니다.");
        }

        try {
            List<GifticonTemplateSnapshot> templates =
                    gifticonTemplateProvider.findAliveTemplates();
            int synchronizedCount =
                    gifticonCatalogService.synchronizeAliveTemplates(templates);
            return new SynchronizationResult(synchronizedCount, Instant.now());
        } finally {
            synchronizing.set(false);
        }
    }

    public record SynchronizationResult(
            int synchronizedCount,
            Instant synchronizedAt
    ) {
    }
}
