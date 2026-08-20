-- Script để xóa sạch ảnh lỗi và thêm ảnh đúng
-- Chạy: sqlcmd -S localhost -U sa -P <mật khẩu> -C -f 65001 -i database/clean-and-fix-images.sql

USE STORE;
GO

-- 1. Xóa tất cả ảnh sản phẩm (cả ảnh đại diện và ảnh variant)
DELETE FROM dbo.ProductImages;
GO

-- 2. Reset identity seed
DBCC CHECKIDENT ('dbo.ProductImages', RESEED, 0);
GO

-- 3. Xóa tất cả sản phẩm và variant cũ
DELETE FROM dbo.ProductVariants;
DELETE FROM dbo.Products;
GO

-- 4. Reset identity seed cho Products
DBCC CHECKIDENT ('dbo.Products', RESEED, 0);
DBCC CHECKIDENT ('dbo.ProductVariants', RESEED, 0);
GO

-- 5. Thêm danh mục (nếu chưa có)
INSERT INTO dbo.Categories (name)
SELECT v.name FROM (VALUES ('iPhone'), ('Samsung'), ('Xiaomi'), ('OPPO')) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Categories c WHERE c.name = v.name);
GO

-- 6. Thêm 4 sản phẩm mới
INSERT INTO dbo.Products (name, description, category_id)
SELECT v.name, v.description, c.id
FROM (VALUES
    ('iPhone 15 Pro', N'Chip A17 Pro, khung titan, camera 48MP', 'iPhone'),
    ('Galaxy S24', N'Snapdragon 8 Gen 3, màn hình 6.2 inch', 'Samsung'),
    ('OPPO Reno 12', N'Camera 50MP AI, màn hình AMOLED 6.7 inch', 'OPPO'),
    ('Redmi Note 13', N'Snapdragon, pin 5000mAh, sạc 33W', 'Xiaomi')
) AS v(name, description, category)
JOIN dbo.Categories c ON c.name = v.category;
GO

-- 7. Thêm variant cho mỗi sản phẩm
INSERT INTO dbo.ProductVariants (product_id, size, color, price, stock)
SELECT p.id, v.size, v.color, v.price, v.stock
FROM (VALUES
    ('iPhone 15 Pro', '256GB', N'Titan', 29990000, 10),
    ('iPhone 15 Pro', '512GB', N'Đen', 34990000, 5),
    ('Galaxy S24', '256GB', N'Xám', 22990000, 8),
    ('OPPO Reno 12', '256GB', N'Trắng', 12990000, 12),
    ('Redmi Note 13', '256GB', N'Xanh', 4990000, 15)
) AS v(product, size, color, price, stock)
JOIN dbo.Products p ON p.name = v.product;
GO

-- 8. Thêm ảnh đại diện (variant_id = NULL) cho mỗi sản phẩm
INSERT INTO dbo.ProductImages (product_id, variant_id, image_url)
SELECT p.id, NULL, v.image_url
FROM dbo.Products p
JOIN (VALUES
    ('iPhone 15 Pro', '/images/iphone15pro.webp'),
    ('Galaxy S24', '/images/galaxys24.webp'),
    ('OPPO Reno 12', '/images/opporeno12.webp'),
    ('Redmi Note 13', '/images/redminote13.webp')
) AS v(product_name, image_url) ON p.name = v.product_name;
GO

-- 9. Kiểm tra kết quả
SELECT p.id, p.name, pi.image_url, pi.variant_id
FROM dbo.Products p
LEFT JOIN dbo.ProductImages pi ON p.id = pi.product_id
ORDER BY p.id, pi.id;
GO

-- 10. Kiểm tra ProductVariants
SELECT * FROM dbo.ProductVariants ORDER BY product_id;
GO
