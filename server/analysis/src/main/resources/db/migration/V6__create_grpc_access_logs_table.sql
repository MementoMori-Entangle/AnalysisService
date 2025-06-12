-- gRPCアクセスログテーブル作成
CREATE TABLE IF NOT EXISTS grpc_access_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL,
    method_name VARCHAR(255) NOT NULL,
    request_json TEXT NOT NULL,
    response_json TEXT,
    access_key VARCHAR(255),
    ip_address VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
