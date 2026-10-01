package com.sherrycardsshop.api.media.storage;

import java.net.URI;

import com.sherrycardsshop.api.media.config.StorageProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
class R2ImageStorage implements ImageStorage {

    // Cada subida usa una clave nueva, así que el navegador y Cloudflare pueden cachearla para siempre.
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final StorageProperties properties;
    private final S3Client client;

    R2ImageStorage(StorageProperties properties) {
        this.properties = properties;
        this.client = properties.enabled() ? createClient(properties) : null;
    }

    @Override
    public boolean enabled() {
        return client != null;
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        client.putObject(PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(contentType)
                .cacheControl(CACHE_CONTROL)
                .build(), RequestBody.fromBytes(content));
    }

    @Override
    public void delete(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(properties.bucket()).key(key).build());
    }

    @Override
    public String publicUrl(String key) {
        String base = properties.publicUrl().strip();
        return (base.endsWith("/") ? base : base + "/") + key;
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
    }

    private static S3Client createClient(StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint().strip()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKeyId().strip(), properties.secretAccessKey().strip())))
                .httpClient(UrlConnectionHttpClient.create())
                // R2 no admite todas las sumas de verificación que el SDK envía por defecto desde la 2.30.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }
}
