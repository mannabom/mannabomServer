package mannabom_server.manabom.application.admin.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminSendPushRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminPushResponse;
import mannabom_server.manabom.application.pushService.dto.response.PushBatchResult;
import mannabom_server.manabom.application.pushService.service.pushSender.PushSender;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.deviceToken.DeviceToken;
import mannabom_server.manabom.domain.deviceToken.DeviceTokenRepository;
import mannabom_server.manabom.domain.pushMessage.PushMessage;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPushService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final PushSender pushSender;
    private final AdminAuditService adminAuditService;

    @Transactional
    public AdminPushResponse sendPush(AdminPrincipal principal,
                                      AdminSendPushRequest request,
                                      String ipAddress) {
        requireAnyRole(principal, AdminRole.SUPER_ADMIN, AdminRole.OPERATOR);
        if (request.getTargetType() == AdminSendPushRequest.TargetType.USER && request.getUserId() == null) {
            throw new IllegalArgumentException("특정 회원에게 전송하려면 userId가 필요합니다.");
        }

        List<String> tokens = switch (request.getTargetType()) {
            case USER -> deviceTokenRepository.findAllByUserIdAndActiveTrue(request.getUserId())
                    .stream()
                    .map(DeviceToken::getToken)
                    .toList();
            case ALL -> deviceTokenRepository.findAllByActiveTrue()
                    .stream()
                    .map(DeviceToken::getToken)
                    .toList();
        };

        int successCount = 0;
        int failureCount = 0;
        List<String> invalidTokens = new ArrayList<>();
        PushMessage message = new PushMessage(request.getTitle(), request.getBody(), request.getData());

        for (List<String> chunk : chunk(tokens, 500)) {
            PushBatchResult result = pushSender.sendToTokens(chunk, message);
            successCount += result.successCount();
            failureCount += result.failureCount();
            invalidTokens.addAll(result.invalidTokens());
        }
        deactivateInvalid(invalidTokens);

        Long targetId = request.getTargetType() == AdminSendPushRequest.TargetType.USER ? request.getUserId() : null;
        String after = "target=" + request.getTargetType()
                + ", tokens=" + tokens.size()
                + ", success=" + successCount
                + ", failure=" + failureCount;
        adminAuditService.log(principal.adminId(), AdminAuditActionType.PUSH_SEND,
                AdminAuditTargetType.PUSH, targetId, null, after, request.getReason(), ipAddress);

        return AdminPushResponse.builder()
                .targetTokenCount(tokens.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .invalidTokenCount(invalidTokens.size())
                .build();
    }

    private void deactivateInvalid(List<String> invalidTokens) {
        if (invalidTokens == null || invalidTokens.isEmpty()) {
            return;
        }
        for (String token : invalidTokens) {
            deviceTokenRepository.findByToken(token).ifPresent(DeviceToken::deactivate);
        }
    }

    private static <T> List<List<T>> chunk(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return result;
    }

    private void requireAnyRole(AdminPrincipal admin, AdminRole... roles) {
        if (admin.hasAnyRole(roles)) {
            return;
        }
        throw new IllegalStateException("해당 관리자 권한으로 수행할 수 없는 작업입니다.");
    }
}
