package mannabom_server.manabom.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mannabom_server.manabom.application.meeting.dto.response.MatchedChatRoomInfo;
import mannabom_server.manabom.domain.user.enums.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    private final ObjectMapper objectMapper = objectMapper();

    @Test
    void serializesExternalLongIdsAsStrings() throws Exception {
        ExternalPayload payload = new ExternalPayload(
                10L,
                List.of(20L, 21L),
                Map.of("roomId", 30L),
                2L
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(payload));

        assertThat(json.get("userId").asText()).isEqualTo("10");
        assertThat(json.get("recipientUserIds").get(0).asText()).isEqualTo("20");
        assertThat(json.get("data").get("roomId").asText()).isEqualTo("30");
        assertThat(json.get("totalCount").isIntegralNumber()).isTrue();
    }

    @Test
    void includesProfileIdInMatchedChatParticipant() throws Exception {
        MatchedChatRoomInfo.Participant participant =
                new MatchedChatRoomInfo.Participant(
                        10L,
                        100L,
                        Gender.MALE,
                        "봄이",
                        "profile.jpg"
                );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(participant));

        assertThat(json.get("userId").asText()).isEqualTo("10");
        assertThat(json.get("profileId").asText()).isEqualTo("100");
    }

    @Test
    void acceptsStringIdInIncomingJson() throws Exception {
        IncomingPayload payload = objectMapper.readValue(
                "{\"targetId\":\"10\"}",
                IncomingPayload.class
        );

        assertThat(payload.targetId()).isEqualTo(10L);
    }

    private ObjectMapper objectMapper() {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        new JacksonConfig().longAsStringCustomizer().customize(builder);
        return builder.build();
    }

    private record ExternalPayload(
            Long userId,
            List<Long> recipientUserIds,
            Map<String, Object> data,
            long totalCount
    ) {
    }

    private record IncomingPayload(Long targetId) {
    }
}
