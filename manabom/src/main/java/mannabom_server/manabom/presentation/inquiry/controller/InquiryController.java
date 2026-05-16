package mannabom_server.manabom.presentation.inquiry.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.inquiry.dto.request.CreateInquiryRequest;
import mannabom_server.manabom.application.inquiry.dto.response.InquiryResponse;
import mannabom_server.manabom.application.inquiry.service.InquiryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {
    private final InquiryService inquiryService;

    @PostMapping
    public InquiryResponse create(@AuthenticationPrincipal Long userId,
                                  @Valid @RequestBody CreateInquiryRequest request) {
        return inquiryService.create(userId, request);
    }

    @GetMapping("/me")
    public List<InquiryResponse> getMine(@AuthenticationPrincipal Long userId) {
        return inquiryService.getMine(userId);
    }
}
