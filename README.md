# Spring App

Dự án Spring Boot 3 với đầy đủ stack backend theo yêu cầu.

## Stack

| Layer        | Technology              | Version |
|--------------|-------------------------|---------|
| Runtime      | OpenJDK                 | 21.x    |
| Framework    | Spring Framework        | 6.x     |
| Security     | Spring Security         | 6.x     |
| Persistence  | Spring Data JPA         | 3.x     |
| Web Server   | Apache Tomcat (embedded)| 10.x    |
| Database     | PostgreSQL              | 17.x    |
| Search       | Apache Lucene           | 10.x    |

## Cấu trúc dự án

```
src/main/java/com/example/app/
├── Application.java              ← Entry point
├── config/
│   ├── AppProperties.java        ← Type-safe config (records)
│   ├── AuditConfig.java          ← JPA Auditing
│   ├── LuceneConfig.java         ← Lucene beans (Analyzer, IndexWriter…)
│   └── SecurityConfig.java       ← Spring Security 6 + CORS + JWT filter
├── controller/
│   ├── AuthController.java       ← POST /api/v1/auth/register|login
│   └── ArticleController.java    ← CRUD + Search /api/v1/articles
├── dto/
│   ├── AuthDto.java
│   ├── ArticleDto.java
│   └── UserDto.java
├── entity/
│   ├── BaseEntity.java           ← Auditing base
│   ├── User.java
│   └── Article.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── ConflictException.java
├── repository/
│   ├── UserRepository.java
│   └── ArticleRepository.java
├── search/
│   └── ArticleSearchService.java ← Lucene 10 indexing & search
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java           ← JJWT 0.12
│   └── UserDetailsServiceImpl.java
└── service/
    ├── AuthService.java
    └── ArticleService.java       ← Business logic + Lucene sync
```

## Chạy local

### 1. Khởi động PostgreSQL 17

```bash
docker compose up -d postgres
```

### 2. Chạy ứng dụng

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Build & chạy bằng Docker

```bash
docker compose up --build
```

## API Endpoints

### Auth

| Method | URL                         | Auth | Mô tả               |
|--------|-----------------------------|------|---------------------|
| POST   | `/api/v1/auth/register`     | ✗    | Đăng ký tài khoản   |
| POST   | `/api/v1/auth/login`        | ✗    | Đăng nhập → JWT     |

### Articles

| Method | URL                         | Auth | Mô tả                    |
|--------|-----------------------------|------|--------------------------|
| GET    | `/api/v1/articles`          | ✗    | Danh sách đã publish      |
| GET    | `/api/v1/articles/{id}`     | ✗    | Chi tiết bài viết         |
| POST   | `/api/v1/articles/search`   | ✗    | Full-text search (Lucene) |
| POST   | `/api/v1/articles`          | ✓    | Tạo bài viết              |
| PATCH  | `/api/v1/articles/{id}`     | ✓    | Cập nhật bài viết         |
| DELETE | `/api/v1/articles/{id}`     | ✓    | Xoá bài viết              |

### Ví dụ request

```bash
# Đăng ký
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@example.com","password":"password123","fullName":"Admin"}'

# Đăng nhập
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"password123"}' | jq -r '.accessToken')

# Tạo bài viết
curl -X POST http://localhost:8080/api/v1/articles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Hello Lucene","content":"Tích hợp Lucene 10 với Spring Boot 3"}'

# Full-text search
curl -X POST http://localhost:8080/api/v1/articles/search \
  -H "Content-Type: application/json" \
  -d '{"query":"lucene spring","status":"DRAFT","maxHits":10}'
```

## Biến môi trường

| Biến                 | Mặc định                | Mô tả                    |
|----------------------|-------------------------|--------------------------|
| `DB_HOST`            | `localhost`             | PostgreSQL host          |
| `DB_PORT`            | `5432`                  | PostgreSQL port          |
| `DB_NAME`            | `appdb`                 | Tên database             |
| `DB_USER`            | `postgres`              | Username                 |
| `DB_PASS`            | `postgres`              | Password                 |
| `JWT_SECRET`         | *(dev default)*         | HMAC secret (≥32 chars)  |
| `LUCENE_INDEX_PATH`  | `./lucene-index`        | Thư mục index Lucene     |
| `SERVER_PORT`        | `8080`                  | Port HTTP                |
| `CORS_ORIGINS`       | `http://localhost:3000` | Allowed origins          |

## Tính năng chính

- **JWT Stateless Auth** — Access token (24h) + Refresh token (7d), filter `OncePerRequestFilter`
- **Spring Security 6** — Method-level `@PreAuthorize`, CORS cấu hình chi tiết
- **JPA Auditing** — `createdAt`, `updatedAt`, `createdBy`, `updatedBy` tự động
- **Lucene 10 Full-text Search** — Index đồng bộ với CRUD, boost trên `title`, filter theo `status`
- **Java 21 Records** — DTOs, config properties
- **Global Exception Handling** — Chuẩn hóa response lỗi
- **Docker ready** — Multi-stage Dockerfile + docker-compose với PostgreSQL 17 + pgAdmin
