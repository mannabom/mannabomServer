package mannabom_server.manabom.infrastructure.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;

@Configuration
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp(@Value("${fcm.service-account-path}") String path) throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) return FirebaseApp.getInstance();

        try (FileInputStream in = new FileInputStream(path)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }
}
