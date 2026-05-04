# Database Design — Foods & Drinks

## Overview

- Database: MySQL 8.x
- Engine: InnoDB (support foreign keys & transactions)
- Charset: utf8mb4

---

## Tables

### 1. `users`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| email | VARCHAR(255) | UNIQUE, NOT NULL | |
| password | VARCHAR(255) | NULL | NULL nếu đăng nhập OAuth2 |
| full_name | VARCHAR(255) | NOT NULL | |
| phone | VARCHAR(20) | NULL | |
| address | TEXT | NULL | |
| avatar_url | VARCHAR(500) | NULL | |
| role | ENUM('USER','ADMIN') | NOT NULL, DEFAULT 'USER' | |
| provider | ENUM('LOCAL','GOOGLE','FACEBOOK','TWITTER') | NOT NULL, DEFAULT 'LOCAL' | Không support account linking — 1 email = 1 provider |
| provider_id | VARCHAR(255) | NULL | ID từ OAuth2 provider (Google sub, Facebook id...) |
| email_verified | BOOLEAN | NOT NULL, DEFAULT FALSE | TRUE ngay khi đăng nhập OAuth2, FALSE với LOCAL cho đến khi verify |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

---

### 2. `categories`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(100) | UNIQUE, NOT NULL | Tên danh mục |
| description | TEXT | NULL | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

---

### 3. `products`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(255) | NOT NULL | |
| description | TEXT | NULL | |
| price | DECIMAL(10,2) | NOT NULL | |
| type | ENUM('FOOD','DRINK') | NOT NULL | Phân loại |
| stock_quantity | INT | NOT NULL, DEFAULT 0 | Số lượng tồn kho |
| average_rating | DECIMAL(2,1) | NOT NULL, DEFAULT 0.0 | Tính toán từ bảng ratings |
| category_id | BIGINT | FK → categories(id) | |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

---

### 4. `product_images`

> Mỗi sản phẩm có thể có nhiều ảnh. Ảnh chính lấy qua `is_primary = true`, **không** lưu riêng trong bảng `products`.

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| product_id | BIGINT | FK → products(id), NOT NULL | |
| image_url | VARCHAR(500) | NOT NULL | Local path |
| is_primary | BOOLEAN | NOT NULL, DEFAULT FALSE | Chỉ 1 ảnh được is_primary = true mỗi sản phẩm |
| created_at | DATETIME | NOT NULL | |

---

### 5. `ratings`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK → users(id), NOT NULL | |
| product_id | BIGINT | FK → products(id), NOT NULL | |
| score | TINYINT | NOT NULL, CHECK (1–5) | Điểm đánh giá |
| comment | TEXT | NULL | |
| created_at | DATETIME | NOT NULL | |

> **UNIQUE** constraint trên `(user_id, product_id)` — mỗi user chỉ rating một sản phẩm một lần.

---

### 6. `carts`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK → users(id), UNIQUE, NOT NULL | Mỗi user có 1 cart |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

---

### 7. `cart_items`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| cart_id | BIGINT | FK → carts(id), NOT NULL | |
| product_id | BIGINT | FK → products(id), NOT NULL | |
| quantity | INT | NOT NULL, DEFAULT 1 | |
| added_at | DATETIME | NOT NULL | |

> **UNIQUE** constraint trên `(cart_id, product_id)`.

---

### 8. `orders`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK → users(id), NOT NULL | |
| total_amount | DECIMAL(10,2) | NOT NULL | |
| status | ENUM('PENDING','CONFIRMED','DELIVERING','COMPLETED','CANCELLED') | NOT NULL, DEFAULT 'PENDING' | |
| shipping_address | TEXT | NOT NULL | |
| note | TEXT | NULL | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

---

### 9. `order_items`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| order_id | BIGINT | FK → orders(id), NOT NULL | |
| product_id | BIGINT | FK → products(id), NOT NULL | |
| product_name | VARCHAR(255) | NOT NULL | Snapshot tên lúc đặt hàng |
| product_price | DECIMAL(10,2) | NOT NULL | Snapshot giá lúc đặt hàng |
| quantity | INT | NOT NULL | |
| subtotal | DECIMAL(10,2) | NOT NULL | product_price × quantity |

---

### 10. `suggestions`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK → users(id), NOT NULL | |
| name | VARCHAR(255) | NOT NULL | Tên sản phẩm đề xuất |
| description | TEXT | NULL | |
| status | ENUM('PENDING','REVIEWED','APPROVED','REJECTED') | NOT NULL, DEFAULT 'PENDING' | |
| created_at | DATETIME | NOT NULL | |

---

## Relationships

```
users (1) ──────────── (1) carts
users (1) ──────────── (N) orders
users (1) ──────────── (N) ratings
users (1) ──────────── (N) suggestions

categories (1) ─────── (N) products

products (1) ──────────(N) product_images
products (1) ──────────(N) ratings
products (1) ──────────(N) cart_items
products (1) ──────────(N) order_items

carts (1) ─────────────(N) cart_items
orders (1) ────────────(N) order_items
```

---

## Notes

- **`provider_id`**: Là ID do bên thứ 3 cấp (Google `sub`, Facebook `id`...), KHÔNG phải FK đến bảng nào trong DB. Dùng để nhận diện user khi họ quay lại login bằng OAuth2.
- **Ảnh sản phẩm**: Bỏ `image_url` khỏi `products`, tập trung tất cả vào `product_images`. Ảnh chính = `WHERE is_primary = true`.
- **Snapshot trong `order_items`**: Lưu lại `product_name` và `product_price` tại thời điểm đặt hàng để tránh ảnh hưởng khi admin cập nhật giá sản phẩm sau này.
- **`average_rating` trong `products`**: Cập nhật mỗi khi có rating mới (tính lại trong service layer), tránh query aggregation mỗi lần load sản phẩm.
- **Soft delete**: Dùng `is_active = false` thay vì xóa thật với `users` và `products`.
- **OAuth2**: Khi đăng nhập bằng provider, `password` để NULL, `provider` và `provider_id` lưu thông tin từ OAuth2.
- **Không support account linking**: 1 email chỉ thuộc về 1 provider. Nếu email đã tồn tại với provider khác → trả lỗi rõ ràng ở tầng Service.
- **OAuth2 flow**: Google/Facebook/Twitter callback → lấy email + provider_id → check email tồn tại chưa → nếu chưa thì tạo user mới, nếu rồi và đúng provider thì login, nếu sai provider thì báo lỗi.
