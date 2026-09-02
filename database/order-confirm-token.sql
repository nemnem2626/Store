-- Thêm cột confirm_token cho bảng Orders (link "Đã nhận hàng" trong email).
-- Chạy file này nếu database đã tồn tại từ trước.
IF COL_LENGTH('dbo.Orders', 'confirm_token') IS NULL
    ALTER TABLE dbo.Orders ADD confirm_token VARCHAR(64) NULL;
GO
