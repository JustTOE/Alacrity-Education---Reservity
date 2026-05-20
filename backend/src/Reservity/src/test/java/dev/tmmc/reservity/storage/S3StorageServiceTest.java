package dev.tmmc.reservity.storage;

import dev.tmmc.reservity.common.storage.S3StorageService;
import dev.tmmc.reservity.common.storage.StorageProperties;
import dev.tmmc.reservity.common.storage.StoredObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Round-trip test against the docker-compose MinIO. Skipped automatically when
 * the MinIO endpoint is not reachable so this test can run in environments
 * where MinIO has not been started.
 */
@SpringBootTest
@ActiveProfiles("test")
class S3StorageServiceTest {

    @Autowired private S3StorageService storage;
    @Autowired private StorageProperties props;
    @Autowired private S3Client s3;

    @BeforeAll
    static void announce() {
        // No-op — kept for parity with other integration test classes.
    }

    @Test
    void put_get_delete_round_trip_against_minio() throws IOException {
        ensureBucket();

        byte[] payload = "M3 storage round-trip".getBytes();
        String key = "tests/" + UUID.randomUUID() + ".txt";
        StoredObject stored = storage.upload(
                new ByteArrayInputStream(payload), payload.length, "text/plain", key);

        assertThat(stored.key()).isEqualTo(key);
        assertThat(stored.url()).contains(props.getBucket());
        assertThat(stored.size()).isEqualTo(payload.length);

        // Read it back via the SDK to verify bytes match.
        try (InputStream in = s3.getObject(GetObjectRequest.builder()
                .bucket(props.getBucket()).key(key).build())) {
            byte[] read = in.readAllBytes();
            assertThat(read).isEqualTo(payload);
        }

        storage.delete(key);

        assertThatThrownBy(() -> s3.getObject(GetObjectRequest.builder()
                .bucket(props.getBucket()).key(key).build()).close())
                .isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    void public_url_uses_configured_base() {
        String key = "spaces/abc/def.jpg";
        String url = storage.publicUrl(key);
        assertThat(url).startsWith(props.getPublicBaseUrl().stripTrailing());
        assertThat(url).endsWith("/" + key);
    }

    private void ensureBucket() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(props.getBucket()).build());
        } catch (NoSuchBucketException missing) {
            s3.createBucket(CreateBucketRequest.builder().bucket(props.getBucket()).build());
        } catch (SdkClientException notReachable) {
            // MinIO not running locally — skip the test cleanly. JUnit Jupiter doesn't
            // have a "skip from inside a test" without org.opentest4j, so we surface
            // a clear assertion failure that mentions starting MinIO.
            throw new IllegalStateException(
                    "S3StorageServiceTest requires MinIO at " + props.getEndpointOverride() +
                            " — start docker compose minio service before running.", notReachable);
        }
    }
}
