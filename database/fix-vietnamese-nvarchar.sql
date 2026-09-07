-- Chuyển các cột chữ sang NVARCHAR để không bị lỗi font tiếng Việt (ví dụ màu
-- sản phẩm hiển thị thành "Titan t? nhiên"). Dùng cho DB đã tạo từ trước.
-- Chạy trong SSMS sau khi đã chọn đúng database, hoặc:
--   sqlcmd -S localhost -U sa -P <mật khẩu> -C -d STORE -f 65001 -i database/fix-vietnamese-nvarchar.sql
-- Script idempotent: chạy lại nhiều lần không lỗi.
--
-- Lưu ý: dữ liệu tiếng Việt đã bị hỏng từ trước (lưu sai kiểu) sẽ KHÔNG tự
-- khôi phục, cần sửa lại nội dung đó bằng tay trong trang quản trị.

SET NOCOUNT ON;

/* --- 1. Tạm gỡ các ràng buộc UNIQUE đang phụ thuộc vào cột cần đổi --- */
DECLARE @drop NVARCHAR(MAX) = N'';

SELECT @drop = @drop + N'ALTER TABLE ' + QUOTENAME(u.schema_name) + N'.' + QUOTENAME(u.table_name)
             + N' DROP CONSTRAINT ' + QUOTENAME(u.constraint_name) + N';' + CHAR(10)
FROM (
    SELECT DISTINCT SCHEMA_NAME(t.schema_id) AS schema_name, t.name AS table_name, kc.name AS constraint_name
    FROM sys.key_constraints kc
    JOIN sys.tables  t ON t.object_id = kc.parent_object_id
    JOIN sys.index_columns ic ON ic.object_id = t.object_id AND ic.index_id = kc.unique_index_id
    JOIN sys.columns c ON c.object_id = t.object_id AND c.column_id = ic.column_id
    JOIN sys.types  ty ON ty.user_type_id = c.user_type_id
    WHERE kc.type = 'UQ' AND ty.name = 'varchar'
      AND t.name IN ('Categories', 'ProductVariants')
      AND c.name IN ('name', 'size', 'color')
) u;

IF @drop <> N'' EXEC sp_executesql @drop;

/* --- 2. Đổi kiểu cột sang NVARCHAR --- */
DECLARE @alter NVARCHAR(MAX) = N'';

SELECT @alter = @alter + N'ALTER TABLE ' + QUOTENAME(s.name) + N'.' + QUOTENAME(t.name)
              + N' ALTER COLUMN ' + QUOTENAME(c.name) + N' NVARCHAR('
              + CASE WHEN c.max_length = -1 THEN N'MAX' ELSE CAST(c.max_length AS NVARCHAR(10)) END
              + N')' + CASE WHEN c.is_nullable = 0 THEN N' NOT NULL' ELSE N' NULL' END + N';' + CHAR(10)
FROM sys.columns c
JOIN sys.tables  t ON t.object_id = c.object_id
JOIN sys.schemas s ON s.schema_id = t.schema_id
JOIN sys.types   ty ON ty.user_type_id = c.user_type_id
WHERE ty.name = 'varchar'
  AND t.name IN ('Categories', 'Users', 'Products', 'ProductVariants', 'CartItems', 'Orders')
  AND c.name IN ('name', 'description', 'fullname', 'address', 'size', 'color', 'productName');

IF @alter = N''
    PRINT N'Khong co cot nao can chuyen - database da dung NVARCHAR.';
ELSE
BEGIN
    PRINT @alter;
    EXEC sp_executesql @alter;
END

/* --- 3. Tạo lại các ràng buộc UNIQUE --- */
IF OBJECT_ID('dbo.Categories', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.key_constraints kc
                   WHERE kc.type = 'UQ' AND kc.parent_object_id = OBJECT_ID('dbo.Categories'))
    ALTER TABLE dbo.Categories ADD CONSTRAINT UQ_Categories_name UNIQUE (name);

IF OBJECT_ID('dbo.ProductVariants', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.key_constraints kc
                   WHERE kc.type = 'UQ' AND kc.parent_object_id = OBJECT_ID('dbo.ProductVariants'))
    ALTER TABLE dbo.ProductVariants ADD CONSTRAINT UQ_ProductVariants UNIQUE (product_id, size, color);

PRINT N'Hoan tat.';
GO
