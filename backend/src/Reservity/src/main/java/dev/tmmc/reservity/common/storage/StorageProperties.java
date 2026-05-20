package dev.tmmc.reservity.common.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "reservity.storage.s3")
public class StorageProperties {

    private String bucket;
    private String region = "eu-central-1";
    private String endpointOverride;
    private boolean pathStyleAccess = false;
    private String publicBaseUrl;
    private String accessKey;
    private String secretKey;
}
