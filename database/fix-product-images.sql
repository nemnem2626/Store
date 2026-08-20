-- Script để sửa ảnh sản phẩm trùng nhau
-- Chạy: sqlcmd -S localhost -U sa -P <mật khẩu> -C -f 65001 -i database/fix-product-images.sql

USE STORE;
GO

-- Xóa tất cả ảnh sản phẩm cũ
DELETE FROM dbo.ProductImages;
GO

-- Thêm ảnh khác nhau cho từng sản phẩm
INSERT INTO dbo.ProductImages (product_id, image_url)
SELECT p.id, v.image_url
FROM dbo.Products p
LEFT JOIN (VALUES
    ('iPhone 15 Pro', '/images/iphone15pro.webp'),
    ('Galaxy S24', '/images/galaxys24.webp'),
    ('OPPO Reno 12', '/images/opporeno12.webp'),
    ('Redmi Note 13', '/images/redminote13.webp')
) AS v(product_name, image_url) ON p.name = v.product_name
WHERE v.image_url IS NOT NULL;
GO

-- Kiểm tra kết quả
SELECT p.id, p.name, pi.image_url
FROM dbo.Products p
LEFT JOIN dbo.ProductImages pi ON p.id = pi.product_id
ORDER BY p.id;
GO
