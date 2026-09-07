/* =====================================================================
   STORE® — Script tạo TOÀN BỘ cơ sở dữ liệu (SQL Server Management Studio)
   ---------------------------------------------------------------------
   Cách dùng: mở file này trong SSMS, bấm Execute (F5). Không cần chọn
   database trước, script tự tạo và tự chuyển sang STORE.

   Script đã bao gồm đầy đủ mọi tính năng mới nhất:
     - Bảng Notifications (thông báo cho admin khi staff giao hàng)
     - Bảng ChatMessages  (chat hỗ trợ khách hàng)
     - Cột Orders.confirm_token (xác nhận "Đã nhận hàng" qua email / mã QR)
     - Toàn bộ cột chữ tiếng Việt dùng NVARCHAR (không bị lỗi font)
     - Dữ liệu mẫu: sản phẩm, biến thể, ảnh và đơn hàng để xem biểu đồ

   CẢNH BÁO: script XOÁ và tạo lại toàn bộ bảng, mọi dữ liệu cũ sẽ mất.

   Tài khoản tạo sẵn (mật khẩu đều là admin123, lưu dạng BCrypt):
     admin / admin123  -> ADMIN
     staff / admin123  -> STAFF
     user  / admin123  -> USER
   ===================================================================== */

IF DB_ID('STORE') IS NULL
    CREATE DATABASE STORE;
GO

USE STORE;
GO

/* ---------- 1. Xoá bảng cũ (theo thứ tự khoá ngoại) ---------- */
DROP TABLE IF EXISTS dbo.Notifications;
DROP TABLE IF EXISTS dbo.ChatMessages;
DROP TABLE IF EXISTS dbo.OrderDetails;
DROP TABLE IF EXISTS dbo.Orders;
DROP TABLE IF EXISTS dbo.CartItems;
DROP TABLE IF EXISTS dbo.Carts;
DROP TABLE IF EXISTS dbo.ProductImages;
DROP TABLE IF EXISTS dbo.ProductVariants;
DROP TABLE IF EXISTS dbo.Products;
DROP TABLE IF EXISTS dbo.Categories;
DROP TABLE IF EXISTS dbo.Users;
GO

/* ---------- 2. Tạo bảng ---------- */

CREATE TABLE dbo.Categories (
    id   BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL UNIQUE
);
GO

CREATE TABLE dbo.Users (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    username   VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    fullname   NVARCHAR(255) NULL,
    phone      VARCHAR(10)   NULL,
    address    NVARCHAR(255) NULL,
    role       NVARCHAR(20)  NULL CONSTRAINT CK_Users_role CHECK (role IN ('USER', 'ADMIN', 'STAFF')),
    active     BIT NULL,
    provider   NVARCHAR(50)  NULL,
    providerId NVARCHAR(255) NULL
);
GO

CREATE TABLE dbo.Products (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(255) NOT NULL,
    description NVARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES dbo.Categories(id)
);
GO

CREATE TABLE dbo.ProductVariants (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES dbo.Products(id),
    size       NVARCHAR(255) NOT NULL,
    color      NVARCHAR(255) NOT NULL,
    price      DECIMAL(38,2) NOT NULL,
    stock      INT NOT NULL,
    CONSTRAINT UQ_ProductVariants UNIQUE (product_id, size, color)
);
GO

CREATE TABLE dbo.ProductImages (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES dbo.Products(id),
    variant_id BIGINT NULL     REFERENCES dbo.ProductVariants(id),
    image_url  VARCHAR(255) NOT NULL
);
GO

CREATE TABLE dbo.Carts (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id    INT NOT NULL REFERENCES dbo.Users(id),
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL
);
GO

CREATE TABLE dbo.CartItems (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    cart_id     BIGINT NOT NULL REFERENCES dbo.Carts(id),
    variant_id  BIGINT NOT NULL REFERENCES dbo.ProductVariants(id),
    productName NVARCHAR(255) NOT NULL,
    price       DECIMAL(38,2) NOT NULL,
    quantity    INT NOT NULL,
    size        NVARCHAR(255) NULL,
    color       NVARCHAR(255) NULL
);
GO

CREATE TABLE dbo.Orders (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id        INT NULL REFERENCES dbo.Users(id),
    fullname       NVARCHAR(255) NOT NULL,
    phone          VARCHAR(10)   NOT NULL,
    address        NVARCHAR(255) NOT NULL,
    payment_method VARCHAR(255)  NOT NULL,
    total_price    FLOAT NOT NULL,
    status         VARCHAR(255) NULL,
    order_date     DATETIME2 NULL,
    confirm_token  VARCHAR(64) NULL
);
GO

CREATE TABLE dbo.OrderDetails (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id   BIGINT NULL REFERENCES dbo.Orders(id),
    variant_id BIGINT NULL REFERENCES dbo.ProductVariants(id),
    quantity   INT NOT NULL,
    price      DECIMAL(38,2) NOT NULL
);
GO

/* Tin nhắn hỗ trợ giữa khách hàng và nhân viên.
   user_id là khách hàng sở hữu hội thoại, sender_id là người gửi tin. */
CREATE TABLE dbo.ChatMessages (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     INT NOT NULL REFERENCES dbo.Users(id),
    sender_id   INT NULL REFERENCES dbo.Users(id),
    sender_role VARCHAR(20) NOT NULL,
    content     NVARCHAR(1000) NOT NULL,
    created_at  DATETIME2 NOT NULL,
    is_read     BIT NOT NULL DEFAULT 0
);
GO

CREATE INDEX IX_ChatMessages_User ON dbo.ChatMessages(user_id, id);
GO

/* Thông báo nội bộ (ví dụ: staff xác nhận giao đơn hàng -> báo cho admin). */
CREATE TABLE dbo.Notifications (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    target_role VARCHAR(20) NOT NULL,
    content     NVARCHAR(500) NOT NULL,
    order_id    BIGINT NULL,
    created_at  DATETIME2 NOT NULL,
    is_read     BIT NOT NULL DEFAULT 0
);
GO

CREATE INDEX IX_Notifications_Role ON dbo.Notifications(target_role, id);
GO

/* ---------- 3. Tài khoản ---------- */
/* Chuỗi dưới đây là BCrypt của "admin123". Đổi mật khẩu ngay sau khi đăng nhập. */
INSERT INTO dbo.Users (username, password, email, fullname, phone, address, role, active, provider) VALUES
('admin', '$2a$10$YmDoYzhitroF/8TwwBS4vO82PRk58rpTwz8t4e/jXNoUVKO4iG1Lq', 'admin@store.local', N'Quản trị viên', '0900000001', N'Hà Nội',       'ADMIN', 1, 'form'),
('staff', '$2a$10$YmDoYzhitroF/8TwwBS4vO82PRk58rpTwz8t4e/jXNoUVKO4iG1Lq', 'staff@store.local', N'Nhân viên',     '0900000002', N'Hà Nội',       'STAFF', 1, 'form'),
('user',  '$2a$10$YmDoYzhitroF/8TwwBS4vO82PRk58rpTwz8t4e/jXNoUVKO4iG1Lq', 'user@store.local',  N'Khách hàng',    '0900000003', N'Hồ Chí Minh', 'USER',  1, 'form');
GO

/* ---------- 4. Danh mục ---------- */
INSERT INTO dbo.Categories (name) VALUES
(N'iPhone'), (N'Samsung'), (N'Xiaomi'), (N'OPPO');
GO

/* ---------- 5. Sản phẩm ---------- */
INSERT INTO dbo.Products (name, description, category_id)
SELECT v.name, v.description, c.id
FROM (VALUES
    (N'iPhone 15 Pro Max', N'Chip A17 Pro, khung titan, camera 48MP, màn hình 6.7 inch', N'iPhone'),
    (N'iPhone 15',         N'Chip A16 Bionic, Dynamic Island, camera 48MP',              N'iPhone'),
    (N'Galaxy S24 Ultra',  N'Snapdragon 8 Gen 3, bút S Pen, camera 200MP',               N'Samsung'),
    (N'Galaxy A55',        N'Exynos 1480, pin 5000mAh, màn hình Super AMOLED',           N'Samsung'),
    (N'Xiaomi 14',         N'Snapdragon 8 Gen 3, ống kính Leica, sạc nhanh 90W',         N'Xiaomi'),
    (N'Redmi Note 13',     N'Camera 108MP, pin 5000mAh, màn hình 120Hz',                 N'Xiaomi'),
    (N'OPPO Reno 12',      N'Dimensity 7300, camera chân dung AI, sạc 80W',              N'OPPO')
) AS v(name, description, category)
JOIN dbo.Categories c ON c.name = v.category;
GO

/* ---------- 6. Biến thể (dung lượng / màu / giá / tồn kho) ---------- */
INSERT INTO dbo.ProductVariants (product_id, size, color, price, stock)
SELECT p.id, v.size, v.color, v.price, v.stock
FROM (VALUES
    (N'iPhone 15 Pro Max', N'256GB', N'Titan tự nhiên', 34990000, 12),
    (N'iPhone 15 Pro Max', N'512GB', N'Titan tự nhiên', 40990000,  6),
    (N'iPhone 15 Pro Max', N'256GB', N'Titan đen',      34990000,  9),
    (N'iPhone 15',         N'128GB', N'Hồng',           22990000, 15),
    (N'iPhone 15',         N'256GB', N'Xanh',           25990000, 10),
    (N'Galaxy S24 Ultra',  N'256GB', N'Xám titan',      31990000, 11),
    (N'Galaxy S24 Ultra',  N'512GB', N'Tím titan',      35990000,  4),
    (N'Galaxy A55',        N'128GB', N'Xanh navy',       9490000, 25),
    (N'Galaxy A55',        N'256GB', N'Đen',            10990000, 18),
    (N'Xiaomi 14',         N'256GB', N'Đen',            21990000,  8),
    (N'Xiaomi 14',         N'512GB', N'Trắng',          24990000,  5),
    (N'Redmi Note 13',     N'128GB', N'Xanh',            4990000, 40),
    (N'Redmi Note 13',     N'256GB', N'Đen',             5690000, 30),
    (N'OPPO Reno 12',      N'256GB', N'Trắng',          12990000, 20)
) AS v(product, size, color, price, stock)
JOIN dbo.Products p ON p.name = v.product;
GO

/* ---------- 7. Ảnh sản phẩm ---------- */
/* Ảnh nằm trong src/main/resources/static/images. Thay đường dẫn khi có ảnh thật,
   hoặc upload ảnh mới trong trang quản trị (ảnh upload lưu ở /uploads). */
INSERT INTO dbo.ProductImages (product_id, image_url)
SELECT p.id, '/images/oppo-reno16f-pop-white.webp' FROM dbo.Products p;
GO

/* ---------- 8. Đơn hàng mẫu (để biểu đồ doanh thu ở trang admin có dữ liệu) ----------
   Mỗi đơn gồm 1 sản phẩm, ngày đặt rải trong 6 tháng gần nhất.
   Xoá cả mục 8 này nếu bạn muốn khởi đầu với dữ liệu đơn hàng trống. */
DECLARE @uid INT = (SELECT id FROM dbo.Users WHERE username = 'user');

INSERT INTO dbo.Orders (user_id, fullname, phone, address, payment_method, total_price, status, order_date, confirm_token)
SELECT @uid, N'Khách hàng', '0900000003', N'123 Lê Lợi, Quận 1, TP. Hồ Chí Minh',
       v.pay, v.total, v.status, DATEADD(DAY, v.days_ago * -1, CAST(GETDATE() AS DATETIME2)), NULL
FROM (VALUES
    ('COD',   34990000, 'DELIVERED', 150),
    ('VNPAY', 22990000, 'DELIVERED', 120),
    ('COD',   31990000, 'DELIVERED',  95),
    ('COD',    9490000, 'CANCELED',   70),
    ('VNPAY', 21990000, 'DELIVERED',  45),
    ('COD',    5690000, 'SHIPPING',   10),
    ('COD',   12990000, 'PENDING',     2)
) AS v(pay, total, status, days_ago);
GO

/* Chi tiết đơn: gắn mỗi đơn với 1 biến thể có giá trùng tổng tiền đơn. */
INSERT INTO dbo.OrderDetails (order_id, variant_id, quantity, price)
SELECT o.id, pv.id, 1, pv.price
FROM dbo.Orders o
CROSS APPLY (
    SELECT TOP 1 v.id, v.price
    FROM dbo.ProductVariants v
    WHERE v.price = CAST(o.total_price AS DECIMAL(38,2))
    ORDER BY v.id
) pv
WHERE NOT EXISTS (SELECT 1 FROM dbo.OrderDetails d WHERE d.order_id = o.id);
GO

/* ---------- 9. Kiểm tra ---------- */
SELECT 'Users' AS bang, COUNT(*) AS so_dong FROM dbo.Users
UNION ALL SELECT 'Categories',      COUNT(*) FROM dbo.Categories
UNION ALL SELECT 'Products',        COUNT(*) FROM dbo.Products
UNION ALL SELECT 'ProductVariants', COUNT(*) FROM dbo.ProductVariants
UNION ALL SELECT 'ProductImages',   COUNT(*) FROM dbo.ProductImages
UNION ALL SELECT 'Orders',          COUNT(*) FROM dbo.Orders
UNION ALL SELECT 'OrderDetails',    COUNT(*) FROM dbo.OrderDetails
UNION ALL SELECT 'ChatMessages',    COUNT(*) FROM dbo.ChatMessages
UNION ALL SELECT 'Notifications',   COUNT(*) FROM dbo.Notifications;
GO
