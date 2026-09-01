/* Bảng thông báo cho admin (staff xác nhận giao hàng...).
   Chạy file này trong SSMS nếu database đã tồn tại và chưa có bảng Notifications. */

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
