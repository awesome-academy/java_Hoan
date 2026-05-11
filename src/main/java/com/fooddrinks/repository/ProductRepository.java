package com.fooddrinks.repository;

import com.fooddrinks.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// JpaSpecificationExecutor enables dynamic filtering (alphabet, type, price, category, rating)
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // Eager-fetch category via JOIN to eliminate N+1 on list queries.
    // Images are handled by @BatchSize on Product.images — no collection fetch here
    // to avoid in-memory pagination issues with @OneToMany joins.
    @Override
    @EntityGraph(attributePaths = { "category" })
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    // Admin: list ALL products (including inactive) with category eager-loaded.
    @Override
    @EntityGraph(attributePaths = { "category" })
    Page<Product> findAll(Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    // Acquires a PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) lock on the product row.
    // Used in addImage() when isPrimary=true to serialize concurrent primary-image
    // uploads.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.isActive = true")
    Optional<Product> findActiveByIdWithLock(@Param("id") Long id);
}
