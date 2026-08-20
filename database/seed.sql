-- Dữ liệu mẫu để chạy thử ứng dụng (chạy sau database/schema.sql).
-- sqlcmd -S localhost -U sa -P <mật khẩu> -C -f 65001 -i database/seed.sql
-- Tài khoản tạo sẵn: admin / admin123 (ADMIN) và user / admin123 (USER). Đổi mật khẩu ngay khi dùng thật.

USE STORE;
GO

INSERT INTO dbo.Categories (name)
SELECT v.name FROM (VALUES ('iPhone'), ('Samsung'), ('Xiaomi'), ('OPPO')) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Categories c WHERE c.name = v.name);
GO

-- Mật khẩu là chuỗi BCrypt của "admin123".
INSERT INTO dbo.Users (username, password, email, fullname, phone, address, role, active, provider)
SELECT v.username, v.password, v.email, v.fullname, v.phone, v.address, v.role, 1, 'form'
FROM (VALUES
    ('admin', '$2a$10$YmDoYzhitroF/8TwwBS4vO82PRk58rpTwz8t4e/jXNoUVKO4iG1Lq', 'admin@store.local', N'Quản trị viên', '0900000001', N'Hà Nội', 'ADMIN'),
    ('user',  '$2a$10$YmDoYzhitroF/8TwwBS4vO82PRk58rpTwz8t4e/jXNoUVKO4iG1Lq', 'user@store.local',  N'Khách hàng',    '0900000002', N'Hà Nội', 'USER')
) AS v(username, password, email, fullname, phone, address, role)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Users u WHERE u.username = v.username);
GO

INSERT INTO dbo.Products (name, description, category_id)
SELECT v.name, v.description, c.id
FROM (VALUES
    ('iPhone 15 Pro', N'Chip A17 Pro, khung titan, camera 48MP', 'iPhone'),
    ('Galaxy S24',    N'Snapdragon 8 Gen 3, màn hình 6.2 inch',  'Samsung'),
    ('OPPO Reno 12',  N'Camera 50MP AI, màn hình AMOLED 6.7 inch', 'OPPO'),
    ('Redmi Note 13', N'Snapdragon, pin 5000mAh, sạc 33W', 'Xiaomi')
) AS v(name, description, category)
JOIN dbo.Categories c ON c.name = v.category
WHERE NOT EXISTS (SELECT 1 FROM dbo.Products p WHERE p.name = v.name);
GO

INSERT INTO dbo.ProductVariants (product_id, size, color, price, stock)
SELECT p.id, v.size, v.color, v.price, v.stock
FROM (VALUES
    ('iPhone 15 Pro', '256GB', N'Titan', 29990000, 10),
    ('iPhone 15 Pro', '512GB', N'Đen',   34990000, 5),
    ('Galaxy S24',    '256GB', N'Xám',   22990000, 8),
    ('OPPO Reno 12',  '256GB', N'Trắng', 12990000, 12),
    ('Redmi Note 13', '256GB', N'Xanh',  4990000, 15)
) AS v(product, size, color, price, stock)
JOIN dbo.Products p ON p.name = v.product
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.ProductVariants pv
    WHERE pv.product_id = p.id AND pv.size = v.size AND pv.color = v.color
);
GO

INSERT INTO dbo.ProductImages (product_id, image_url)
SELECT p.id, v.image_url
FROM dbo.Products p
JOIN (VALUES
    ('iPhone 15 Pro', '/images/iphone15pro.webp'),
    ('Galaxy S24', '/images/galaxys24.webp'),
    ('OPPO Reno 12', '/images/opporeno12.webp'),
    ('Redmi Note 13', '/images/redminote13.webp')
) AS v(product_name, image_url) ON p.name = v.product_name
WHERE NOT EXISTS (SELECT 1 FROM dbo.ProductImages i WHERE i.product_id = p.id);
GO
