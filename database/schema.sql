-- Tạo cơ sở dữ liệu và toàn bộ bảng cho ứng dụng STORE (SQL Server).
-- Chạy bằng SSMS hoặc: sqlcmd -S localhost -U sa -P <mật khẩu> -C -f 65001 -i database/schema.sql
-- Script này idempotent: chạy lại nhiều lần không lỗi.

IF DB_ID('STORE') IS NULL
    CREATE DATABASE STORE;
GO

USE STORE;
GO

IF OBJECT_ID('dbo.Categories', 'U') IS NULL
CREATE TABLE dbo.Categories (
    id   BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL UNIQUE
);
GO

IF OBJECT_ID('dbo.Users', 'U') IS NULL
CREATE TABLE dbo.Users (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    username   VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    fullname   NVARCHAR(255) NULL,
    phone      VARCHAR(10)   NULL,
    address    NVARCHAR(255) NULL,
    role       NVARCHAR(20) NULL CONSTRAINT CK_Users_role CHECK (role IN ('USER', 'ADMIN', 'STAFF')),
    active     BIT NULL,
    provider   NVARCHAR(50)  NULL,
    providerId NVARCHAR(255) NULL
);
GO

IF OBJECT_ID('dbo.Products', 'U') IS NULL
CREATE TABLE dbo.Products (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(255) NOT NULL,
    description NVARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES dbo.Categories(id)
);
GO

IF OBJECT_ID('dbo.ProductVariants', 'U') IS NULL
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

IF OBJECT_ID('dbo.ProductImages', 'U') IS NULL
CREATE TABLE dbo.ProductImages (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES dbo.Products(id),
    variant_id BIGINT NULL     REFERENCES dbo.ProductVariants(id),
    image_url  VARCHAR(255) NOT NULL
);
GO

IF OBJECT_ID('dbo.Carts', 'U') IS NULL
CREATE TABLE dbo.Carts (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id    INT NOT NULL REFERENCES dbo.Users(id),
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL
);
GO

IF OBJECT_ID('dbo.CartItems', 'U') IS NULL
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

IF OBJECT_ID('dbo.Orders', 'U') IS NULL
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

IF COL_LENGTH('dbo.Orders', 'confirm_token') IS NULL
    ALTER TABLE dbo.Orders ADD confirm_token VARCHAR(64) NULL;
GO

IF OBJECT_ID('dbo.OrderDetails', 'U') IS NULL
CREATE TABLE dbo.OrderDetails (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id   BIGINT NULL REFERENCES dbo.Orders(id),
    variant_id BIGINT NULL REFERENCES dbo.ProductVariants(id),
    quantity   INT NOT NULL,
    price      DECIMAL(38,2) NOT NULL
);
GO

IF OBJECT_ID('dbo.ChatMessages', 'U') IS NULL
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

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ChatMessages_User' AND object_id = OBJECT_ID('dbo.ChatMessages'))
CREATE INDEX IX_ChatMessages_User ON dbo.ChatMessages(user_id, id);
GO

IF OBJECT_ID('dbo.Notifications', 'U') IS NULL
CREATE TABLE dbo.Notifications (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    target_role VARCHAR(20) NOT NULL,
    content     NVARCHAR(500) NOT NULL,
    order_id    BIGINT NULL,
    created_at  DATETIME2 NOT NULL,
    is_read     BIT NOT NULL DEFAULT 0
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Notifications_Role' AND object_id = OBJECT_ID('dbo.Notifications'))
CREATE INDEX IX_Notifications_Role ON dbo.Notifications(target_role, id);
GO
