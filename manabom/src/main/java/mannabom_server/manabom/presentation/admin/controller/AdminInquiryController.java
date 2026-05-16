package mannabom_server.manabom.presentation.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.inquiry.dto.request.AnswerInquiryRequest;
import mannabom_server.manabom.application.inquiry.dto.response.InquiryResponse;
import mannabom_server.manabom.application.inquiry.service.InquiryService;
import mannabom_server.manabom.domain.inquiry.enums.InquiryStatus;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/inquiries")
public class AdminInquiryController {
    private final InquiryService inquiryService;

    @GetMapping
    public List<InquiryResponse> getInquiries(@AuthenticationPrincipal AdminPrincipal principal,
                                              @RequestParam(required = false) InquiryStatus status) {
        return inquiryService.getAdminInquiries(principal, status);
    }

    @GetMapping("/{inquiryId}")
    public InquiryResponse getInquiry(@AuthenticationPrincipal AdminPrincipal principal,
                                      @PathVariable Long inquiryId) {
        return inquiryService.getAdminInquiry(principal, inquiryId);
    }

    @PatchMapping("/{inquiryId}/answer")
    public InquiryResponse answer(@AuthenticationPrincipal AdminPrincipal principal,
                                  @PathVariable Long inquiryId,
                                  @Valid @RequestBody AnswerInquiryRequest request) {
        return inquiryService.answer(principal, inquiryId, request);
    }
}
