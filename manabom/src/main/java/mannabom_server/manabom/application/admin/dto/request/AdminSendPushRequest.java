package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.Map;

@Getter
public class AdminSendPushRequest {
    @NotNull
    private TargetType targetType;

    private Long userId;

    @NotBlank
    private String title;

    @NotBlank
    private String body;

    private Map<String, String> data;

    private String reason;

    public enum TargetType {
        USER,
        ALL
    }
}
