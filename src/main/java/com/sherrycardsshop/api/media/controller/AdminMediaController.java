package com.sherrycardsshop.api.media.controller;

import com.sherrycardsshop.api.common.dto.ApiResponse;
import com.sherrycardsshop.api.media.dto.MediaFileDto;
import com.sherrycardsshop.api.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/media")
@Tag(name = "Administración · Imágenes", description = "Subida de imágenes de producto a Cloudflare R2")
public class AdminMediaController {

    private final MediaService mediaService;

    public AdminMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(path = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir imagen", description = "JPG, PNG o WebP de hasta 5 MB. Devuelve la URL pública.")
    public ResponseEntity<ApiResponse<MediaFileDto>> upload(@RequestPart("file") MultipartFile file,
                                                            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Imagen subida correctamente",
                mediaService.uploadProductImage(file, Long.valueOf(jwt.getSubject()))));
    }
}
