package com.fooddrinks.repository.spec;

import com.fooddrinks.entity.Product;
import com.fooddrinks.entity.ProductType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Tập hợp các điều kiện filter động cho Product.
 *
 * Mỗi method trả về một Specification<Product> — hiểu đơn giản là 1 mảnh WHERE clause.
 * Các mảnh này được ghép lại bằng .and() trong ProductServiceImpl.
 *
 * Ví dụ kết quả SQL khi ghép isActive + hasType + minPrice:
 *   WHERE is_active = true AND type = 'FOOD' AND price >= 50000
 *
 * Lambda (root, query, cb) gồm 3 tham số:
 *   - root: đại diện cho bảng products (dùng để truy cập các cột)
 *   - query: đại diện cho toàn bộ câu query (ít dùng ở đây)
 *   - cb (CriteriaBuilder): công cụ tạo điều kiện (equal, like, between...)
 */
public class ProductSpecification {

    // Utility class — không cho phép tạo instance, chỉ dùng static methods
    private ProductSpecification() {}

    // WHERE is_active = true — luôn áp dụng, không trả về sản phẩm đã bị xóa mềm
    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    // WHERE LOWER(name) LIKE 'a%' — lọc theo chữ cái đầu, không phân biệt hoa thường
    public static Specification<Product> nameStartsWith(String letter) {
        if (letter == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), letter.toLowerCase() + "%");
    }

    // WHERE type = 'FOOD' hoặc WHERE type = 'DRINK'
    public static Specification<Product> hasType(ProductType type) {
        if (type == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    // WHERE category_id = ? — root.get("category").get("id") tương đương JOIN products.category_id
    public static Specification<Product> hasCategory(Long categoryId) {
        if (categoryId == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    // WHERE price BETWEEN min AND max
    public static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        if (min == null || max == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.between(root.get("price"), min, max);
    }

    // WHERE price >= min
    public static Specification<Product> minPrice(BigDecimal min) {
        if (min == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    // WHERE price <= max
    public static Specification<Product> maxPrice(BigDecimal max) {
        if (max == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    // WHERE average_rating >= minRating — lọc sản phẩm có rating đủ cao
    public static Specification<Product> minRating(BigDecimal minRating) {
        if (minRating == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("averageRating"), minRating);
    }
}
