package mannabom_server.manabom.presentation.gifticon.controller;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.gifticon.dto.response.GifticonProductSliceResponse;
import mannabom_server.manabom.application.gifticon.service.GifticonCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gifticons")
public class GifticonController {

    private final GifticonCatalogService gifticonCatalogService;

    @GetMapping
    public ResponseEntity<GifticonProductSliceResponse> getGifticons(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                gifticonCatalogService.getAvailableProducts(category, cursor, size)
        );
    }
}
