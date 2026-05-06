package com.fooddrinks.service.impl;

import com.fooddrinks.dto.request.ProductRequest;
import com.fooddrinks.dto.response.ProductImageResponse;
import com.fooddrinks.dto.response.ProductResponse;
import com.fooddrinks.entity.Category;
import com.fooddrinks.entity.Product;
import com.fooddrinks.entity.ProductImage;
import com.fooddrinks.entity.ProductType;
import com.fooddrinks.exception.BadRequestException;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.repository.CategoryRepository;
import com.fooddrinks.repository.ProductImageRepository;
import com.fooddrinks.repository.ProductRepository;
import com.fooddrinks.repository.spec.ProductSpecification;
import com.fooddrinks.service.FileStorageService;
import com.fooddrinks.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(String letter, ProductType type, Long categoryId,
            BigDecimal minPrice, BigDecimal maxPrice,
            BigDecimal minRating, Pageable pageable) {
        Specification<Product> spec = ProductSpecification.isActive();

        if (letter != null && !letter.isBlank()) {
            if (!letter.matches("[A-Za-z]")) {
                throw new BadRequestException("letter must be a single A\u2013Z character");
            }
            spec = spec.and(ProductSpecification.nameStartsWith(letter));
        }
        if (type != null) {
            spec = spec.and(ProductSpecification.hasType(type));
        }
        if (categoryId != null) {
            spec = spec.and(ProductSpecification.hasCategory(categoryId));
        }
        if (minPrice != null && maxPrice != null) {
            if (minPrice.compareTo(maxPrice) > 0) {
                throw new BadRequestException("minPrice must be less than or equal to maxPrice");
            }
            spec = spec.and(ProductSpecification.priceBetween(minPrice, maxPrice));
        } else if (minPrice != null) {
            spec = spec.and(ProductSpecification.minPrice(minPrice));
        } else if (maxPrice != null) {
            spec = spec.and(ProductSpecification.maxPrice(maxPrice));
        }
        if (minRating != null) {
            spec = spec.and(ProductSpecification.minRating(minRating));
        }

        return productRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(findActiveOrThrow(id));
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findActiveOrThrow(id);
        applyRequest(product, request);
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findActiveOrThrow(id);
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public ProductResponse addImage(Long productId, MultipartFile file, boolean isPrimary) {
        // When setting a new primary, acquire a pessimistic write lock (SELECT FOR
        // UPDATE)
        // on the product row. This serializes concurrent isPrimary=true uploads for the
        // same product — only one transaction can hold the lock at a time, so the
        // clearPrimaryImages + insert sequence is effectively atomic across requests.
        Product product = isPrimary
                ? findActiveWithLockOrThrow(productId)
                : findActiveOrThrow(productId);

        if (isPrimary) {
            productImageRepository.clearPrimaryImages(productId);
        }

        // Store file BEFORE persisting to DB.
        // Register a compensation callback: if the transaction rolls back after
        // store() succeeds, the file on disk is deleted so nothing is orphaned.
        String url = fileStorageService.store(file);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    fileStorageService.delete(url);
                }
            }
        });

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl(url);
        image.setIsPrimary(isPrimary);
        productImageRepository.save(image);

        return toResponse(findActiveOrThrow(productId));
    }

    @Override
    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));
        if (!image.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Image does not belong to product " + productId);
        }
        // Delete DB record first — if the TX rolls back, the file on disk is still
        // intact.
        // Delete the file only after the commit is confirmed (afterCommit) so we never
        // end up with a DB record pointing to a missing file.
        String imageUrl = image.getImageUrl();
        productImageRepository.delete(image);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.delete(imageUrl);
            }
        });
    }

    // --- helpers ---

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setType(request.getType());
        product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }
    }

    private Product findActiveOrThrow(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (!product.getIsActive()) {
            throw new ResourceNotFoundException("Product", id);
        }
        return product;
    }

    private Product findActiveWithLockOrThrow(Long id) {
        return productRepository.findActiveByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private ProductResponse toResponse(Product product) {
        List<ProductImageResponse> imageResponses = product.getImages().stream()
                .map(img -> ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .isPrimary(img.getIsPrimary())
                        .createdAt(img.getCreatedAt())
                        .build())
                .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .type(product.getType())
                .stockQuantity(product.getStockQuantity())
                .averageRating(product.getAverageRating())
                .isActive(product.getIsActive())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .images(imageResponses)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
