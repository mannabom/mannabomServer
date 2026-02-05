package mannabom_server.manabom.presentation.partner.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.partner.dto.request.*;
import mannabom_server.manabom.application.partner.dto.response.*;
import mannabom_server.manabom.application.partner.service.PartnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class PartnerController {
    private final PartnerService partnerService;

    @PostMapping("/profile/detail")
    public ResponseEntity<GetTargetProfileDetailResponseDto> getTargetProfileDetail(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid GetTargetProfileDetailRequestDto request
            ){
        return ResponseEntity.ok(partnerService.getTargetProfileDetail(userId, request));
    }

    @PostMapping("/loveview/detail")
    public ResponseEntity<GetTargetLoveViewDetailResponseDto> getTargetLoveViewDetail(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid GetTargetLoveViewDetailRequestDto request
            ){
        return ResponseEntity.ok(partnerService.getTargetLoveViewDetail(userId, request.getTargetProfileId()));
    }

    @PostMapping("/profile/detail/extra_photo")
    public ResponseEntity<UnlockTargetPhotoResponseDto> unlockTargetPhoto(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid UnlockTargetPhotoRequestDto request
            ){
        return ResponseEntity.ok(partnerService.unlockTargetPhoto(userId, request));
    }

    @PostMapping("/extra_profile/ting")
    public ResponseEntity<Void> purchaseAdditionalProfileByTing(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PurchaseAdditionalProfileByTingRequestDto request
            ){
        partnerService.purchaseAdditionalProfileByTing(userId, request);

        return ResponseEntity.status(200).build();
    }

    @PostMapping("/score/received")
    public ResponseEntity<GetReceivedScoreResponseDto> getReceivedScore(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid GetReceivedScoreRequestDto request
            ) {
        return ResponseEntity.ok(partnerService.getReceivedScore(userId, request));
    }

    @PostMapping("/score/isReceived")
    public ResponseEntity<CheckReceivedScoreResponseDto> checkReceivedScore(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CheckReceivedScoreRequestDto request
    ){
        return ResponseEntity.ok(partnerService.checkReceivedScore(userId, request.getTargetProfileId()));
    }
}
