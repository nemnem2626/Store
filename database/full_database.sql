/* =====================================================================
   STORE® — Script tạo toàn bộ cơ sở dữ liệu (SQL Server Management Studio)
   ---------------------------------------------------------------------
   Cách dùng: mở file này trong SSMS, bấm Execute (F5). Không cần chọn
   database trước, script tự tạo và tự chuyển sang STORE.

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
    name VARCHAR(255) NOT NULL UNIQUE
);
GO

CREATE TABLE dbo.Users (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    username   VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    fullname   VARCHAR(255) NULL,
    phone      VARCHAR(10)  NULL,
    address    VARCHAR(255) NULL,
    role       NVARCHAR(20) NULL CONSTRAINT CK_Users_role CHECK (role IN ('USER', 'ADMIN', 'STAFF')),
    active     BIT NULL,
    provider   NVARCHAR(50)  NULL,
    providerId NVARCHAR(255) NULL
);
GO

CREATE TABLE dbo.Products (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES dbo.Categories(id)
);
GO

CREATE TABLE dbo.ProductVariants (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES dbo.Products(id),
    size       VARCHAR(255)  NOT NULL,
    color      VARCHAR(255)  NOT NULL,
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
    productName VARCHAR(255)  NOT NULL,
    price       DECIMAL(38,2) NOT NULL,
    quantity    INT NOT NULL,
    size        VARCHAR(255) NULL,
    color       VARCHAR(255) NULL
);
GO

CREATE TABLE dbo.Orders (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id        INT NULL REFERENCES dbo.Users(id),
    fullname       VARCHAR(255) NOT NULL,
    phone          VARCHAR(10)  NOT NULL,
    address        VARCHAR(255) NOT NULL,
    payment_method VARCHAR(255) NOT NULL,
    total_price    FLOAT NOT NULL,
    status         VARCHAR(255) NULL,
    order_date     DATETIME2 NULL
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

/* ---------- 3. Tài khoản ---------- */
/* Chuỗi dưới đây là BCrypt của "admin123". Đổi mật khẩu ngay sau khi đăng nhập. */
INSERT INTO dbo.Users (username, password, email, fullname, phone, address, role, active, provider) VALUES
('admin', '$2a$10$YmDoYzhitroF/8TwwBS4vO82PRk58rpTwz8t4e/jXNoUVKO4iG1Lq', 'admin@store.local', N'Quản trị viên', '0900000001', N'Hà Nội',       'ADMIN', 1, 'form'),
('staff', '$2a$10$YmDoYzhitroF/8TwwBS4vO82PRk58rpTwz8t4e/jXNoUVKO4iG1Lq', 'staff@store.local', N'Nhân viên',     '0900000002', N'Hà Nội',       'STAFF', 1, 'form'),
('user',  '$2a$10$YmDoYzhitroF/8TwwBS4vO82PRk58rpTwz8t4e/jXNoUVKO4iG1Lq', 'user@store.local',  N'Khách hàng',    '0900000003', N'Hồ Chí Minh', 'USER',  1, 'form');
GO

/* ---------- 4. Danh mục ---------- */
INSERT INTO dbo.Categories (name) VALUES
('iPhone'), ('Samsung'), ('Xiaomi'), ('OPPO');
GO

/* ---------- 5. Sản phẩm ---------- */
INSERT INTO dbo.Products (name, description, category_id)
SELECT v.name, v.description, c.id
FROM (VALUES
    ('iPhone 15 Pro Max', N'Chip A17 Pro, khung titan, camera 48MP, màn hình 6.7 inch', 'iPhone'),
    ('iPhone 15',         N'Chip A16 Bionic, Dynamic Island, camera 48MP',              'iPhone'),
    ('Galaxy S24 Ultra',  N'Snapdragon 8 Gen 3, bút S Pen, camera 200MP',               'Samsung'),
    ('Galaxy A55',        N'Exynos 1480, pin 5000mAh, màn hình Super AMOLED',           'Samsung'),
    ('Xiaomi 14',         N'Snapdragon 8 Gen 3, ống kính Leica, sạc nhanh 90W',         'Xiaomi'),
    ('Redmi Note 13',     N'Camera 108MP, pin 5000mAh, màn hình 120Hz',                 'Xiaomi'),
    ('OPPO Reno 12',      N'Dimensity 7300, camera chân dung AI, sạc 80W',              'OPPO')
) AS v(name, description, category)
JOIN dbo.Categories c ON c.name = v.category;
GO

/* ---------- 6. Biến thể (dung lượng / màu / giá / tồn kho) ---------- */
INSERT INTO dbo.ProductVariants (product_id, size, color, price, stock)
SELECT p.id, v.size, v.color, v.price, v.stock
FROM (VALUES
    ('iPhone 15 Pro Max', '256GB', N'Titan tự nhiên', 34990000, 12),
    ('iPhone 15 Pro Max', '512GB', N'Titan tự nhiên', 40990000,  6),
    ('iPhone 15 Pro Max', '256GB', N'Titan đen',      34990000,  9),
    ('iPhone 15',         '128GB', N'Hồng',           22990000, 15),
    ('iPhone 15',         '256GB', N'Xanh',           25990000, 10),
    ('Galaxy S24 Ultra',  '256GB', N'Xám titan',      31990000, 11),
    ('Galaxy S24 Ultra',  '512GB', N'Tím titan',      35990000,  4),
    ('Galaxy A55',        '128GB', N'Xanh navy',       9490000, 25),
    ('Galaxy A55',        '256GB', N'Đen',            10990000, 18),
    ('Xiaomi 14',         '256GB', N'Đen',            21990000,  8),
    ('Xiaomi 14',         '512GB', N'Trắng',          24990000,  5),
    ('Redmi Note 13',     '128GB', N'Xanh',            4990000, 40),
    ('Redmi Note 13',     '256GB', N'Đen',             5690000, 30),
    ('OPPO Reno 12',      '256GB', N'Trắng',          12990000, 20)
) AS v(product, size, color, price, stock)
JOIN dbo.Products p ON p.name = v.product;
GO

/* ---------- 7. Ảnh sản phẩm ---------- */
/* Ảnh nằm trong src/main/resources/static/images. Thay đường dẫn khi có ảnh thật,
   hoặc upload ảnh mới trong trang quản trị (ảnh upload lưu ở /uploads). */
INSERT INTO dbo.ProductImages (product_id, image_url)
SELECT p.id, '/images/oppo-reno16f-pop-white.webp' FROM dbo.Products p;
GO

/* ---------- 8. Kiểm tra ---------- */
SELECT 'Users' AS bang, COUNT(*) AS so_dong FROM dbo.Users
UNION ALL SELECT 'Categories',      COUNT(*) FROM dbo.Categories
UNION ALL SELECT 'Products',        COUNT(*) FROM dbo.Products
UNION ALL SELECT 'ProductVariants', COUNT(*) FROM dbo.ProductVariants
UNION ALL SELECT 'ProductImages',   COUNT(*) FROM dbo.ProductImages;
GO
