package mannabom_server.manabom.application.inquiry.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.inquiry.dto.request.AnswerInquiryRequest;
import mannabom_server.manabom.application.inquiry.dto.request.CreateInquiryRequest;
import mannabom_server.manabom.application.inquiry.dto.response.InquiryResponse;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.inquiry.entity.UserInquiry;
import mannabom_server.manabom.domain.inquiry.enums.InquiryCategory;
import mannabom_server.manabom.domain.inquiry.enums.InquiryStatus;
import mannabom_server.manabom.domain.inquiry.repository.UserInquiryRepository;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InquiryService {
    private final UserInquiryRepository userInquiryRepository;
    private final TingWalletRepository tingWalletRepository;
    private final EntityManager entityManager;

    @Transactional
    public InquiryResponse create(Long userId, CreateInquiryRequest request) {
        UserInquiry inquiry = UserInquiry.builder()
                .userId(userId)
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .relatedChatRoomId(request.getRelatedChatRoomId())
                .relatedMeetingId(request.getRelatedMeetingId())
                .relatedPaymentId(request.getRelatedPaymentId())
                .build();
        return toResponse(userInquiryRepository.save(inquiry), false);
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> getMine(Long userId) {
        return userInquiryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(inquiry -> toResponse(inquiry, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> getAdminInquiries(AdminPrincipal admin, InquiryStatus status) {
        requireSupport(admin);
        List<UserInquiry> inquiries = status == null
                ? userInquiryRepository.findAllByOrderByCreatedAtDesc()
                : userInquiryRepository.findByStatusOrderByCreatedAtDesc(status);
        return inquiries.stream().map(inquiry -> toResponse(inquiry, false)).toList();
    }

    @Transactional(readOnly = true)
    public InquiryResponse getAdminInquiry(AdminPrincipal admin, Long inquiryId) {
        requireSupport(admin);
        return toResponse(getInquiry(inquiryId), true);
    }

    @Transactional
    public InquiryResponse answer(AdminPrincipal admin, Long inquiryId, AnswerInquiryRequest request) {
        requireSupport(admin);
        UserInquiry inquiry = getInquiry(inquiryId);
        inquiry.answer(request.getAnswer(), admin.adminId());
        return toResponse(inquiry, true);
    }

    private UserInquiry getInquiry(Long inquiryId) {
        return userInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의 내역을 찾을 수 없습니다."));
    }

    private void requireSupport(AdminPrincipal admin) {
        if (!admin.hasAnyRole(AdminRole.SUPER_ADMIN, AdminRole.OPERATOR, AdminRole.SUPPORT)) {
            throw new IllegalStateException("문의 관리 권한이 없습니다.");
        }
    }

    private InquiryResponse toResponse(UserInquiry inquiry, boolean includeContext) {
        return InquiryResponse.builder()
                .inquiryId(inquiry.getInquiryId())
                .userId(inquiry.getUserId())
                .category(inquiry.getCategory())
                .status(inquiry.getStatus())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .relatedChatRoomId(inquiry.getRelatedChatRoomId())
                .relatedMeetingId(inquiry.getRelatedMeetingId())
                .relatedPaymentId(inquiry.getRelatedPaymentId())
                .answer(inquiry.getAnswer())
                .answeredByAdminId(inquiry.getAnsweredByAdminId())
                .answeredAt(inquiry.getAnsweredAt())
                .createdAt(inquiry.getCreatedAt())
                .relatedContext(includeContext ? relatedContext(inquiry) : Map.of())
                .build();
    }

    private Map<String, Object> relatedContext(UserInquiry inquiry) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (inquiry.getCategory() == InquiryCategory.CHAT && inquiry.getRelatedChatRoomId() != null) {
            List<ChatMessage> messages = entityManager.createQuery("""
                            select m from ChatMessage m
                            join fetch m.user
                            where m.room.id = :roomId
                            order by m.createdAt desc
                            """, ChatMessage.class)
                    .setParameter("roomId", inquiry.getRelatedChatRoomId())
                    .setMaxResults(50)
                    .getResultList();
            context.put("chatMessages", messages.stream()
                    .map(message -> Map.of(
                            "messageId", message.getId(),
                            "senderUserId", message.getUser().getUserId(),
                            "type", message.getType(),
                            "content", message.getContent(),
                            "createdAt", message.getCreatedAt()
                    ))
                    .toList());
        }
        if (inquiry.getCategory() == InquiryCategory.PAYMENT) {
            TingWallet wallet = tingWalletRepository.findByUserId(inquiry.getUserId()).orElse(null);
            if (wallet != null) {
                Map<String, Object> walletContext = new LinkedHashMap<>();
                walletContext.put("ting", wallet.getTing());
                walletContext.put("eventTing", wallet.getEventTing());
                walletContext.put("membershipActiveUntil", wallet.getMembershipActiveUntil());
                context.put("wallet", walletContext);
            }
            context.put("paymentHistory", "결제 내역 테이블 연결 전입니다. relatedPaymentId와 지갑 상태를 우선 제공합니다.");
        }
        if (inquiry.getCategory() == InquiryCategory.MATCHING) {
            context.put("profileRecommendations", entityManager.createQuery("""
                            select new map(
                                h.id as id,
                                h.requesterUserId as requesterUserId,
                                h.targetUserId as targetUserId,
                                h.recommendType as recommendType,
                                h.recommendedAt as recommendedAt
                            )
                            from ProfileRecommendHistory h
                            where h.requesterUserId = :userId or h.targetUserId = :userId
                            order by h.recommendedAt desc
                            """)
                    .setParameter("userId", inquiry.getUserId())
                    .setMaxResults(20)
                    .getResultList());
            context.put("loveViewRecommendations", entityManager.createQuery("""
                            select new map(
                                h.id as id,
                                h.requesterUserId as requesterUserId,
                                h.targetUserId as targetUserId,
                                h.recommendType as recommendType,
                                h.recommendedAt as recommendedAt
                            )
                            from LoveViewRecommendHistory h
                            where h.requesterUserId = :userId or h.targetUserId = :userId
                            order by h.recommendedAt desc
                            """)
                    .setParameter("userId", inquiry.getUserId())
                    .setMaxResults(20)
                    .getResultList());
        }
        return context;
    }
}
