package mannabom_server.manabom.application.pushService.service.pushSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.pushService.dto.response.PushBatchResult;
import mannabom_server.manabom.domain.deviceToken.DeviceToken;
import mannabom_server.manabom.domain.deviceToken.DeviceTokenRepository;
import mannabom_server.manabom.domain.pushMessage.PushMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushService {

    private final DeviceTokenRepository tokenRepo;
    private final PushSender sender;

    /**
     *
     * @param userId
     * @param msg
     * 해당 유저의 활성화된 device token을 대상으로 push 알람 전송
     * 토큰이 만료되었거나 앱이 삭제된 경우 해당 토큰은 비활성화 처리
     */
    @Async
    @Transactional
    public void sendToUser(Long userId, PushMessage msg) {
        List<String> tokens = tokenRepo.findAllByUserIdAndActiveTrue(userId)
                .stream().map(DeviceToken::getToken).toList();
        log.info("특정 유저 푸시 알람 전송\n전송 예정 디바이스 토큰 수 : {}", tokens.size());
        PushBatchResult result = sender.sendToTokens(tokens, msg);
        deactivateInvalid(result.invalidTokens());
        log.info("특정 유저 push 알람 서비스 계층 동작 완료");
    }

    /**
     *
     * @param msg
     * 전체 유저의 활성화된 device token을 대상으로 push 알람 전송
     * 토큰이 만료되었거나 앱이 삭제된 경우 해당 토큰은 비활성화 처리
     * 대용량 전송이므로 chunk로 쪼개서 전송
     */
    @Async
    @Transactional
    public void broadcastToAll(PushMessage msg) {
        List<String> allTokens = tokenRepo.findAllByActiveTrue()
                .stream().map(DeviceToken::getToken).toList();
        int total = allTokens.size();
        int success = 0, fail = 0;

        log.info("전체 유저 푸시 알람 전송\n전송 예정 디바이스 토큰 수 : {}", total);

        // 멀티캐스트는 제한이 있으니 안전하게 chunk
        for (List<String> chunk : chunk(allTokens, 500)) {
            PushBatchResult result = sender.sendToTokens(chunk, msg);
            success += result.successCount();
            fail += result.failureCount();
            deactivateInvalid(result.invalidTokens());
        }

        log.info("전체 유저 푸시 알람 전송 완료\n총 전송량 : {}\n성공 : {}\n실패 : {}", total, success, fail);
    }

    private void deactivateInvalid(List<String> invalidTokens) {
        if (invalidTokens == null || invalidTokens.isEmpty()) return;
        for (String t : invalidTokens) {
            tokenRepo.findByToken(t).ifPresent(DeviceToken::deactivate);
        }
    }

    private static <T> List<List<T>> chunk(List<T> list, int size) {
        List<List<T>> res = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            res.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return res;
    }
}

