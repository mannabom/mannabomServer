package mannabom_server.manabom;

import mannabom_server.manabom.application.pushService.service.pushSender.PushSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("local")
class ManabomApplicationTests {

	@MockitoBean
	PushSender pushSender;

	@Test
	void contextLoads() {
	}

}
