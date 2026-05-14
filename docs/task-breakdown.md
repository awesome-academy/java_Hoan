# Task Breakdown — Foods & Drinks
> 10 ngày × 8h = 80h | Mỗi ngày = 1 Pull Request

---

## Day 1 — DB Design + Project Setup (8h)
- Database design (ERD, 10 bảng)
- Init Spring Boot project: Maven, dependencies
- Docker Compose: MySQL + phpMyAdmin
- `application.yml` (dev profile, datasource, JWT, OAuth2, Mail, Slack)
- Cấu trúc package (`controller`, `service`, `repository`, `entity`, `dto`, `config`, `exception`, `util`)
- Git init, `.gitignore`, README skeleton
- Entity classes cho 10 bảng + JPA relationships
- Base classes: `ApiResponse<T>`, `GlobalExceptionHandler`, `BaseEntity`
- **Output:** App khởi động không lỗi, kết nối DB thành công

---

## Day 2 — Product & Category API (8h)
> Public endpoints, không cần auth — test Postman ngay sau khi xong

- Category API: CRUD
- Product API: CRUD + upload ảnh (local storage)
- Product filter: alphabet, type (FOOD/DRINK), price, category, rating
- Serve static files (ảnh)
- **Output:** Gọi API xem/lọc sản phẩm được ngay

---

## Day 3 — Auth LOCAL + JWT (8h)
> Đủ để unblock toàn bộ các feature còn lại

- Spring Security config
- Register / Login / Logout API
- JWT: generate, validate, filter
- Role-based access: `USER` vs `ADMIN`
- Profile: GET + UPDATE API
- **Output:** Test login, nhận JWT, gọi protected API được

---

## Day 4 — Cart & Order API (8h)
- Cart API: view, add item, remove item, update quantity
- Order API: place order (từ cart), view history, view detail
- Business logic: trừ `stock_quantity`, tính `total_amount`, snapshot giá
- **Output:** Flow đặt hàng hoàn chỉnh từ đầu đến cuối

---

## Day 5 — Rating + Suggestion + Summary API (8h)
- Rating API: create/update (1 user – 1 sản phẩm), cập nhật `average_rating`
- Suggestion API: user gửi đề xuất sản phẩm
- Summary API: history orders + current cart
- **Output:** Toàn bộ REST API cho user hoàn chỉnh

---

## Day 6 — Admin UI: Layout + User + Category + Product (8h)
- Thymeleaf layout base (navbar, sidebar, template)
- Admin login page
- Admin: manage users (list, view, active/inactive)
- Admin: manage categories (list, create, edit, delete)
- Admin: manage products (list, create, edit, delete, upload ảnh)
- **Output:** Admin UI chạy được, quản lý user/category/product

---

## Day 7 — Admin UI: Order + Suggestion + Dashboard (8h)
- Admin: manage orders (list, view detail, update status)
- Admin: manage suggestions (list, view, update status)
- Admin dashboard: thống kê tổng quan (users/products/orders)
- **Output:** Admin UI hoàn chỉnh toàn bộ

---

## Day 8 — Auth OAuth2 (8h)
> Làm sau khi core features xong, không block gì

- OAuth2 config: Google, Facebook, Apple
- `CustomOAuth2UserService`: xử lý callback, auto-create user
- `OAuth2SuccessHandler`: generate JWT sau login thành công
- Xử lý conflict email (sai provider → trả lỗi rõ ràng)
- **Output:** Login bằng Google/Facebook/Apple được

---

## Day 9 — System Features (8h)
- Slack Webhook: gửi message khi có order mới
- Email notification: gửi email admin khi có order mới
- Scheduled job: báo cáo thống kê cuối tháng
- **Output:** 3 tính năng system tự động hoạt động

---

## Day 10 — Testing + Buffer + Demo (8h)
- Test toàn bộ API (Postman collection)
- Fix bugs phát sinh
- README hoàn chỉnh (hướng dẫn chạy project)
- Buffer / demo prep
