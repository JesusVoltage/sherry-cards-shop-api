package com.sherrycardsshop.api.media.service;

import java.io.IOException;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.sherrycardsshop.api.catalog.repository.ProductImageRepository;
import com.sherrycardsshop.api.common.exception.ApiException;
import com.sherrycardsshop.api.media.config.StorageProperties;
import com.sherrycardsshop.api.media.dto.MediaFileDto;
import com.sherrycardsshop.api.media.dto.StorageUsageDto;
import com.sherrycardsshop.api.media.entity.MediaFile;
import com.sherrycardsshop.api.media.repository.MediaFileRepository;
import com.sherrycardsshop.api.media.storage.ImageStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {

    private static final Logger logger = LoggerFactory.getLogger(MediaService.class);

    private final ImageStorage storage;
    private final MediaFileRepository mediaFileRepository;
    private final ProductImageRepository productImageRepository;
    private final StorageProperties properties;
    private final TransactionTemplate transactionTemplate;

    public MediaService(ImageStorage storage, MediaFileRepository mediaFileRepository,
                        ProductImageRepository productImageRepository, StorageProperties properties,
                        PlatformTransactionManager transactionManager) {
        this.storage = storage;
        this.mediaFileRepository = mediaFileRepository;
        this.productImageRepository = productImageRepository;
        this.properties = properties;
        // En afterCommit la transacción original sigue enlazada pero ya no confirma nada: hace falta una nueva.
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public MediaFileDto uploadProductImage(MultipartFile file, Long uploadedBy) {
        if (!storage.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "La subida de imágenes todavía no está configurada");
        }
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El archivo está vacío", "file");
        }
        if (file.getSize() > properties.maxImageSize().toBytes()) {
            throw tooLarge();
        }
        byte[] content = readBytes(file);
        ImageFormat format = ImageFormat.detect(content).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "Solo se admiten imágenes JPG, PNG o WebP", "file"));
        // El tope deja margen por debajo de los 10 GB gratuitos de R2: nunca se paga por almacenamiento.
        if (mediaFileRepository.sumSizeBytes() + content.length > properties.maxTotalSize().toBytes()) {
            throw new ApiException(HttpStatus.INSUFFICIENT_STORAGE,
                    "Se ha alcanzado el límite de almacenamiento de imágenes (" + properties.maxTotalSize().toGigabytes() + " GB)");
        }

        String key = "products/" + YearMonth.now(ZoneOffset.UTC) + "/" + UUID.randomUUID() + "." + format.extension();
        storage.put(key, content, format.contentType());

        MediaFile media = new MediaFile();
        media.setObjectKey(key);
        media.setUrl(storage.publicUrl(key));
        media.setContentType(format.contentType());
        media.setSizeBytes(content.length);
        media.setUploadedBy(uploadedBy);
        media = mediaFileRepository.save(media);
        return new MediaFileDto(media.getId(), media.getUrl(), media.getContentType(), media.getSizeBytes());
    }

    @Transactional(readOnly = true)
    public StorageUsageDto usage() {
        return new StorageUsageDto(storage.enabled(), mediaFileRepository.sumSizeBytes(), properties.maxTotalSize().toBytes());
    }

    /**
     * Borra del almacén las imágenes que ya no usa ningún producto, solo cuando la transacción que
     * las quitó se confirma: un rollback no puede dejar un producto apuntando a un archivo borrado.
     */
    public void releaseAfterCommit(Collection<String> urls) {
        if (urls.isEmpty()) {
            return;
        }
        Set<String> released = Set.copyOf(urls);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            released.forEach(this::deleteIfUnused);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                released.forEach(MediaService.this::deleteIfUnused);
            }
        });
    }

    private void deleteIfUnused(String url) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (productImageRepository.existsByUrl(url)) {
                    return;
                }
                mediaFileRepository.findByUrl(url).ifPresent(media -> {
                    if (storage.enabled()) {
                        storage.delete(media.getObjectKey());
                    }
                    mediaFileRepository.delete(media);
                });
            });
        } catch (RuntimeException exception) {
            // Un archivo huérfano solo ocupa espacio; no debe tumbar la operación que ya se guardó.
            logger.warn("No se pudo borrar la imagen {} del almacén", url, exception);
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            byte[] content = file.getBytes();
            if (content.length > properties.maxImageSize().toBytes()) {
                throw tooLarge();
            }
            return content;
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo", "file");
        }
    }

    private ApiException tooLarge() {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                "La imagen supera los " + properties.maxImageSize().toMegabytes() + " MB", "file");
    }
}
