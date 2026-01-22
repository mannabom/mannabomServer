package mannabom_server.manabom.domain.pushMessage;

import java.util.Map;

public record PushMessage(String title, String body, Map<String, String> data) {}

