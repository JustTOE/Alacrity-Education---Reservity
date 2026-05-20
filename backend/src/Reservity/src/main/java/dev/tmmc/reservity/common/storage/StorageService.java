package dev.tmmc.reservity.common.storage;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

public interface StorageService {

    StoredObject upload(InputStream content, long contentLength, String contentType, String key);

    void delete(String key);

    String publicUrl(String key);

    URL presignPut(String key, String contentType, Duration ttl);

    URL presignGet(String key, Duration ttl);
}
