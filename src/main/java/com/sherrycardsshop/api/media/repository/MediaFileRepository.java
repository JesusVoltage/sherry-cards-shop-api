package com.sherrycardsshop.api.media.repository;

import java.util.Optional;

import com.sherrycardsshop.api.media.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    Optional<MediaFile> findByUrl(String url);

    @Query("select coalesce(sum(m.sizeBytes), 0) from MediaFile m")
    long sumSizeBytes();
}
