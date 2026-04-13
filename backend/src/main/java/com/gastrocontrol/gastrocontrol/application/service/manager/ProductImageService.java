// src/main/java/com/gastrocontrol/gastrocontrol/application/service/manager/ProductImageService.java
package com.gastrocontrol.gastrocontrol.application.service.manager;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.ProductJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;

/**
 * Handles uploading and deleting product images on the local filesystem.
 *
 * <p>Images are stored under {@code ${app.uploads.dir}/products/} which is
 * a Docker volume shared between the backend container and the Nginx container.
 * The public URL written to the database is a server-relative path that Nginx
 * serves directly, so the backend never needs to stream image bytes on reads.</p>
 *
 * <h3>Allowed file types</h3>
 * <p>Only JPEG, PNG, and WebP files are accepted.  The check is done against
 * the file's declared content-type; the backend does not do deep byte-magic
 * inspection (out of scope for this phase).</p>
 *
 * <h3>Naming convention</h3>
 * <p>Files are saved as {@code {productId}.{extension}} so uploading a new image
 * for a product automatically overwrites the old one regardless of format.</p>
 */
@Service
public class ProductImageService {

    private static final Logger log = LoggerFactory.getLogger(ProductImageService.class);

    /** Maximum accepted upload size: 5 MB. */
    private static final long MAX_BYTES = 5 * 1024 * 1024;

    /** Accepted MIME types. */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ProductRepository productRepository;

    /**
     * Absolute path to the uploads root directory inside the container,
     * e.g. {@code /app/uploads}.  Configured via {@code app.uploads.dir}.
     */
    private final Path uploadsRoot;

    /**
     * Server-relative URL prefix exposed to the public, e.g.
     * {@code /gastrocontrol/uploads}.  Configured via {@code app.uploads.url-prefix}.
     */
    private final String urlPrefix;

    public ProductImageService(
            ProductRepository productRepository,
            @Value("${app.uploads.dir:/app/uploads}") String uploadsDir,
            @Value("${app.uploads.url-prefix:/gastrocontrol/uploads}") String urlPrefix
    ) {
        this.productRepository = productRepository;
        this.uploadsRoot       = Path.of(uploadsDir);
        this.urlPrefix         = urlPrefix;
    }

    /**
     * Uploads an image for the given product, replacing any previously stored image.
     *
     * <p>The image is saved to {@code <uploadsRoot>/products/<productId>.<ext>} and
     * the product's {@code imageUrl} column is updated with the public URL.</p>
     *
     * @param productId the product to attach the image to
     * @param file      the uploaded multipart file
     * @return the public server-relative URL of the saved image
     * @throws NotFoundException   if the product does not exist
     * @throws ValidationException if the file type is not allowed or the file is too large
     * @throws RuntimeException    wrapping an {@link IOException} if the write fails
     */
    @Transactional
    public String upload(Long productId, MultipartFile file) {
        ProductJpaEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        validate(file);

        String extension  = resolveExtension(file.getContentType());
        Path   productDir = uploadsRoot.resolve("products");
        Path   target     = productDir.resolve(productId + "." + extension);

        try {
            Files.createDirectories(productDir);

            // Delete any existing image file for this product (different extension)
            deleteExistingFiles(productDir, productId);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved product image: {}", target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save product image for product " + productId, e);
        }

        String publicUrl = urlPrefix + "/products/" + productId + "." + extension;
        product.setImageUrl(publicUrl);
        productRepository.save(product);

        return publicUrl;
    }

    /**
     * Removes the image associated with the given product.
     *
     * <p>Clears {@code imageUrl} on the entity and deletes the file from disk.
     * If no image exists this method is a no-op.</p>
     *
     * @param productId the product whose image should be removed
     * @throws NotFoundException if the product does not exist
     */
    @Transactional
    public void delete(Long productId) {
        ProductJpaEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        if (product.getImageUrl() == null) {
            return; // nothing to do
        }

        Path productDir = uploadsRoot.resolve("products");
        deleteExistingFiles(productDir, productId);

        product.setImageUrl(null);
        productRepository.save(product);

        log.info("Deleted product image for product {}", productId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Validates the uploaded file's content-type and size.
     *
     * @param file the file to validate
     * @throws ValidationException if validation fails
     */
    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException(Map.of("file", "File must not be empty"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ValidationException(Map.of(
                    "file", "File type not allowed. Accepted types: JPEG, PNG, WebP"
            ));
        }

        if (file.getSize() > MAX_BYTES) {
            throw new ValidationException(Map.of(
                    "file", "File size must not exceed 5 MB"
            ));
        }
    }

    /**
     * Resolves the file extension from a MIME type.
     *
     * @param contentType the MIME type
     * @return the file extension without a leading dot
     */
    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default           -> "bin"; // should never reach here after validate()
        };
    }

    /**
     * Deletes all image files for the given product ID across all allowed extensions.
     * Silently ignores files that do not exist.
     *
     * @param dir       the directory to search
     * @param productId the product whose files should be removed
     */
    private void deleteExistingFiles(Path dir, Long productId) {
        for (String ext : new String[]{"jpg", "png", "webp"}) {
            Path candidate = dir.resolve(productId + "." + ext);
            try {
                Files.deleteIfExists(candidate);
            } catch (IOException e) {
                log.warn("Could not delete image file {}: {}", candidate, e.getMessage());
            }
        }
    }
}