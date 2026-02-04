package mannabom_server.manabom.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS 관련 설정
 */
@Configuration
@Slf4j
public class AwsConfig {

    @Value("${app.aws.region}")
    private String region;

    @Value("${app.aws.s3.access-key}")
    private String accessKey;

    @Value("${app.aws.s3.secret-key}")
    private String secretKey;

    /**
     * S3Client Bean 등록
     */
    @Bean
    public S3Client s3Client() {
        try {
            S3Client client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)
                    ))
                    .build();

            log.info("S3Client 초기화 완료 - 리전: {}", region);
            return client;

        } catch (Exception e) {
            log.error("S3Client 초기화 실패", e);
            throw new RuntimeException("S3 설정이 올바르지 않습니다.", e);
        }
    }
}