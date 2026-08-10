# STORE® — Website bán điện thoại

Ứng dụng Spring Boot 3.4 (Java 21, Thymeleaf, Spring Security, SQL Server).

## Chạy lần đầu

1. Tạo database và dữ liệu mẫu (cần SQL Server đang chạy):

```bash
sqlcmd -S localhost -U sa -P <mật khẩu sa> -C -f 65001 -i database/schema.sql
sqlcmd -S localhost -U sa -P <mật khẩu sa> -C -f 65001 -i database/seed.sql
```

Hoặc mở hai file này trong SSMS và bấm Execute.

2. Chạy ứng dụng:

```bash
./mvnw spring-boot:run          # Linux/macOS
.\mvnw.cmd spring-boot:run      # Windows
```

Mở http://localhost:8080/home

Tài khoản mẫu từ `database/seed.sql`: `admin` / `admin123` (quản trị) và `user` / `admin123` (khách hàng).

## Cấu hình

`src/main/resources/application.properties` đọc từ biến môi trường, đều có giá trị mặc định cho môi trường phát triển. Nếu SQL Server của bạn dùng tài khoản khác thì set trước khi chạy:

| Biến | Mặc định |
| --- | --- |
| `DB_URL` | `jdbc:sqlserver://localhost:1433;databaseName=STORE;encrypt=true;trustServerCertificate=true` |
| `DB_USERNAME` | `sa` |
| `DB_PASSWORD` | `12345` |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | `disabled` |
| `FACEBOOK_CLIENT_ID` / `FACEBOOK_CLIENT_SECRET` | `disabled` |
| `VNPAY_TMN_CODE` / `VNPAY_HASH_SECRET` | `disabled` |

Ví dụ trên Windows PowerShell:

```powershell
$env:DB_PASSWORD="mật_khẩu_sa_của_bạn"
.\mvnw.cmd spring-boot:run
```

Đăng nhập Google/Facebook và thanh toán VNPAY chỉ hoạt động khi khai báo credential thật.
