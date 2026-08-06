package mannabom_server.manabom.application.gifticon.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.gifticon.dto.response.GifticonProductResponse;
import mannabom_server.manabom.application.gifticon.dto.response.GifticonProductSliceResponse;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.vo.GifticonTemplateSnapshot;
import mannabom_server.manabom.domain.gifticon.repository.GifticonProductRepository;
import mannabom_server.manabom.domain.gifticon.service.GifticonPriceCalculator;
import mannabom_server.manabom.policy.service.RuntimePolicyService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GifticonCatalogService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final int MAX_PAGE_SIZE = 100;

    private final GifticonProductRepository gifticonProductRepository;
    private final GifticonPriceCalculator gifticonPriceCalculator;
    private final RuntimePolicyService runtimePolicyService;
    private final Clock clock = Clock.system(KOREA_ZONE);

    @Transactional(readOnly = true)
    public GifticonProductSliceResponse getAvailableProducts(
            String category,
            Long cursor,
            int size
    ) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size는 1 이상 100 이하여야 합니다.");
        }
        if (cursor != null && cursor < 0) {
            throw new IllegalArgumentException("cursor는 0 이상이어야 합니다.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String normalizedCategory = StringUtils.hasText(category) ? category.trim() : null;
        long cursorId = cursor == null ? 0L : cursor;
        List<GifticonProduct> fetchedProducts = gifticonProductRepository
                .findAvailableProductsAfter(
                        now,
                        normalizedCategory,
                        cursorId,
                        PageRequest.of(0, size + 1)
                );

        boolean hasNext = fetchedProducts.size() > size;
        List<GifticonProduct> currentProducts = hasNext
                ? fetchedProducts.subList(0, size)
                : fetchedProducts;
        List<GifticonProductResponse> contents = currentProducts.stream()
                .map(GifticonProductResponse::from)
                .toList();
        Long nextCursor = hasNext && !currentProducts.isEmpty()
                ? currentProducts.get(currentProducts.size() - 1).getGifticonProductId()
                : null;

        return new GifticonProductSliceResponse(contents, nextCursor, hasNext);
    }

    @Transactional
    public int synchronizeAliveTemplates(List<GifticonTemplateSnapshot> fetchedTemplates) {
        Objects.requireNonNull(fetchedTemplates, "기프티콘 템플릿 목록은 null일 수 없습니다.");

        fetchedTemplates.forEach(this::validateTemplate);
        Map<Long, GifticonTemplateSnapshot> templatesByTraceId = fetchedTemplates.stream()
                .collect(Collectors.toMap(
                        GifticonTemplateSnapshot::templateTraceId,
                        Function.identity(),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));

        if (templatesByTraceId.isEmpty()) {
            throw new IllegalStateException("활성 템플릿이 0개이므로 동기화를 중단합니다.");
        }

        Map<Long, GifticonProduct> productsByTraceId = gifticonProductRepository
                .findAllByTemplateTraceIdIn(templatesByTraceId.keySet())
                .stream()
                .collect(Collectors.toMap(GifticonProduct::getTemplateTraceId, Function.identity()));

        Instant syncedAt = clock.instant();
        List<GifticonProduct> synchronizedProducts = templatesByTraceId.values().stream()
                .map(template -> synchronizeProduct(
                        productsByTraceId.computeIfAbsent(
                                template.templateTraceId(),
                                GifticonProduct::new
                        ),
                        template,
                        syncedAt
                ))
                .toList();

        gifticonProductRepository.saveAllAndFlush(synchronizedProducts);
        gifticonProductRepository.markProductsNotSeenSinceUnavailable(syncedAt);
        return synchronizedProducts.size();
    }

    private GifticonProduct synchronizeProduct(
            GifticonProduct gifticonProduct,
            GifticonTemplateSnapshot template,
            Instant syncedAt
    ) {
        gifticonProduct.synchronize(
                template.templateName(),
                template.startAt(),
                template.endAt(),
                template.status(),
                template.budgetType(),
                template.sentCount() == null ? 0L : template.sentCount(),
                template.senderName(),
                template.messageCardImageUrl(),
                template.messageCardText(),
                template.itemType(),
                template.productName(),
                template.brandName(),
                template.productImageUrl(),
                template.productThumbnailImageUrl(),
                template.brandImageUrl(),
                template.productPrice(),
                gifticonPriceCalculator.calculateSalePrice(
                        template.productPrice(),
                        runtimePolicyService.snapshot()
                                .getGifticon()
                                .getPricing()
                                .getMarkupPercent()
                ),
                syncedAt
        );
        return gifticonProduct;
    }

    private void validateTemplate(GifticonTemplateSnapshot template) {
        if (template == null) {
            throw new IllegalArgumentException("기프티콘 템플릿에 null 항목이 포함되어 있습니다.");
        }
        if (template.templateTraceId() == null) {
            throw new IllegalArgumentException("기프티콘 템플릿 식별자가 없습니다.");
        }
        if (!StringUtils.hasText(template.templateName())) {
            throw new IllegalArgumentException("기프티콘 템플릿 이름이 없습니다.");
        }
        if (!StringUtils.hasText(template.status())) {
            throw new IllegalArgumentException("기프티콘 템플릿 상태가 없습니다.");
        }
        if (!StringUtils.hasText(template.itemType())
                || !StringUtils.hasText(template.productName())
                || !StringUtils.hasText(template.brandName())) {
            throw new IllegalArgumentException("기프티콘 상품의 필수 정보가 누락되었습니다.");
        }
        if (template.productPrice() == null || template.productPrice() < 0) {
            throw new IllegalArgumentException("기프티콘 상품 가격이 올바르지 않습니다.");
        }
    }
}
