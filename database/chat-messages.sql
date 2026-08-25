/* Bảng tin nhắn hỗ trợ (khách hàng <-> nhân viên).
   Chạy file này trong SSMS nếu database đã tồn tại và chưa có bảng ChatMessages. */

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
