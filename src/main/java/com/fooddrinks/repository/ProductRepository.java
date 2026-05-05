package com.fooddrinks.repository;

import com.fooddrinks.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// JpaSpecificationExecutor enables dynamic filtering (alphabet, type, price, category, rating)
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // Eager-fetch category via JOIN to eliminate N+1 on list queries.
    // Images are handled by @BatchSize on Product.images — no collection fetch here
    // to avoid
    // in-memory pagination issues with @OneToMany joins.
    @Override
    @EntityGraph(attributePaths = { "category" })
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);
}
