package mannabom_server.manabom.tests;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.FileInputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FirebaseIntegrationTest {

    @Test
    @EnabledIfSystemProperty(named = "fcmConfigTest", matches = "true")
    void firebaseAppInitializesFromServiceAccountFile() throws Exception {
        String appName = "fcm-config-test-" + UUID.randomUUID();
        FirebaseApp app = initializeFirebaseApp(appName);

        try {
            assertThat(app.getName()).isEqualTo(appName);
            ServiceAccountCredentials serviceAccount = loadServiceAccountCredentials();
            assertThat(serviceAccount.getClientEmail()).contains("firebase-adminsdk");

            System.out.println("Firebase config test service account: " + serviceAccount.getClientEmail());
        } finally {
            app.delete();
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "fcmDryRunTest", matches = "true")
    void firebaseMessagingDryRunCanReachFcm() throws Exception {
        String token = System.getProperty("fcm.testToken", "");
        assumeTrue(!token.isBlank(), "Set -Pfcm.testToken=<device-token> to run the FCM dry-run test.");

        String appName = "fcm-dry-run-test-" + UUID.randomUUID();
        FirebaseApp app = initializeFirebaseApp(appName);

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle("mannabom fcm dry run")
                            .setBody("dry run validation only")
                            .build())
                    .putData("type", "dry-run")
                    .build();

            String messageId = FirebaseMessaging.getInstance(app).send(message, true);

            assertThat(messageId).isNotBlank();
            System.out.println("Firebase dry-run message id: " + messageId);
        } finally {
            app.delete();
        }
    }

    private FirebaseApp initializeFirebaseApp(String appName) throws Exception {
        ServiceAccountCredentials credentials = loadServiceAccountCredentials();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        return FirebaseApp.initializeApp(options, appName);
    }

    private ServiceAccountCredentials loadServiceAccountCredentials() throws Exception {
        String path = System.getProperty("fcm.serviceAccountPath", "");
        assumeTrue(!path.isBlank(), "Set -Pfcm.serviceAccountPath=<service-account-json> to run Firebase tests.");

        try (FileInputStream in = new FileInputStream(path)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in);
            assertThat(credentials).isInstanceOf(ServiceAccountCredentials.class);
            return (ServiceAccountCredentials) credentials;
        }
    }
}
