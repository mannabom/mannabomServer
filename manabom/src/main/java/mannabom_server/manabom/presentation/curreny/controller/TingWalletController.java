package mannabom_server.manabom.presentation.curreny.controller;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.currency.service.TingWalletService;
import mannabom_server.manabom.application.currency.dto.response.CheckTingWalletResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class TingWalletController {
    private final TingWalletService tingWalletService;

    @GetMapping("/check_tingwallet")
    public ResponseEntity<CheckTingWalletResponseDto> checkFreeContact(
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(tingWalletService.checkTingWallet(userId));
    }
}
