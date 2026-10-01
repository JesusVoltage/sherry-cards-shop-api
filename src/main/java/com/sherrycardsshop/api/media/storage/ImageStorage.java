package com.sherrycardsshop.api.media.storage;

/** Almacén de objetos donde se guardan las imágenes subidas desde el panel. */
public interface ImageStorage {

    boolean enabled();

    void put(String key, byte[] content, String contentType);

    void delete(String key);

    String publicUrl(String key);
}
