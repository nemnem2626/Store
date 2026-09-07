# STORE® — Website bán điện thoại

Ứng dụng Spring Boot 3.4 (Java 21, Thymeleaf, Spring Security, SQL Server).

## Chạy lần đầu

1. Tạo database và dữ liệu mẫu (cần SQL Server đang chạy). Mở `database/full_database.sql` trong SSMS rồi bấm Execute (F5) — file này tạo sẵn database `STORE`, toàn bộ bảng và dữ liệu mẫu. Hoặc chạy bằng dòng lệnh:

```bash
sqlcmd -S localhost -U sa -P <mật khẩu sa> -C -f 65001 -i database/full_database.sql
```

> `full_database.sql` XOÁ và tạo lại toàn bộ bảng. Nếu database đã có dữ liệu thật, dùng `database/schema.sql` (idempotent, chỉ tạo bảng còn thiếu) thay cho nó.

2. Chạy ứng dụng:

```bash
./mvnw spring-boot:run          # Linux/macOS
.\mvnw.cmd spring-boot:run      # Windows
```

Mở http://localhost:8080/home

Tài khoản mẫu: `admin` / `admin123` (quản trị), `staff` / `admin123` (nhân viên), `user` / `admin123` (khách hàng).

## Các file SQL trong `database/`

| File | Dùng khi nào |
| --- | --- |
| `full_database.sql` | Cài mới hoàn toàn: tạo database + tất cả bảng + dữ liệu mẫu (xoá dữ liệu cũ) |
| `schema.sql` | Tạo bảng còn thiếu mà không xoá dữ liệu (chạy lại nhiều lần được) |
| `seed.sql` | Thêm dữ liệu mẫu vào database đã có bảng |
| `notifications.sql` | DB cũ chưa có bảng `Notifications` (thông báo cho admin) |
| `chat-messages.sql` | DB cũ chưa có bảng `ChatMessages` (chat hỗ trợ) |
| `order-confirm-token.sql` | DB cũ chưa có cột `Orders.confirm_token` (xác nhận nhận hàng / mã QR) |
| `fix-vietnamese-nvarchar.sql` | Chữ tiếng Việt bị lỗi font (`Titan t? nhiên`): đổi các cột `VARCHAR` sang `NVARCHAR` |

## Cấu hình

`src/main/resources/application.properties` đọc từ biến môi trường, đều có giá trị mặc định cho môi trường phát triển. Nếu SQL Server của bạn dùng tài khoản khác thì set trước khi chạy:

| Biến | Mặc định |
| --- | --- |
| `DB_URL` | `jdbc:sqlserver://localhost:1433;databaseName=STORE;encrypt=true;trustServerCertificate=true` |
| `DB_USERNAME` | `sa` |
| `DB_PASSWORD` | `123` |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | `disabled` |
| `FACEBOOK_CLIENT_ID` / `FACEBOOK_CLIENT_SECRET` | `disabled` |
| `VNPAY_TMN_CODE` / `VNPAY_HASH_SECRET` | `disabled` |

Ví dụ trên Windows PowerShell:

```powershell
$env:DB_PASSWORD="mật_khẩu_sa_của_bạn"
.\mvnw.cmd spring-boot:run
```

Đăng nhập Google/Facebook và thanh toán VNPAY chỉ hoạt động khi khai báo credential thật.
