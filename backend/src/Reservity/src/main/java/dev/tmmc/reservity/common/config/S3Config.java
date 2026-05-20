package dev.tmmc.reservity.common.config;

import dev.tmmc.reservity.common.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final StorageProperties storageProperties;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(storageProperties.getRegion()))
                .credentialsProvider(credentialsProvider());

        if (storageProperties.getEndpointOverride() != null && !storageProperties.getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(storageProperties.getEndpointOverride()));
        }
        if (storageProperties.isPathStyleAccess()) {
            builder.forcePathStyle(true);
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(storageProperties.getRegion()))
                .credentialsProvider(credentialsProvider());

        if (storageProperties.getEndpointOverride() != null && !storageProperties.getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(storageProperties.getEndpointOverride()));
        }
        return builder.build();
    }

    /**
     * In dev/test we point at MinIO with hardcoded credentials. In prod the env
     * doesn't define {@code reservity.storage.s3.access-key} so we fall back to
     * the AWS SDK default chain (env vars, instance profile, ECS role, etc.).
     */
    private AwsCredentialsProvider credentialsProvider() {
        String accessKey = storageProperties.getAccessKey();
        String secretKey = storageProperties.getSecretKey();
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        return DefaultCredentialsProvider.create();
    }
}
