package mannabom_server.manabom.presentation.common.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 서버 상태 확인용 헬스체크 API
 */

@RestController
@Slf4j
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "mannabom-server");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "서버가 정상적으로 실행 중입니다.");

        log.debug("헬스체크 요청 처리 완료");
        return ResponseEntity.ok(response);
    }
}
